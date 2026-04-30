package com.scratch.community.module.classroom.service;

import com.scratch.community.common.exception.BizException;
import com.scratch.community.common.result.ErrorCode;
import com.scratch.community.module.classroom.dto.GradeHomeworkDTO;
import com.scratch.community.module.classroom.entity.Homework;
import com.scratch.community.module.classroom.entity.HomeworkSubmission;
import com.scratch.community.module.classroom.mapper.HomeworkMapper;
import com.scratch.community.module.classroom.mapper.HomeworkSubmissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 作业批改服务
 *
 * <p>从 {@link HomeworkService} 中拆分出的评分逻辑，
 * 负责作业提交的批改、分数校验、graded_count 维护。
 *
 * <p>拆分目的：
 * <ul>
 *   <li>单一职责：批改逻辑与作业管理逻辑分离</li>
 *   <li>便于独立测试和维护</li>
 *   <li>为未来 AI 辅助批改预留扩展点</li>
 * </ul>
 *
 * @author scratch-community
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HomeworkGradingService {

    private final HomeworkMapper homeworkMapper;
    private final HomeworkSubmissionMapper homeworkSubmissionMapper;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 批改作业
     *
     * @param teacherId 教师 ID（用于权限校验）
     * @param dto 批改信息（提交 ID、分数、评语）
     */
    @Transactional
    public void grade(Long teacherId, GradeHomeworkDTO dto) {
        HomeworkSubmission submission = homeworkSubmissionMapper.selectById(dto.getSubmissionId());
        if (submission == null) {
            throw new BizException(ErrorCode.HOMEWORK_NOT_FOUND);
        }

        // 校验作业归属
        Homework hw = homeworkMapper.selectById(submission.getHomeworkId());
        if (hw == null || !hw.getTeacherId().equals(teacherId)) {
            throw new BizException(ErrorCode.USER_NO_PERMISSION);
        }

        // 分数校验
        if (dto.getScore() < 0 || dto.getScore() > hw.getTotalScore()) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(),
                    "分数必须在 0-" + hw.getTotalScore() + " 之间");
        }

        // 判断是首次批改还是重新批改
        boolean isRegrade = "graded".equals(submission.getStatus());

        submission.setScore(dto.getScore());
        submission.setComment(dto.getComment());
        submission.setStatus("graded");
        submission.setGradedAt(LocalDateTime.now());
        homeworkSubmissionMapper.updateById(submission);

        // 仅首次批改时递增 graded_count（避免重复批改导致计数虚增）
        if (!isRegrade) {
            jdbcTemplate.update(
                    "UPDATE homework SET graded_count = graded_count + 1 WHERE id = ?",
                    hw.getId());
        }

        log.info("批改作业: teacherId={}, submissionId={}, score={}, regrade={}",
                teacherId, dto.getSubmissionId(), dto.getScore(), isRegrade);
    }
}
