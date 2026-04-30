package com.scratch.community.module.judge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scratch.community.module.judge.entity.CompetitionRanking;
import com.scratch.community.module.judge.entity.CompetitionRegistration;
import com.scratch.community.module.judge.mapper.CompetitionRankingMapper;
import com.scratch.community.module.judge.mapper.CompetitionRegistrationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 竞赛排名计算服务
 *
 * <p>从 CompetitionService 中抽取的排名计算逻辑。
 * 负责排名的增量更新、全量重排和排序。
 *
 * <p>查询次数对比 (50 人 × 5 题)：
 * <ul>
 *   <li>submitAnswer 增量: 6 次</li>
 *   <li>updateRankings 全量: 4 次</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompetitionRankingService {

    private final CompetitionRankingMapper rankingMapper;
    private final CompetitionRegistrationMapper registrationMapper;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 排名计算结果（纯数据，用于方法间传递）
     */
    public static class RankingCalcResult {
        public final int totalScore;
        public final int solvedCount;
        public final int penalty;
        public final Map<Long, Map<String, Object>> problemDetails;

        public RankingCalcResult(int totalScore, int solvedCount, int penalty,
                                 Map<Long, Map<String, Object>> problemDetails) {
            this.totalScore = totalScore;
            this.solvedCount = solvedCount;
            this.penalty = penalty;
            this.problemDetails = problemDetails;
        }
    }

    /**
     * 计算单个用户的排名数据（纯计算，无 DB 操作）
     */
    public RankingCalcResult calculateUserRanking(
            Long competitionId, Long userId,
            List<Long> problemIds, List<Integer> problemScores,
            Map<Long, List<Map<String, Object>>> subsByProblem) {

        int totalScore = 0;
        int solvedCount = 0;
        int penalty = 0;
        Map<Long, Map<String, Object>> problemDetails = new LinkedHashMap<>();

        for (int i = 0; i < problemIds.size(); i++) {
            Long pid = problemIds.get(i);
            int maxScore = i < problemScores.size() ? problemScores.get(i) : 100;

            List<Map<String, Object>> submissions = subsByProblem.getOrDefault(pid, Collections.emptyList());
            boolean solved = false;
            for (Map<String, Object> sub : submissions) {
                if ("AC".equals(sub.get("verdict"))) {
                    solved = true;
                    break;
                }
            }
            int attempts = submissions.size();

            Map<String, Object> detail = new HashMap<>();
            detail.put("problemId", pid);
            detail.put("attempts", attempts);
            detail.put("solved", solved);

            if (solved) {
                totalScore += maxScore;
                solvedCount++;
                penalty += Math.max(0, (attempts - 1)) * 20;
                detail.put("score", maxScore);
            } else {
                detail.put("score", 0);
            }
            problemDetails.put(pid, detail);
        }

        return new RankingCalcResult(totalScore, solvedCount, penalty, problemDetails);
    }

    /**
     * 增量更新单个用户的排名（用于 submitAnswer）
     *
     * <p>只查询该用户在竞赛题目上的提交记录，不查其他用户。
     */
    @Transactional
    public void recalculateUserRanking(Long competitionId, Long userId,
                                        List<Long> problemIds, List<Integer> problemScores) {
        // 查询该用户在每道题的提交（P 次查询）
        Map<Long, List<Map<String, Object>>> subsByProblem = new HashMap<>();
        for (Long pid : problemIds) {
            List<Map<String, Object>> submissions = jdbcTemplate.query(
                    "SELECT verdict, created_at FROM submission " +
                            "WHERE user_id = ? AND problem_id = ? AND deleted = 0 " +
                            "ORDER BY created_at ASC",
                    (rs, rowNum) -> {
                        Map<String, Object> row = new HashMap<>();
                        row.put("verdict", rs.getString("verdict"));
                        row.put("createdAt", rs.getTimestamp("created_at").toLocalDateTime());
                        return row;
                    },
                    userId, pid);
            if (!submissions.isEmpty()) {
                subsByProblem.put(pid, submissions);
            }
        }

        // 计算排名数据（内存）
        RankingCalcResult calc = calculateUserRanking(
                competitionId, userId, problemIds, problemScores, subsByProblem);

        // 更新或创建排名记录
        CompetitionRanking existing = rankingMapper.selectOne(
                new LambdaQueryWrapper<CompetitionRanking>()
                        .eq(CompetitionRanking::getCompetitionId, competitionId)
                        .eq(CompetitionRanking::getUserId, userId));

        if (existing != null) {
            existing.setTotalScore(calc.totalScore);
            existing.setSolvedCount(calc.solvedCount);
            existing.setPenalty(calc.penalty);
            existing.setProblemDetails(toJson(calc.problemDetails));
            rankingMapper.updateById(existing);
        } else {
            CompetitionRanking ranking = new CompetitionRanking();
            ranking.setCompetitionId(competitionId);
            ranking.setUserId(userId);
            ranking.setTotalScore(calc.totalScore);
            ranking.setSolvedCount(calc.solvedCount);
            ranking.setPenalty(calc.penalty);
            ranking.setProblemDetails(toJson(calc.problemDetails));
            rankingMapper.insert(ranking);
        }
    }

    /**
     * 全局重新排序排名（只排顺序，不重算分数）
     *
     * <p>优化: 使用单条 UPDATE + 子查询替代逐条更新，查询次数从 U+1 降为 2。
     */
    @Transactional
    public void reorderRankings(Long competitionId) {
        // 1. 查询并排序（内存操作）
        List<CompetitionRanking> rankings = rankingMapper.selectList(
                new LambdaQueryWrapper<CompetitionRanking>()
                        .eq(CompetitionRanking::getCompetitionId, competitionId)
                        .orderByDesc(CompetitionRanking::getTotalScore)
                        .orderByAsc(CompetitionRanking::getPenalty));

        if (rankings.isEmpty()) return;

        // 2. 构建 CASE WHEN 批量更新（单条 SQL）
        StringBuilder sql = new StringBuilder("UPDATE competition_ranking SET `rank` = CASE id ");
        List<Object> params = new ArrayList<>();
        int rank = 1;
        for (CompetitionRanking r : rankings) {
            sql.append("WHEN ? THEN ? ");
            params.add(r.getId());
            params.add(rank++);
        }
        sql.append("END WHERE competition_id = ? AND id IN (");
        sql.append(placeholders(rankings.size()));
        sql.append(")");
        params.add(competitionId);
        for (CompetitionRanking r : rankings) {
            params.add(r.getId());
        }

        jdbcTemplate.update(sql.toString(), params.toArray());
    }

    /**
     * 全量更新所有参赛者排名（批量版）
     */
    @Transactional
    public void updateAllRankings(Long competitionId, List<Long> problemIds, List<Integer> problemScores) {
        // 1. 查询所有参赛者（1 次查询）
        List<CompetitionRegistration> registrations = registrationMapper.selectList(
                new LambdaQueryWrapper<CompetitionRegistration>()
                        .eq(CompetitionRegistration::getCompetitionId, competitionId));

        if (registrations.isEmpty()) return;

        List<Long> userIds = registrations.stream()
                .map(CompetitionRegistration::getUserId)
                .toList();

        // 2. 一次查出该竞赛所有题目的全部提交记录
        List<Map<String, Object>> allSubmissions = jdbcTemplate.query(
                "SELECT user_id, problem_id, verdict, created_at " +
                        "FROM submission " +
                        "WHERE user_id IN (" + placeholders(userIds.size()) + ") " +
                        "  AND problem_id IN (" + placeholders(problemIds.size()) + ") " +
                        "  AND deleted = 0 " +
                        "ORDER BY user_id, problem_id, created_at ASC",
                (rs, rowNum) -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("userId", rs.getLong("user_id"));
                    row.put("problemId", rs.getLong("problem_id"));
                    row.put("verdict", rs.getString("verdict"));
                    row.put("createdAt", rs.getTimestamp("created_at").toLocalDateTime());
                    return row;
                },
                concatParams(userIds, problemIds));

        // 3. 按 userId → problemId 分组
        Map<Long, Map<Long, List<Map<String, Object>>>> userProblemSubmissions = new HashMap<>();
        for (Map<String, Object> sub : allSubmissions) {
            Long uid = (Long) sub.get("userId");
            Long pid = (Long) sub.get("problemId");
            userProblemSubmissions
                    .computeIfAbsent(uid, k -> new HashMap<>())
                    .computeIfAbsent(pid, k -> new ArrayList<>())
                    .add(sub);
        }

        // 4. 一次查出所有现有排名记录
        List<CompetitionRanking> existingRankings = rankingMapper.selectList(
                new LambdaQueryWrapper<CompetitionRanking>()
                        .eq(CompetitionRanking::getCompetitionId, competitionId));
        Map<Long, CompetitionRanking> rankingMap = existingRankings.stream()
                .collect(Collectors.toMap(CompetitionRanking::getUserId, r -> r));

        // 5. 批量计算每个用户的排名数据
        for (CompetitionRegistration reg : registrations) {
            Long userId = reg.getUserId();
            Map<Long, List<Map<String, Object>>> subsByProblem =
                    userProblemSubmissions.getOrDefault(userId, Collections.emptyMap());

            RankingCalcResult calc = calculateUserRanking(
                    competitionId, userId, problemIds, problemScores, subsByProblem);

            CompetitionRanking ranking = rankingMap.get(userId);
            if (ranking != null) {
                ranking.setTotalScore(calc.totalScore);
                ranking.setSolvedCount(calc.solvedCount);
                ranking.setPenalty(calc.penalty);
                ranking.setProblemDetails(toJson(calc.problemDetails));
                rankingMapper.updateById(ranking);
            } else {
                ranking = new CompetitionRanking();
                ranking.setCompetitionId(competitionId);
                ranking.setUserId(userId);
                ranking.setTotalScore(calc.totalScore);
                ranking.setSolvedCount(calc.solvedCount);
                ranking.setPenalty(calc.penalty);
                ranking.setProblemDetails(toJson(calc.problemDetails));
                rankingMapper.insert(ranking);
            }
        }

        // 6. 更新排名序号
        reorderRankings(competitionId);
    }

    // ==================== 工具方法 ====================

    private String placeholders(int count) {
        return String.join(",", Collections.nCopies(count, "?"));
    }

    private Object[] concatParams(List<Long> userIds, List<Long> problemIds) {
        Object[] params = new Object[userIds.size() + problemIds.size()];
        int i = 0;
        for (Long id : userIds) params[i++] = id;
        for (Long id : problemIds) params[i++] = id;
        return params;
    }

    /** 对象序列化为 JSON 字符串（运行时异常包装） */
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("JSON 序列化失败", e);
        }
    }
}
