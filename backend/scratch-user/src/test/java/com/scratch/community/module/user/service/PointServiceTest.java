package com.scratch.community.module.user.service;

import com.scratch.community.common.repository.CrossModuleQueryRepository;
import com.scratch.community.module.user.mapper.PointLogMapper;
import com.scratch.community.module.user.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PointService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @InjectMocks
    private PointService pointService;

    @Mock
    private PointLogMapper pointLogMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private CrossModuleQueryRepository crossModuleQuery;

    @Nested
    @DisplayName("等级计算")
    class LevelCalculationTests {

        @Test
        @DisplayName("0 积分 = 等级 1")
        void calculateLevel_zero() {
            assertEquals(1, pointService.calculateLevel(0));
        }

        @Test
        @DisplayName("99 积分 = 等级 1")
        void calculateLevel_99() {
            assertEquals(1, pointService.calculateLevel(99));
        }

        @Test
        @DisplayName("100 积分 = 等级 2")
        void calculateLevel_100() {
            assertEquals(2, pointService.calculateLevel(100));
        }

        @Test
        @DisplayName("300 积分 = 等级 3")
        void calculateLevel_300() {
            assertEquals(3, pointService.calculateLevel(300));
        }

        @Test
        @DisplayName("12000 积分 = 等级 8")
        void calculateLevel_12000() {
            assertEquals(8, pointService.calculateLevel(12000));
        }

        @Test
        @DisplayName("超过 12000 仍为等级 8")
        void calculateLevel_max() {
            assertEquals(8, pointService.calculateLevel(99999));
        }
    }

    @Nested
    @DisplayName("等级名称")
    class LevelNameTests {

        @Test
        @DisplayName("等级 1 = 编程新手")
        void levelName_1() {
            assertEquals("编程新手", pointService.getLevelName(1));
        }

        @Test
        @DisplayName("等级 8 = 编程大师")
        void levelName_8() {
            assertEquals("编程传奇", pointService.getLevelName(8));
        }

        @Test
        @DisplayName("无效等级返回默认")
        void levelName_invalid() {
            assertEquals("未知等级", pointService.getLevelName(0));
            assertEquals("未知等级", pointService.getLevelName(9));
        }
    }

    @Nested
    @DisplayName("积分常量")
    class ConstantsTests {

        @Test
        @DisplayName("积分规则值正确")
        void constants() {
            assertEquals(5, PointService.POINTS_DAILY_CHECKIN);
            assertEquals(10, PointService.POINTS_PUBLISH_PROJECT);
            assertEquals(2, PointService.POINTS_RECEIVE_LIKE);
            assertEquals(15, PointService.POINTS_AC_SUBMISSION);
            assertEquals(20, PointService.POINTS_COMPLETE_HOMEWORK);
        }
    }

    @Nested
    @DisplayName("积分排行榜")
    class RankingTests {

        @Test
        @DisplayName("排行榜调用 CrossModuleQueryRepository")
        void getPointRanking() {
            when(crossModuleQuery.getPointRanking(10)).thenReturn(List.of(
                    new HashMap<>(Map.of("id", 1L, "nickname", "Alice", "points", 500)),
                    new HashMap<>(Map.of("id", 2L, "nickname", "Bob", "points", 300))
            ));

            List<Map<String, Object>> result = pointService.getPointRanking(10);

            assertEquals(2, result.size());
            verify(crossModuleQuery).getPointRanking(10);
        }
    }
}
