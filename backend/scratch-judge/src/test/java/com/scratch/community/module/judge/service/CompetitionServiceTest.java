package com.scratch.community.module.judge.service;

import com.scratch.community.common.exception.BizException;
import com.scratch.community.module.judge.dto.CreateCompetitionDTO;
import com.scratch.community.module.judge.entity.Competition;
import com.scratch.community.module.judge.mapper.*;
import com.scratch.community.module.judge.vo.CompetitionVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompetitionServiceTest {

    @InjectMocks
    private CompetitionService competitionService;

    @Mock private CompetitionMapper competitionMapper;
    @Mock private CompetitionRegistrationMapper registrationMapper;
    @Mock private CompetitionRankingMapper rankingMapper;
    @Mock private ProblemMapper problemMapper;
    @Mock private SubmissionMapper submissionMapper;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private JudgeService judgeService;

    private CreateCompetitionDTO validDTO;

    @BeforeEach
    void setUp() {
        validDTO = new CreateCompetitionDTO();
        validDTO.setTitle("春季编程竞赛");
        validDTO.setDescription("面向少儿的 Scratch 编程竞赛");
        validDTO.setType("TIMED");
        validDTO.setStartTime(LocalDateTime.now().plusDays(1));
        validDTO.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        validDTO.setProblemIds(List.of(1L, 2L, 3L));
    }

    @Nested
    @DisplayName("创建竞赛")
    class CreateTests {
        @Test
        @DisplayName("正常创建竞赛")
        void create_success() {
            when(competitionMapper.insert(any(Competition.class))).thenReturn(1);
            CompetitionVO result = competitionService.create(1L, validDTO);
            assertNotNull(result);
            assertEquals("春季编程竞赛", result.getTitle());
            assertEquals("DRAFT", result.getStatus());
        }
    }

    @Nested
    @DisplayName("报名")
    class RegisterTests {
        @Test
        @DisplayName("正常报名")
        void register_success() {
            Competition comp = new Competition();
            comp.setId(1L);
            comp.setStatus("PUBLISHED");
            comp.setStartTime(LocalDateTime.now().plusDays(1));
            when(competitionMapper.selectById(1L)).thenReturn(comp);
            when(registrationMapper.countByCompetitionAndUser(1L, 10L)).thenReturn(0);
            when(registrationMapper.insert(any())).thenReturn(1);
            when(jdbcTemplate.update(anyString(), (Object[]) any())).thenReturn(1);

            assertDoesNotThrow(() -> competitionService.register(1L, 10L));
        }

        @Test
        @DisplayName("已结束的竞赛不能报名")
        void register_ended() {
            Competition comp = new Competition();
            comp.setId(1L);
            comp.setStatus("ENDED");
            when(competitionMapper.selectById(1L)).thenReturn(comp);
            assertThrows(BizException.class, () -> competitionService.register(1L, 10L));
        }

        @Test
        @DisplayName("重复报名幂等")
        void register_duplicate() {
            Competition comp = new Competition();
            comp.setId(1L);
            comp.setStatus("PUBLISHED");
            comp.setStartTime(LocalDateTime.now().plusDays(1));
            when(competitionMapper.selectById(1L)).thenReturn(comp);
            when(registrationMapper.countByCompetitionAndUser(1L, 10L)).thenReturn(1);

            assertDoesNotThrow(() -> competitionService.register(1L, 10L));
            verify(registrationMapper, never()).insert(any());
        }
    }

    @Nested
    @DisplayName("状态自动流转")
    class StatusTransitionTests {
        @Test
        @DisplayName("autoUpdateStatus 使用 jdbcTemplate 批量更新")
        void autoStatus_usesJdbcTemplate() {
            when(jdbcTemplate.update(anyString(), (Object[]) any())).thenReturn(1);
            competitionService.autoUpdateStatus();
            verify(jdbcTemplate, times(2)).update(anyString(), (Object[]) any());
        }
    }
}
