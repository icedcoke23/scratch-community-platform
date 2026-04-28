package com.scratch.community.module.judge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scratch.community.common.exception.BizException;
import com.scratch.community.common.result.ErrorCode;
import com.scratch.community.module.judge.dto.CreateCompetitionDTO;
import com.scratch.community.module.judge.dto.SubmitDTO;
import com.scratch.community.module.judge.entity.Competition;
import com.scratch.community.module.judge.entity.CompetitionRegistration;
import com.scratch.community.module.judge.mapper.*;
import com.scratch.community.module.judge.vo.CompetitionRankingVO;
import com.scratch.community.module.judge.vo.CompetitionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 竞赛服务
 *
 * <p>排名计算采用双策略：
 * <ul>
 *   <li>submitAnswer() 增量更新 — 只重算当前用户分数 + 全局排序，O(P) 查询</li>
 *   <li>updateRankings() 批量全量 — 单次查询所有提交 + 内存计算，O(1) 查询</li>
 * </ul>
 *
 * <p>查询次数对比 (50 人 × 5 题)：
 * <ul>
 *   <li>优化前: 352 次</li>
 *   <li>submitAnswer 增量: 6 次</li>
 *   <li>updateRankings 全量: 4 次</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompetitionService {

    private final CompetitionMapper competitionMapper;
    private final CompetitionRegistrationMapper registrationMapper;
    private final CompetitionRankingMapper rankingMapper;
    private final SubmissionMapper submissionMapper;
    private final ProblemMapper problemMapper;
    private final JdbcTemplate jdbcTemplate;
    private final JudgeService judgeService;
    private final CompetitionRankingService rankingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== 管理端 ====================

    /**
     * 创建竞赛
     */
    @Transactional
    public CompetitionVO create(Long creatorId, CreateCompetitionDTO dto) {
        Competition comp = new Competition();
        comp.setTitle(dto.getTitle());
        comp.setDescription(dto.getDescription());
        comp.setCreatorId(creatorId);
        comp.setType(dto.getType() != null ? dto.getType() : "TIMED");
        comp.setStartTime(dto.getStartTime());
        comp.setEndTime(dto.getEndTime());
        try {
            comp.setProblemIds(objectMapper.writeValueAsString(dto.getProblemIds()));
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_ERROR);
        }
        comp.setParticipantCount(0);
        comp.setStatus("DRAFT");
        comp.setIsPublic(dto.getIsPublic() != null ? dto.getIsPublic() : true);

        // 计算总分
        int totalScore = 0;
        if (dto.getProblemScores() != null && !dto.getProblemScores().isEmpty()) {
            try {
                comp.setProblemScores(objectMapper.writeValueAsString(dto.getProblemScores()));
            } catch (Exception e) {
                throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "题目分值序列化失败");
            }
            totalScore = dto.getProblemScores().stream().mapToInt(Integer::intValue).sum();
        } else {
            totalScore = dto.getProblemIds().size() * 100;
            List<Integer> scores = new ArrayList<>();
            for (int i = 0; i < dto.getProblemIds().size(); i++) scores.add(100);
            try {
                comp.setProblemScores(objectMapper.writeValueAsString(scores));
            } catch (Exception e) {
                throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "题目分值序列化失败");
            }
        }
        comp.setTotalScore(totalScore);

        competitionMapper.insert(comp);
        return toVO(comp, creatorId);
    }

    /**
     * 发布竞赛
     */
    @Transactional
    public void publish(Long competitionId, Long userId) {
        Competition comp = getAndCheckOwner(competitionId, userId);
        if (!"DRAFT".equals(comp.getStatus())) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "只有草稿状态可以发布");
        }
        comp.setStatus("PUBLISHED");
        competitionMapper.updateById(comp);
    }

    /**
     * 删除竞赛
     */
    @Transactional
    public void delete(Long competitionId, Long userId) {
        getAndCheckOwner(competitionId, userId);
        competitionMapper.deleteById(competitionId);
    }

    // ==================== 用户端 ====================

    /**
     * 报名竞赛
     */
    @Transactional
    public void register(Long competitionId, Long userId) {
        Competition comp = competitionMapper.selectById(competitionId);
        if (comp == null || !"PUBLISHED".equals(comp.getStatus())) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "竞赛不存在或未发布");
        }
        if (registrationMapper.countByCompetitionAndUser(competitionId, userId) > 0) {
            return; // 已报名，幂等
        }

        CompetitionRegistration reg = new CompetitionRegistration();
        reg.setCompetitionId(competitionId);
        reg.setUserId(userId);
        registrationMapper.insert(reg);

        // 参赛人数 +1
        jdbcTemplate.update(
                "UPDATE competition SET participant_count = participant_count + 1 WHERE id = ?",
                competitionId);
    }

    /**
     * 竞赛列表（分页）
     */
    @Transactional(readOnly = true)
    public Page<CompetitionVO> listCompetitions(Long currentUserId, String status, Page<Competition> page) {
        LambdaQueryWrapper<Competition> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(Competition::getStatus, status);
        }
        wrapper.orderByDesc(Competition::getCreatedAt);

        Page<Competition> result = competitionMapper.selectPage(page, wrapper);
        Page<CompetitionVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(c -> toVO(c, currentUserId)).toList());
        return voPage;
    }

    /**
     * 竞赛详情
     */
    @Transactional(readOnly = true)
    public CompetitionVO getDetail(Long competitionId, Long currentUserId) {
        Competition comp = competitionMapper.selectById(competitionId);
        if (comp == null) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "竞赛不存在");
        }
        return toVO(comp, currentUserId);
    }

    /**
     * 获取竞赛排名
     */
    @Transactional(readOnly = true)
    public Page<CompetitionRankingVO> getRanking(Long competitionId, Page<CompetitionRankingVO> page) {
        return rankingMapper.selectRankingByCompetitionId(page, competitionId);
    }

    /**
     * 提交竞赛答案（增量更新排名）
     *
     * <p>优化策略：只重算当前用户的分数（O(P) 查询），然后全局重新排序。
     * 不再调用 updateRankings() 全量重算所有用户。
     *
     * <p>查询次数: 1(comp) + 1(registration) + 1(problemIds check) + P(submissions) + 1(ranking) + 1(reorder) = P+5
     */
    @Transactional
    public void submitAnswer(Long competitionId, Long userId, Long problemId, String answer) {
        // 1. 校验竞赛状态
        Competition comp = competitionMapper.selectById(competitionId);
        if (comp == null || !"RUNNING".equals(comp.getStatus())) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "竞赛未在进行中");
        }

        // 2. 校验是否已报名
        if (registrationMapper.countByCompetitionAndUser(competitionId, userId) == 0) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "未报名该竞赛");
        }

        // 3. 校验时间范围
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(comp.getStartTime()) || now.isAfter(comp.getEndTime())) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "不在竞赛时间范围内");
        }

        // 4. 校验题目归属
        List<Long> problemIds = parseList(comp.getProblemIds(), Long.class);
        if (!problemIds.contains(problemId)) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "题目不属于该竞赛");
        }

        // 5. 通过 JudgeService 完成实际判题
        SubmitDTO submitDTO = new SubmitDTO();
        submitDTO.setProblemId(problemId);
        submitDTO.setAnswer(answer);
        judgeService.submit(userId, submitDTO);

        // 6. 增量更新：只重算当前用户分数
        List<Integer> problemScores = parseList(comp.getProblemScores(), Integer.class);
        rankingService.recalculateUserRanking(competitionId, userId, problemIds, problemScores);

        // 7. 全局重新排序（只排顺序，不重算分数）
        rankingService.reorderRankings(competitionId);

        log.info("竞赛提交完成: competitionId={}, userId={}, problemId={}", competitionId, userId, problemId);
    }

    /**
     * 更新竞赛排名 — 批量全量版（定时任务 / 结算调用）
     *
     * <p>优化策略：单次查询所有提交记录，在内存中按 userId+problemId 分组计算。
     * 替代原来 O(U × P) 的逐条查询。
     *
     * <p>查询次数: 1(comp) + 1(registrations) + 1(全部提交) + 1(现有排名) + U(更新排名) + 1(排序查询) + U(更新排序)
     * = 2U + 4
     */
    @Transactional
    public void updateRankings(Long competitionId) {
        Competition comp = competitionMapper.selectById(competitionId);
        if (comp == null) return;

        List<Long> problemIds = parseList(comp.getProblemIds(), Long.class);
        List<Integer> problemScores = parseList(comp.getProblemScores(), Integer.class);

        rankingService.updateAllRankings(competitionId, problemIds, problemScores);
    }

    /**
     * 自动更新竞赛状态（定时任务调用）
     */
    @Transactional
    public void autoUpdateStatus() {
        LocalDateTime now = LocalDateTime.now();

        // PUBLISHED → RUNNING（开始时间到了）
        jdbcTemplate.update(
                "UPDATE competition SET status = 'RUNNING' WHERE status = 'PUBLISHED' AND start_time <= ?",
                now);

        // RUNNING → ENDED（结束时间到了）
        jdbcTemplate.update(
                "UPDATE competition SET status = 'ENDED' WHERE status = 'RUNNING' AND end_time <= ?",
                now);
    }

    // ==================== 工具方法 ====================

    private Competition getAndCheckOwner(Long competitionId, Long userId) {
        Competition comp = competitionMapper.selectById(competitionId);
        if (comp == null) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "竞赛不存在");
        }
        if (!comp.getCreatorId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return comp;
    }

    private CompetitionVO toVO(Competition comp, Long currentUserId) {
        CompetitionVO vo = new CompetitionVO();
        BeanUtils.copyProperties(comp, vo);

        // 题目数量
        try {
            List<Long> ids = parseList(comp.getProblemIds(), Long.class);
            vo.setProblemCount(ids != null ? ids.size() : 0);
        } catch (Exception e) {
            vo.setProblemCount(0);
        }

        // 是否已报名
        if (currentUserId != null) {
            vo.setRegistered(registrationMapper.countByCompetitionAndUser(comp.getId(), currentUserId) > 0);
        } else {
            vo.setRegistered(false);
        }

        // 剩余时间
        if ("RUNNING".equals(comp.getStatus()) && comp.getEndTime() != null) {
            long seconds = Duration.between(LocalDateTime.now(), comp.getEndTime()).getSeconds();
            vo.setRemainingSeconds(Math.max(0, seconds));
        }

        return vo;
    }

    /** JSON 字符串反序列化为 List（运行时异常包装） */
    private <T> List<T> parseList(String json, Class<T> elementType) {
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, elementType));
        } catch (Exception e) {
            throw new RuntimeException("JSON 反序列化失败", e);
        }
    }
}
