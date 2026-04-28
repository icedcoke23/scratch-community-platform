package com.scratch.community.module.judge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scratch.community.common.event.PointEvent;
import com.scratch.community.common.exception.BizException;
import com.scratch.community.common.result.ErrorCode;
import com.scratch.community.module.judge.dto.SubmitDTO;
import com.scratch.community.module.judge.entity.Problem;
import com.scratch.community.module.judge.entity.Submission;
import com.scratch.community.module.judge.mapper.ProblemMapper;
import com.scratch.community.module.judge.mapper.SubmissionMapper;
import com.scratch.community.module.judge.vo.SubmissionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 判题服务
 * 支持选择题/判断题即时判题 + Scratch 编程题异步判题
 *
 * 改进:
 * - 沙箱 API 路径统一为 POST /judge
 * - 异步判题增加重试机制（最多 3 次）
 * - 判题结果解析更加健壮
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JudgeService {

    private final ProblemMapper problemMapper;
    private final SubmissionMapper submissionMapper;
    private final ObjectMapper objectMapper;
    @Qualifier("judgeRestTemplate")
    private final RestTemplate restTemplate;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${scratch.judge.sandbox-url:http://localhost:8081}")
    private String sandboxUrl;

    @Value("${scratch.judge.timeout:30000}")
    private int judgeTimeout;

    /** 异步判题最大重试次数 */
    private static final int MAX_RETRIES = 3;

    /**
     * 提交答案
     */
    @Transactional
    public SubmissionVO submit(Long userId, SubmitDTO dto) {
        // 1. 校验题目
        Problem problem = problemMapper.selectById(dto.getProblemId());
        if (problem == null) {
            throw new BizException(ErrorCode.PROBLEM_NOT_FOUND);
        }
        if (!"published".equals(problem.getStatus())) {
            throw new BizException(ErrorCode.PROBLEM_NOT_PUBLISHED);
        }

        // 2. 检查重复提交（PENDING 状态的提交）
        Long existingCount = submissionMapper.selectCount(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getUserId, userId)
                        .eq(Submission::getProblemId, dto.getProblemId())
                        .eq(Submission::getVerdict, "PENDING"));
        if (existingCount > 0) {
            throw new BizException(ErrorCode.SUBMISSION_DUPLICATE);
        }

        // 3. 创建提交记录 + 判题
        Submission submission;
        if ("choice".equals(problem.getType()) || "true_false".equals(problem.getType())) {
            submission = createSubmission(userId, dto, problem);
            judgeInstant(submission, problem);
            updateProblemStats(problem, submission.getVerdict());
        } else if ("scratch_algo".equals(problem.getType())) {
            submission = createSubmission(userId, dto, problem);
            // 异步判题只传 ID，避免 detached entity 跨线程问题
            judgeAsync(submission.getId(), problem.getId());
        } else {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "不支持的题目类型: " + problem.getType());
        }

        return toVO(submission, problem.getTitle());
    }

    @Transactional
    public Submission createSubmission(Long userId, SubmitDTO dto, Problem problem) {
        Submission submission = new Submission();
        submission.setUserId(userId);
        submission.setProblemId(dto.getProblemId());
        submission.setSubmitType(problem.getType());
        submission.setAnswer(dto.getAnswer());
        submission.setVerdict("PENDING");
        submissionMapper.insert(submission);
        return submission;
    }

    @Transactional
    public void updateProblemStats(Problem problem, String verdict) {
        problemMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Problem>()
                .eq("id", problem.getId())
                .setSql("submit_count = submit_count + 1"));
        if ("AC".equals(verdict)) {
            problemMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Problem>()
                    .eq("id", problem.getId())
                    .setSql("accept_count = accept_count + 1"));
        }
    }

    @Transactional(readOnly = true)
    public Page<SubmissionVO> mySubmissions(Long userId, Long problemId, Page<Submission> page) {
        LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<Submission>()
                .eq(Submission::getUserId, userId);
        if (problemId != null) {
            wrapper.eq(Submission::getProblemId, problemId);
        }
        wrapper.orderByDesc(Submission::getCreatedAt);
        Page<Submission> result = submissionMapper.selectPage(page, wrapper);
        return toVOPage(result);
    }

    @Transactional(readOnly = true)
    public SubmissionVO getResult(Long userId, Long submissionId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new BizException(ErrorCode.SUBMISSION_NOT_FOUND);
        }
        if (!submission.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        Problem problem = problemMapper.selectById(submission.getProblemId());
        String problemTitle = problem != null ? problem.getTitle() : "未知题目";
        return toVO(submission, problemTitle);
    }

    // ==================== 判题逻辑 ====================

    /**
     * 即时判题（选择题/判断题）
     */
    @Transactional
    public void judgeInstant(Submission submission, Problem problem) {
        String userAnswer = submission.getAnswer();
        String correctAnswer = problem.getAnswer();

        try {
            if (userAnswer != null && userAnswer.trim().equalsIgnoreCase(
                    correctAnswer != null ? correctAnswer.trim() : "")) {
                submission.setVerdict("AC");
                submission.setJudgeDetail(objectMapper.writeValueAsString(
                        Map.of("message", "答案正确")));
            } else {
                submission.setVerdict("WA");
                submission.setJudgeDetail(objectMapper.writeValueAsString(
                        Map.of("message", "答案错误",
                                "expected", correctAnswer != null ? correctAnswer : "",
                                "actual", userAnswer != null ? userAnswer : "")));
            }
        } catch (Exception e) {
            log.warn("判题详情序列化失败: {}", e.getMessage());
            submission.setJudgeDetail("{\"message\":\"判题完成\"}");
        }
        submissionMapper.updateById(submission);

        if ("AC".equals(submission.getVerdict())) {
            eventPublisher.publishEvent(new PointEvent(this, submission.getUserId(),
                    PointEvent.PointAction.AC_SUBMISSION, submission.getId()));
        }
    }

    /**
     * 异步判题（Scratch 编程题）
     *
     * 使用 @Async 在独立线程池中执行，带指数退避重试机制。
     * 沙箱 API: POST /judge
     *
     * <p>改进: 使用指数退避替代 Thread.sleep，避免阻塞线程池线程。
     * 重试间隔: 1s, 2s, 4s（指数退避）
     *
     * <p>注意: 只接收 ID 参数，在异步线程中重新查询 entity，
     * 避免 detached entity 跨线程导致的 persistence context 问题。
     */
    @Async("judgeExecutor")
    @Transactional
    public CompletableFuture<Void> judgeAsync(Long submissionId, Long problemId) {
        // 在异步线程中重新查询 entity，确保在当前事务的 persistence context 中
        Submission submission = submissionMapper.selectById(submissionId);
        Problem problem = problemMapper.selectById(problemId);
        if (submission == null || problem == null) {
            log.error("异步判题失败: submission 或 problem 不存在, submissionId={}, problemId={}", submissionId, problemId);
            return CompletableFuture.completedFuture(null);
        }

        log.info("异步判题开始: submissionId={}, problemId={}", submissionId, problemId);

        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                Map<String, Object> request = new HashMap<>();
                request.put("submissionId", submissionId);
                request.put("sb3Url", submission.getSb3Url());
                request.put("expectedOutput", problem.getExpectedOutput());
                request.put("timeoutMs", judgeTimeout);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

                ResponseEntity<Map> response = restTemplate.postForEntity(
                        sandboxUrl + "/judge", entity, Map.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Map<String, Object> body = response.getBody();
                    String verdict = (String) body.getOrDefault("verdict", body.getOrDefault("result", "RE"));
                    submission.setVerdict(verdict);
                    submission.setRuntimeMs(body.get("runtimeMs") != null ?
                            ((Number) body.get("runtimeMs")).longValue() : null);
                    submission.setMemoryKb(body.get("memoryKb") != null ?
                            ((Number) body.get("memoryKb")).longValue() : null);

                    try {
                        Object detail = body.get("detail");
                        if (detail != null) {
                            submission.setJudgeDetail(objectMapper.writeValueAsString(detail));
                        } else {
                            submission.setJudgeDetail(objectMapper.writeValueAsString(body));
                        }
                    } catch (Exception e) {
                        submission.setJudgeDetail("{\"message\":\"判题完成\"}");
                    }

                    // 成功，跳出重试循环
                    lastException = null;
                    break;
                } else {
                    lastException = new RuntimeException("沙箱返回异常状态: " + response.getStatusCode());
                    log.warn("异步判题第 {} 次失败: submissionId={}, status={}", attempt, submissionId, response.getStatusCode());
                }
            } catch (Exception e) {
                lastException = e;
                log.warn("异步判题第 {} 次异常: submissionId={}, error={}", attempt, submissionId, e.getMessage());
            }

            // 指数退避重试（1s, 2s, 4s）
            if (attempt < MAX_RETRIES) {
                long delayMs = 1000L * (1L << (attempt - 1)); // 指数退避: 1s, 2s, 4s
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // 所有重试都失败
        if (lastException != null) {
            log.error("异步判题最终失败: submissionId={}, retries={}", submissionId, MAX_RETRIES);
            submission.setVerdict("RE");
            try {
                submission.setJudgeDetail(objectMapper.writeValueAsString(
                        Map.of("message", "判题服务不可用，已重试 " + MAX_RETRIES + " 次")));
            } catch (Exception ex) {
                submission.setJudgeDetail("{\"message\":\"判题服务不可用\"}");
            }
        }

        submissionMapper.updateById(submission);
        updateProblemStats(problem, submission.getVerdict());
        log.info("异步判题完成: submissionId={}, verdict={}", submissionId, submission.getVerdict());
        return CompletableFuture.completedFuture(null);
    }

    // ==================== 私有方法 ====================

    private SubmissionVO toVO(Submission submission, String problemTitle) {
        SubmissionVO vo = new SubmissionVO();
        BeanUtils.copyProperties(submission, vo);
        vo.setProblemTitle(problemTitle);
        return vo;
    }

    private Page<SubmissionVO> toVOPage(Page<Submission> page) {
        Page<SubmissionVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream()
                .map(s -> toVO(s, null))
                .toList());
        return voPage;
    }
}
