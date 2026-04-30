package com.scratch.community.module.classroom.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scratch.community.common.exception.BizException;
import com.scratch.community.module.classroom.dto.CreateHomeworkDTO;
import com.scratch.community.module.classroom.dto.GradeHomeworkDTO;
import com.scratch.community.module.classroom.dto.SubmitHomeworkDTO;
import com.scratch.community.module.classroom.entity.Homework;
import com.scratch.community.module.classroom.entity.HomeworkSubmission;
import com.scratch.community.module.classroom.mapper.HomeworkMapper;
import com.scratch.community.module.classroom.mapper.HomeworkSubmissionMapper;
import com.scratch.community.module.classroom.vo.HomeworkSubmissionVO;
import com.scratch.community.module.classroom.vo.HomeworkVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * HomeworkService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class HomeworkServiceTest {

    @InjectMocks
    private HomeworkService homeworkService;

    @InjectMocks
    private HomeworkGradingService homeworkGradingService;

    @Mock
    private HomeworkMapper homeworkMapper;

    @Mock
    private HomeworkSubmissionMapper homeworkSubmissionMapper;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private static final Long TEACHER_ID = 1L;
    private static final Long STUDENT_ID = 2L;
    private static final Long CLASS_ID = 10L;
    private static final Long HOMEWORK_ID = 100L;

    private Homework publishedHomework;

    @BeforeEach
    void setUp() {
        publishedHomework = new Homework();
        publishedHomework.setId(HOMEWORK_ID);
        publishedHomework.setClassId(CLASS_ID);
        publishedHomework.setTeacherId(TEACHER_ID);
        publishedHomework.setTitle("Scratch 第一课");
        publishedHomework.setType("scratch_project");
        publishedHomework.setTotalScore(100);
        publishedHomework.setStatus("published");
        publishedHomework.setSubmitCount(0);
        publishedHomework.setGradedCount(0);
    }

    // ==================== 创建作业 ====================

    @Nested
    @DisplayName("创建作业测试")
    class CreateTests {

        @Test
        @DisplayName("正常创建作业")
        void create_success() {
            when(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM class WHERE id = ? AND teacher_id = ? AND deleted = 0",
                    Integer.class, CLASS_ID, TEACHER_ID)).thenReturn(1);
            when(homeworkMapper.insert(any(Homework.class))).thenReturn(1);

            CreateHomeworkDTO dto = new CreateHomeworkDTO();
            dto.setClassId(CLASS_ID);
            dto.setTitle("Scratch 第一课");
            dto.setTotalScore(100);

            HomeworkVO result = homeworkService.create(TEACHER_ID, dto);

            assertNotNull(result);
            assertEquals("draft", result.getStatus());
            verify(homeworkMapper).insert(any(Homework.class));
        }

        @Test
        @DisplayName("班级不存在 → 抛异常")
        void create_classNotFound() {
            when(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM class WHERE id = ? AND teacher_id = ? AND deleted = 0",
                    Integer.class, CLASS_ID, TEACHER_ID)).thenReturn(0);

            CreateHomeworkDTO dto = new CreateHomeworkDTO();
            dto.setClassId(CLASS_ID);
            dto.setTitle("作业");

            assertThrows(BizException.class, () -> homeworkService.create(TEACHER_ID, dto));
        }

        @Test
        @DisplayName("非该教师的班级 → 抛异常")
        void create_notOwner() {
            when(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM class WHERE id = ? AND teacher_id = ? AND deleted = 0",
                    Integer.class, CLASS_ID, TEACHER_ID)).thenReturn(0);

            CreateHomeworkDTO dto = new CreateHomeworkDTO();
            dto.setClassId(CLASS_ID);
            dto.setTitle("作业");

            assertThrows(BizException.class, () -> homeworkService.create(TEACHER_ID, dto));
        }
    }

    // ==================== 发布/关闭 ====================

    @Nested
    @DisplayName("发布/关闭作业测试")
    class PublishCloseTests {

        @Test
        @DisplayName("正常发布作业")
        void publish_success() {
            when(homeworkMapper.selectById(HOMEWORK_ID)).thenReturn(publishedHomework);
            // 改为 draft 状态来测试发布
            publishedHomework.setStatus("draft");

            homeworkService.publish(TEACHER_ID, HOMEWORK_ID);

            assertEquals("published", publishedHomework.getStatus());
            verify(homeworkMapper).updateById(publishedHomework);
        }

        @Test
        @DisplayName("非教师本人发布 → 抛异常")
        void publish_notOwner() {
            publishedHomework.setTeacherId(999L);
            when(homeworkMapper.selectById(HOMEWORK_ID)).thenReturn(publishedHomework);

            assertThrows(BizException.class,
                    () -> homeworkService.publish(TEACHER_ID, HOMEWORK_ID));
        }

        @Test
        @DisplayName("作业不存在 → 抛异常")
        void publish_notFound() {
            when(homeworkMapper.selectById(HOMEWORK_ID)).thenReturn(null);

            assertThrows(BizException.class,
                    () -> homeworkService.publish(TEACHER_ID, HOMEWORK_ID));
        }

        @Test
        @DisplayName("正常关闭作业")
        void close_success() {
            when(homeworkMapper.selectById(HOMEWORK_ID)).thenReturn(publishedHomework);

            homeworkService.close(TEACHER_ID, HOMEWORK_ID);

            assertEquals("closed", publishedHomework.getStatus());
        }
    }

    // ==================== 提交作业 ====================

    @Nested
    @DisplayName("提交作业测试")
    class SubmitTests {

        @Test
        @DisplayName("正常提交作业")
        void submit_success() {
            when(homeworkMapper.selectById(HOMEWORK_ID)).thenReturn(publishedHomework);
            when(homeworkSubmissionMapper.countByHomeworkAndStudent(HOMEWORK_ID, STUDENT_ID)).thenReturn(0);
            // 项目归属校验
            when(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM project WHERE id = ? AND user_id = ? AND deleted = 0",
                    Integer.class, 10L, STUDENT_ID)).thenReturn(1);
            when(homeworkSubmissionMapper.insert(any(HomeworkSubmission.class))).thenReturn(1);

            SubmitHomeworkDTO dto = new SubmitHomeworkDTO();
            dto.setHomeworkId(HOMEWORK_ID);
            dto.setProjectId(10L);

            HomeworkSubmissionVO result = homeworkService.submit(STUDENT_ID, dto);

            assertNotNull(result);
            verify(jdbcTemplate).update(
                    "UPDATE homework SET submit_count = submit_count + 1 WHERE id = ?", HOMEWORK_ID);
        }

        @Test
        @DisplayName("作业不存在 → 抛异常")
        void submit_homeworkNotFound() {
            when(homeworkMapper.selectById(HOMEWORK_ID)).thenReturn(null);

            SubmitHomeworkDTO dto = new SubmitHomeworkDTO();
            dto.setHomeworkId(HOMEWORK_ID);

            assertThrows(BizException.class, () -> homeworkService.submit(STUDENT_ID, dto));
        }

        @Test
        @DisplayName("作业未发布 → 抛异常")
        void submit_notPublished() {
            publishedHomework.setStatus("draft");
            when(homeworkMapper.selectById(HOMEWORK_ID)).thenReturn(publishedHomework);

            SubmitHomeworkDTO dto = new SubmitHomeworkDTO();
            dto.setHomeworkId(HOMEWORK_ID);

            assertThrows(BizException.class, () -> homeworkService.submit(STUDENT_ID, dto));
        }

        @Test
        @DisplayName("重复提交 → 抛异常")
        void submit_duplicate() {
            when(homeworkMapper.selectById(HOMEWORK_ID)).thenReturn(publishedHomework);
            when(homeworkSubmissionMapper.countByHomeworkAndStudent(HOMEWORK_ID, STUDENT_ID)).thenReturn(1);

            SubmitHomeworkDTO dto = new SubmitHomeworkDTO();
            dto.setHomeworkId(HOMEWORK_ID);

            assertThrows(BizException.class, () -> homeworkService.submit(STUDENT_ID, dto));
        }

        @Test
        @DisplayName("超过截止时间 → 抛异常")
        void submit_deadlinePassed() {
            publishedHomework.setDeadline(LocalDateTime.now().minusDays(1));
            when(homeworkMapper.selectById(HOMEWORK_ID)).thenReturn(publishedHomework);

            SubmitHomeworkDTO dto = new SubmitHomeworkDTO();
            dto.setHomeworkId(HOMEWORK_ID);

            assertThrows(BizException.class, () -> homeworkService.submit(STUDENT_ID, dto));
        }

        @Test
        @DisplayName("项目不属于该学生 → 抛异常")
        void submit_projectNotOwned() {
            when(homeworkMapper.selectById(HOMEWORK_ID)).thenReturn(publishedHomework);
            when(homeworkSubmissionMapper.countByHomeworkAndStudent(HOMEWORK_ID, STUDENT_ID)).thenReturn(0);
            when(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM project WHERE id = ? AND user_id = ? AND deleted = 0",
                    Integer.class, 10L, STUDENT_ID)).thenReturn(0);

            SubmitHomeworkDTO dto = new SubmitHomeworkDTO();
            dto.setHomeworkId(HOMEWORK_ID);
            dto.setProjectId(10L);

            assertThrows(BizException.class, () -> homeworkService.submit(STUDENT_ID, dto));
        }
    }

    // ==================== 批改作业 ====================

    @Nested
    @DisplayName("批改作业测试")
    class GradeTests {

        @Test
        @DisplayName("正常批改作业")
        void grade_success() {
            HomeworkSubmission submission = new HomeworkSubmission();
            submission.setId(200L);
            submission.setHomeworkId(HOMEWORK_ID);
            submission.setStatus("submitted");
            when(homeworkSubmissionMapper.selectById(200L)).thenReturn(submission);
            when(homeworkMapper.selectById(HOMEWORK_ID)).thenReturn(publishedHomework);
            when(homeworkSubmissionMapper.updateById(any(HomeworkSubmission.class))).thenReturn(1);

            GradeHomeworkDTO dto = new GradeHomeworkDTO();
            dto.setSubmissionId(200L);
            dto.setScore(85);
            dto.setComment("做得不错！");

            homeworkGradingService.grade(TEACHER_ID, dto);

            assertEquals("graded", submission.getStatus());
            assertEquals(85, submission.getScore());
            verify(jdbcTemplate).update(
                    "UPDATE homework SET graded_count = graded_count + 1 WHERE id = ?", HOMEWORK_ID);
        }

        @Test
        @DisplayName("重新批改 → 不重复递增 graded_count")
        void grade_regrade() {
            HomeworkSubmission submission = new HomeworkSubmission();
            submission.setId(200L);
            submission.setHomeworkId(HOMEWORK_ID);
            submission.setStatus("graded"); // 已批改过
            when(homeworkSubmissionMapper.selectById(200L)).thenReturn(submission);
            when(homeworkMapper.selectById(HOMEWORK_ID)).thenReturn(publishedHomework);

            GradeHomeworkDTO dto = new GradeHomeworkDTO();
            dto.setSubmissionId(200L);
            dto.setScore(90);

            homeworkGradingService.grade(TEACHER_ID, dto);

            // 重新批改不应递增 graded_count
            verify(jdbcTemplate, never()).update(
                    "UPDATE homework SET graded_count = graded_count + 1 WHERE id = ?", HOMEWORK_ID);
        }

        @Test
        @DisplayName("分数超范围 → 抛异常")
        void grade_scoreOutOfRange() {
            HomeworkSubmission submission = new HomeworkSubmission();
            submission.setId(200L);
            submission.setHomeworkId(HOMEWORK_ID);
            submission.setStatus("submitted");
            when(homeworkSubmissionMapper.selectById(200L)).thenReturn(submission);
            when(homeworkMapper.selectById(HOMEWORK_ID)).thenReturn(publishedHomework);

            GradeHomeworkDTO dto = new GradeHomeworkDTO();
            dto.setSubmissionId(200L);
            dto.setScore(150); // 超过满分 100

            assertThrows(BizException.class, () -> homeworkGradingService.grade(TEACHER_ID, dto));
        }

        @Test
        @DisplayName("非教师本人批改 → 抛异常")
        void grade_notOwner() {
            HomeworkSubmission submission = new HomeworkSubmission();
            submission.setId(200L);
            submission.setHomeworkId(HOMEWORK_ID);
            submission.setStatus("submitted");
            when(homeworkSubmissionMapper.selectById(200L)).thenReturn(submission);
            publishedHomework.setTeacherId(999L);
            when(homeworkMapper.selectById(HOMEWORK_ID)).thenReturn(publishedHomework);

            GradeHomeworkDTO dto = new GradeHomeworkDTO();
            dto.setSubmissionId(200L);
            dto.setScore(80);

            assertThrows(BizException.class, () -> homeworkGradingService.grade(TEACHER_ID, dto));
        }
    }
}
