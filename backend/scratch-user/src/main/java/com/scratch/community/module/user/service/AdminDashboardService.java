package com.scratch.community.module.user.service;

import com.scratch.community.module.user.vo.DashboardVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 管理后台数据统计服务
 *
 * <p>从 {@link AdminService} 中拆分出的统计逻辑，
 * 负责管理后台 Dashboard 的数据聚合查询。
 *
 * <p>拆分目的：
 * <ul>
 *   <li>单一职责：用户管理与数据统计分离</li>
 *   <li>统计逻辑独立，便于优化查询性能</li>
 *   <li>为未来缓存策略预留扩展点</li>
 * </ul>
 *
 * @author scratch-community
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 获取管理后台统计数据
     *
     * <p>优化：合并为 2 次查询（用户+今日+项目+题目，提交+教室+审核），减少数据库往返。
     *
     * @return Dashboard 统计数据
     */
    @Transactional(readOnly = true)
    public DashboardVO getDashboard() {
        DashboardVO vo = new DashboardVO();
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);

        // 查询 1: 用户 + 项目 + 题目统计
        jdbcTemplate.query(
                "SELECT " +
                        "(SELECT COUNT(*) FROM user WHERE deleted = 0) AS total_users, " +
                        "(SELECT COUNT(*) FROM user WHERE deleted = 0 AND created_at >= ?) AS today_new_users, " +
                        "(SELECT COUNT(*) FROM project WHERE deleted = 0) AS total_projects, " +
                        "(SELECT COUNT(*) FROM project WHERE deleted = 0 AND status = 'published') AS published_projects, " +
                        "(SELECT COUNT(*) FROM problem WHERE deleted = 0) AS total_problems",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
                    vo.setTotalUsers(rs.getLong("total_users"));
                    vo.setTodayNewUsers(rs.getLong("today_new_users"));
                    vo.setTotalProjects(rs.getLong("total_projects"));
                    vo.setPublishedProjects(rs.getLong("published_projects"));
                    vo.setTotalProblems(rs.getLong("total_problems"));
                },
                todayStart);

        // 查询 2: 提交 + 教室 + 审核统计
        jdbcTemplate.query(
                "SELECT " +
                        "(SELECT COUNT(*) FROM submission WHERE deleted = 0) AS total_submissions, " +
                        "(SELECT COUNT(*) FROM submission WHERE deleted = 0 AND verdict = 'AC') AS ac_submissions, " +
                        "(SELECT COUNT(*) FROM class WHERE deleted = 0) AS total_classes, " +
                        "(SELECT COUNT(*) FROM homework WHERE deleted = 0) AS total_homework, " +
                        "(SELECT COUNT(*) FROM content_audit_log WHERE status = 'pending') AS pending_audits",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
                    vo.setTotalSubmissions(rs.getLong("total_submissions"));
                    vo.setAcSubmissions(rs.getLong("ac_submissions"));
                    vo.setTotalClasses(rs.getLong("total_classes"));
                    vo.setTotalHomework(rs.getLong("total_homework"));
                    vo.setPendingAudits(rs.getLong("pending_audits"));
                });

        return vo;
    }
}
