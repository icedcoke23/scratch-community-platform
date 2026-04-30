package com.scratch.community.module.judge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scratch.community.common.exception.BizException;
import com.scratch.community.common.result.ErrorCode;
import com.scratch.community.module.judge.dto.CreateProblemDTO;
import com.scratch.community.module.judge.entity.Problem;
import com.scratch.community.module.judge.mapper.ProblemMapper;
import com.scratch.community.module.judge.vo.ProblemDetailVO;
import com.scratch.community.module.judge.vo.ProblemVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 题目服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProblemService {

    private final ProblemMapper problemMapper;
    private final ObjectMapper objectMapper;

    /**
     * 创建题目（教师/管理员）
     */
    @Transactional
    public ProblemVO create(Long creatorId, CreateProblemDTO dto) {
        Problem problem = new Problem();
        problem.setTitle(dto.getTitle());
        problem.setDescription(dto.getDescription());
        problem.setType(dto.getType());
        problem.setDifficulty(dto.getDifficulty() != null ? dto.getDifficulty() : "medium");
        problem.setTags(dto.getTags());
        problem.setScore(dto.getScore() != null ? dto.getScore() : 10);
        problem.setAnswer(dto.getAnswer());
        problem.setExpectedOutput(dto.getExpectedOutput());
        problem.setCreatorId(creatorId);
        problem.setStatus("draft");
        problem.setSubmitCount(0);
        problem.setAcceptCount(0);

        // 序列化选项
        if (dto.getOptions() != null) {
            try {
                problem.setOptions(objectMapper.writeValueAsString(dto.getOptions()));
            } catch (Exception e) {
                log.warn("选项序列化失败: {}", e.getMessage());
            }
        }

        problemMapper.insert(problem);
        return toVO(problem);
    }

    /**
     * 题目列表（分页，支持类型和难度筛选）
     */
    @Transactional(readOnly = true)
    public Page<ProblemVO> listProblems(String type, String difficulty, Page<Problem> page) {
        LambdaQueryWrapper<Problem> wrapper = new LambdaQueryWrapper<Problem>()
                .eq(Problem::getStatus, "published");
        if (type != null) {
            wrapper.eq(Problem::getType, type);
        }
        if (difficulty != null) {
            wrapper.eq(Problem::getDifficulty, difficulty);
        }
        wrapper.orderByDesc(Problem::getCreatedAt);

        Page<Problem> result = problemMapper.selectPage(page, wrapper);
        return toVOPage(result);
    }

    /**
     * 题目详情
     */
    @Transactional(readOnly = true)
    public ProblemDetailVO getDetail(Long problemId) {
        Problem problem = problemMapper.selectById(problemId);
        if (problem == null) {
            throw new BizException(ErrorCode.PROBLEM_NOT_FOUND);
        }

        ProblemDetailVO vo = new ProblemDetailVO();
        BeanUtils.copyProperties(problem, vo);

        // 反序列化选项
        if (problem.getOptions() != null) {
            try {
                vo.setOptions(objectMapper.readValue(problem.getOptions(),
                        new TypeReference<List<Map<String, String>>>() {}));
            } catch (Exception e) {
                log.warn("选项反序列化失败: {}", e.getMessage());
            }
        }

        return vo;
    }

    /**
     * 更新题目
     */
    @Transactional
    public void update(Long userId, Long problemId, CreateProblemDTO dto) {
        Problem problem = getAndCheckOwner(userId, problemId);
        if (dto.getTitle() != null) problem.setTitle(dto.getTitle());
        if (dto.getDescription() != null) problem.setDescription(dto.getDescription());
        if (dto.getDifficulty() != null) problem.setDifficulty(dto.getDifficulty());
        if (dto.getTags() != null) problem.setTags(dto.getTags());
        if (dto.getScore() != null) problem.setScore(dto.getScore());
        if (dto.getAnswer() != null) problem.setAnswer(dto.getAnswer());
        if (dto.getExpectedOutput() != null) problem.setExpectedOutput(dto.getExpectedOutput());
        if (dto.getOptions() != null) {
            try {
                problem.setOptions(objectMapper.writeValueAsString(dto.getOptions()));
            } catch (Exception e) {
                log.warn("选项序列化失败: {}", e.getMessage());
            }
        }
        problemMapper.updateById(problem);
    }

    /**
     * 发布题目
     */
    @Transactional
    public void publish(Long userId, Long problemId) {
        Problem problem = getAndCheckOwner(userId, problemId);
        problem.setStatus("published");
        problemMapper.updateById(problem);
    }

    /**
     * 删除题目（软删除）
     */
    @Transactional
    public void delete(Long userId, Long problemId) {
        getAndCheckOwner(userId, problemId);
        problemMapper.deleteById(problemId);
    }

    // ==================== 私有方法 ====================

    private Problem getAndCheckOwner(Long userId, Long problemId) {
        Problem problem = problemMapper.selectById(problemId);
        if (problem == null) {
            throw new BizException(ErrorCode.PROBLEM_NOT_FOUND);
        }
        if (!problem.getCreatorId().equals(userId)) {
            throw new BizException(ErrorCode.USER_NO_PERMISSION);
        }
        return problem;
    }

    private ProblemVO toVO(Problem problem) {
        ProblemVO vo = new ProblemVO();
        BeanUtils.copyProperties(problem, vo);
        return vo;
    }

    private Page<ProblemVO> toVOPage(Page<Problem> page) {
        Page<ProblemVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }
}
