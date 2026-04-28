package com.scratch.community.module.judge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scratch.community.common.event.PointEvent;
import com.scratch.community.common.exception.BizException;
import com.scratch.community.module.judge.dto.SubmitDTO;
import com.scratch.community.module.judge.entity.Problem;
import com.scratch.community.module.judge.entity.Submission;
import com.scratch.community.module.judge.mapper.ProblemMapper;
import com.scratch.community.module.judge.mapper.SubmissionMapper;
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
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * JudgeService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class JudgeServiceTest {

    @InjectMocks
    private JudgeService judgeService;

    @Mock
    private ProblemMapper problemMapper;

    @Mock
    private SubmissionMapper submissionMapper;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private Problem choiceProblem;
    private SubmitDTO submitDTO;

    @BeforeEach
    void setUp() {
        choiceProblem = new Problem();
        choiceProblem.setId(1L);
        choiceProblem.setTitle("1+1=?");
        choiceProblem.setType("choice");
        choiceProblem.setAnswer("B");
        choiceProblem.setStatus("published");
        choiceProblem.setSubmitCount(0);
        choiceProblem.setAcceptCount(0);

        submitDTO = new SubmitDTO();
        submitDTO.setProblemId(1L);
        submitDTO.setAnswer("B");
    }

    @Nested
    @DisplayName("提交答案测试")
    class SubmitTests {

        @Test
        @DisplayName("题目不存在")
        void submit_problemNotFound() {
            when(problemMapper.selectById(1L)).thenReturn(null);

            assertThrows(BizException.class, () -> judgeService.submit(1L, submitDTO));
        }

        @Test
        @DisplayName("题目未发布")
        void submit_problemNotPublished() {
            choiceProblem.setStatus("draft");
            when(problemMapper.selectById(1L)).thenReturn(choiceProblem);

            assertThrows(BizException.class, () -> judgeService.submit(1L, submitDTO));
        }

        @Test
        @DisplayName("重复提交检查")
        void submit_duplicate() {
            when(problemMapper.selectById(1L)).thenReturn(choiceProblem);
            when(submissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            assertThrows(BizException.class, () -> judgeService.submit(1L, submitDTO));
        }
    }

    @Nested
    @DisplayName("即时判题测试")
    class InstantJudgeTests {

        @Test
        @DisplayName("选择题答对 → AC")
        void judgeInstant_correctAnswer() {
            Submission submission = new Submission();
            submission.setAnswer("B");

            judgeService.judgeInstant(submission, choiceProblem);

            assertEquals("AC", submission.getVerdict());
            assertNotNull(submission.getJudgeDetail());
            verify(submissionMapper).updateById(submission);
        }

        @Test
        @DisplayName("选择题答错 → WA")
        void judgeInstant_wrongAnswer() {
            Submission submission = new Submission();
            submission.setAnswer("A");

            judgeService.judgeInstant(submission, choiceProblem);

            assertEquals("WA", submission.getVerdict());
            verify(submissionMapper).updateById(submission);
        }

        @Test
        @DisplayName("答案大小写不敏感")
        void judgeInstant_caseInsensitive() {
            Submission submission = new Submission();
            submission.setAnswer("b");

            judgeService.judgeInstant(submission, choiceProblem);

            assertEquals("AC", submission.getVerdict());
        }

        @Test
        @DisplayName("答案为空 → WA")
        void judgeInstant_nullAnswer() {
            Submission submission = new Submission();
            submission.setAnswer(null);

            judgeService.judgeInstant(submission, choiceProblem);

            assertEquals("WA", submission.getVerdict());
        }
    }

    @Nested
    @DisplayName("题目统计更新测试")
    class StatsTests {

        @Test
        @DisplayName("AC 时递增 submit_count 和 accept_count")
        void updateProblemStats_ac() {
            judgeService.updateProblemStats(choiceProblem, "AC");

            verify(problemMapper, times(2)).update(eq(null), any());
        }

        @Test
        @DisplayName("WA 时只递增 submit_count")
        void updateProblemStats_wa() {
            judgeService.updateProblemStats(choiceProblem, "WA");

            verify(problemMapper, times(1)).update(eq(null), any());
        }
    }
}
