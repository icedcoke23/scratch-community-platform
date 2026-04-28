package com.scratch.community.module.judge.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scratch.community.common.auth.LoginUser;
import com.scratch.community.common.auth.RequireRole;
import com.scratch.community.common.idempotent.Idempotent;
import com.scratch.community.common.result.R;
import com.scratch.community.module.judge.dto.CreateCompetitionDTO;
import com.scratch.community.module.judge.entity.Competition;
import com.scratch.community.module.judge.service.CompetitionService;
import com.scratch.community.module.judge.vo.CompetitionRankingVO;
import com.scratch.community.module.judge.vo.CompetitionVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 竞赛 API
 */
@Tag(name = "竞赛", description = "竞赛管理/报名/排名")
@RestController
@RequestMapping("/api/v1/competition")
@RequiredArgsConstructor
public class CompetitionController {

    private final CompetitionService competitionService;

    // ==================== 管理端 ====================

    @Operation(summary = "创建竞赛")
    @PostMapping
    @RequireRole({"TEACHER", "ADMIN"})
    public R<CompetitionVO> create(@Valid @RequestBody CreateCompetitionDTO dto) {
        return R.ok(competitionService.create(LoginUser.getUserId(), dto));
    }

    @Operation(summary = "发布竞赛")
    @PostMapping("/{id}/publish")
    @RequireRole({"TEACHER", "ADMIN"})
    public R<Void> publish(@PathVariable Long id) {
        competitionService.publish(id, LoginUser.getUserId());
        return R.ok();
    }

    @Operation(summary = "删除竞赛")
    @DeleteMapping("/{id}")
    @RequireRole({"TEACHER", "ADMIN"})
    public R<Void> delete(@PathVariable Long id) {
        competitionService.delete(id, LoginUser.getUserId());
        return R.ok();
    }

    // ==================== 用户端 ====================

    @Operation(summary = "竞赛列表")
    @GetMapping
    public R<?> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return R.ok(competitionService.listCompetitions(
                LoginUser.getUserId(), status, new Page<>(page, size)));
    }

    @Operation(summary = "竞赛详情")
    @GetMapping("/{id}")
    public R<CompetitionVO> detail(@PathVariable Long id) {
        return R.ok(competitionService.getDetail(id, LoginUser.getUserId()));
    }

    @Operation(summary = "报名竞赛")
    @Idempotent
    @PostMapping("/{id}/register")
    public R<Void> register(@PathVariable Long id) {
        competitionService.register(id, LoginUser.getUserId());
        return R.ok();
    }

    @Operation(summary = "竞赛排名")
    @GetMapping("/{id}/ranking")
    public R<?> ranking(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return R.ok(competitionService.getRanking(id, new Page<>(page, size)));
    }
}
