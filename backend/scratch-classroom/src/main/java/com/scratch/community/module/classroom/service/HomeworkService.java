package com.scratch.community.module.classroom.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scratch.community.common.event.PointEvent;
import com.scratch.community.common.exception.BizException;
import com.scratch.community.common.result.ErrorCode;
import com.scratch.community.module.classroom.dto.CreateHomeworkDTO;
import com.scratch.community.module.classroom.dto.SubmitHomeworkDTO;
import com.scratch.community.module.classroom.entity.Homework;
import com.scratch.community.module.classroom.entity.HomeworkSubmission;
import com.scratch.community.module.classroom.mapper.HomeworkMapper;
import com.scratch.community.module.classroom.mapper.HomeworkSubmissionMapper;
import com.scratch.community.module.classroom.vo.HomeworkSubmissionVO;
import com.scratch.community.module.classroom.vo.HomeworkVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 作业服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HomeworkService {

    private final HomeworkMapper homeworkMapper;
    private final HomeworkSubmissionMapper homeworkSubmissionMapper;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    // ==================== 教师端 ====================

    /**
     * 创建作业
     */
    @Transactional
    public HomeworkVO create(Long teacherId, CreateHomeworkDTO dto) {
        // 校验班级是否存在且是该教师的
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM class WHERE id = ? AND teacher_id = ? AND deleted = 0",
                Integer.class, dto.getClassId(), teacherId);
        if (count == null || count == 0) {
            throw new BizException(ErrorCode.CLASS_NOT_FOUND);
        }

        Homework homework = new Homework();
        homework.setClassId(dto.getClassId());
        homework.setTeacherId(teacherId);
        homework.setTitle(dto.getTitle());
        homework.setDescription(dto.getDescription());
        homework.setType(dto.getType() != null ? dto.getType() : "scratch_project");
        homework.setDeadline(dto.getDeadline());
        homework.setTotalScore(dto.getTotalScore() != null ? dto.getTotalScore() : 100);
        homework.setStatus("draft");
        homework.setSubmitCount(0);
        homework.setGradedCount(0);

        // 序列化题目 ID 列表
        if (dto.getProblemIds() != null) {
            try {
                homework.setProblemIds(objectMapper.writeValueAsString(dto.getProblemIds()));
            } catch (Exception e) {
                log.warn("题目 ID 序列化失败: {}", e.getMessage());
            }
        }

        homeworkMapper.insert(homework);
        return toVO(homework);
    }

    /**
     * 发布作业
     */
    @Transactional
    public void publish(Long teacherId, Long homeworkId) {
        Homework hw = getAndCheckOwner(teacherId, homeworkId);
        hw.setStatus("published");
        homeworkMapper.updateById(hw);
    }

    /**
     * 关闭作业
     */
    @Transactional
    public void close(Long teacherId, Long homeworkId) {
        Homework hw = getAndCheckOwner(teacherId, homeworkId);
        hw.setStatus("closed");
        homeworkMapper.updateById(hw);
    }

    /**
     * 获取班级作业列表
     */
    @Transactional(readOnly = true)
    public Page<HomeworkVO> listByClass(Long classId, Page<Homework> page) {
        Page<Homework> result = homeworkMapper.selectPage(page,
                new LambdaQueryWrapper<Homework>()
                        .eq(Homework::getClassId, classId)
                        .orderByDesc(Homework::getCreatedAt));
        return toVOPage(result);
    }

    /**
     * 作业详情
     */
    @Transactional(readOnly = true)
    public HomeworkVO getDetail(Long homeworkId) {
        Homework hw = homeworkMapper.selectById(homeworkId);
        if (hw == null) {
            throw new BizException(ErrorCode.HOMEWORK_NOT_FOUND);
        }
        return toVO(hw);
    }

    /**
     * 获取作业提交列表（教师查看）
     */
    @Transactional(readOnly = true)
    public Page<HomeworkSubmissionVO> getSubmissions(Long teacherId, Long homeworkId,
                                                      Page<HomeworkSubmissionVO> page) {
        getAndCheckOwner(teacherId, homeworkId);
        return homeworkSubmissionMapper.selectByHomeworkId(page, homeworkId);
    }

    // ==================== 学生端 ====================

    /**
     * 提交作业
     */
    @Transactional
    public HomeworkSubmissionVO submit(Long studentId, SubmitHomeworkDTO dto) {
        Homework hw = homeworkMapper.selectById(dto.getHomeworkId());
        if (hw == null) {
            throw new BizException(ErrorCode.HOMEWORK_NOT_FOUND);
        }
        if (!"published".equals(hw.getStatus())) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "作业未发布或已关闭");
        }

        // 检查截止时间
        if (hw.getDeadline() != null && LocalDateTime.now().isAfter(hw.getDeadline())) {
            throw new BizException(ErrorCode.HOMEWORK_DEADLINE_PASSED);
        }

        // 检查是否已提交
        int count = homeworkSubmissionMapper.countByHomeworkAndStudent(dto.getHomeworkId(), studentId);
        if (count > 0) {
            throw new BizException(ErrorCode.HOMEWORK_ALREADY_SUBMITTED);
        }

        // 校验项目归属（如果提交的是 Scratch 项目）
        if (dto.getProjectId() != null) {
            Integer projectCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM project WHERE id = ? AND user_id = ? AND deleted = 0",
                    Integer.class, dto.getProjectId(), studentId);
            if (projectCount == null || projectCount == 0) {
                throw new BizException(ErrorCode.PROJECT_NO_PERMISSION);
            }
        }

        HomeworkSubmission submission = new HomeworkSubmission();
        submission.setHomeworkId(dto.getHomeworkId());
        submission.setStudentId(studentId);
        submission.setProjectId(dto.getProjectId());
        submission.setAnswers(dto.getAnswers());
        submission.setStatus("submitted");
        homeworkSubmissionMapper.insert(submission);

        // 原子递增提交人数
        jdbcTemplate.update("UPDATE homework SET submit_count = submit_count + 1 WHERE id = ?", hw.getId());

        // 发布积分事件：完成作业
        eventPublisher.publishEvent(new PointEvent(this, studentId, PointEvent.PointAction.COMPLETE_HOMEWORK, hw.getId()));

        log.info("学生提交作业: studentId={}, homeworkId={}", studentId, dto.getHomeworkId());

        HomeworkSubmissionVO vo = new HomeworkSubmissionVO();
        BeanUtils.copyProperties(submission, vo);
        return vo;
    }

    /**
     * 查询我的提交记录
     */
    @Transactional(readOnly = true)
    public Page<HomeworkSubmissionVO> mySubmissions(Long studentId, Long classId,
                                                     Page<HomeworkSubmissionVO> page) {
        // 参数化分页（避免 SQL 拼接）
        long offset = (page.getCurrent() - 1) * page.getSize();
        if (offset < 0) offset = 0;

        List<HomeworkSubmissionVO> records = jdbcTemplate.query(
                "SELECT hs.id, hs.homework_id, hs.student_id, hs.project_id, hs.score, " +
                        "hs.comment, hs.status, hs.created_at, hs.graded_at " +
                        "FROM homework_submission hs JOIN homework h ON hs.homework_id = h.id " +
                        "WHERE hs.student_id = ? AND h.class_id = ? AND hs.deleted = 0 " +
                        "ORDER BY hs.created_at DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> {
                    HomeworkSubmissionVO vo = new HomeworkSubmissionVO();
                    vo.setId(rs.getLong("id"));
                    vo.setHomeworkId(rs.getLong("homework_id"));
                    vo.setStudentId(rs.getLong("student_id"));
                    vo.setProjectId(rs.getLong("project_id"));
                    vo.setScore(rs.getInt("score"));
                    vo.setComment(rs.getString("comment"));
                    vo.setStatus(rs.getString("status"));
                    vo.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    return vo;
                },
                studentId, classId, page.getSize(), offset);

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM homework_submission hs JOIN homework h ON hs.homework_id = h.id " +
                        "WHERE hs.student_id = ? AND h.class_id = ? AND hs.deleted = 0",
                Long.class, studentId, classId);

        Page<HomeworkSubmissionVO> result = new Page<>(page.getCurrent(), page.getSize(), total != null ? total : 0);
        result.setRecords(records);
        return result;
    }

    // ==================== 私有方法 ====================

    private Homework getAndCheckOwner(Long teacherId, Long homeworkId) {
        Homework hw = homeworkMapper.selectById(homeworkId);
        if (hw == null) {
            throw new BizException(ErrorCode.HOMEWORK_NOT_FOUND);
        }
        if (!hw.getTeacherId().equals(teacherId)) {
            throw new BizException(ErrorCode.USER_NO_PERMISSION);
        }
        return hw;
    }

    private HomeworkVO toVO(Homework hw) {
        HomeworkVO vo = new HomeworkVO();
        BeanUtils.copyProperties(hw, vo);
        // 查询班级名称
        try {
            String className = jdbcTemplate.queryForObject(
                    "SELECT name FROM class WHERE id = ? AND deleted = 0", String.class, hw.getClassId());
            vo.setClassName(className);
        } catch (Exception e) {
            // 忽略
        }
        return vo;
    }

    private Page<HomeworkVO> toVOPage(Page<Homework> page) {
        if (page.getRecords().isEmpty()) {
            return new Page<>(page.getCurrent(), page.getSize(), 0);
        }

        // 批量查询班级名称（避免 N+1，参数化查询）
        Set<Long> classIds = page.getRecords().stream()
                .map(Homework::getClassId)
                .collect(Collectors.toSet());
        String inClause = classIds.stream().map(id -> "?").collect(Collectors.joining(","));
        Map<Long, String> classNameMap = new HashMap<>();
        try {
            jdbcTemplate.query(
                    "SELECT id, name FROM class WHERE id IN (" + inClause + ") AND deleted = 0",
                    (org.springframework.jdbc.core.RowCallbackHandler) rs ->
                            classNameMap.put(rs.getLong("id"), rs.getString("name")),
                    classIds.toArray());
        } catch (Exception e) {
            log.warn("批量查询班级名称失败: {}", e.getMessage());
        }

        Page<HomeworkVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(hw -> {
            HomeworkVO vo = new HomeworkVO();
            BeanUtils.copyProperties(hw, vo);
            vo.setClassName(classNameMap.get(hw.getClassId()));
            return vo;
        }).toList());
        return voPage;
    }
}
