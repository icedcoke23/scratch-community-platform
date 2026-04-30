package com.scratch.community.module.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scratch.community.common.auth.LoginUser;
import com.scratch.community.common.result.R;
import com.scratch.community.module.user.entity.PointLog;
import com.scratch.community.module.user.service.PointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 积分系统 API
 */
@Tag(name = "积分", description = "积分/等级/签到/排行榜")
@RestController
@RequestMapping("/api/v1/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    @Operation(summary = "获取我的积分信息")
    @GetMapping("/me")
    public R<Map<String, Object>> myPoints() {
        return R.ok(pointService.getUserPoints(LoginUser.getUserId()));
    }

    @Operation(summary = "每日签到")
    @PostMapping("/checkin")
    public R<Map<String, Object>> checkin() {
        int earned = pointService.dailyCheckin(LoginUser.getUserId());
        Map<String, Object> info = pointService.getUserPoints(LoginUser.getUserId());
        info.put("earned", earned);
        info.put("checkedIn", earned > 0);
        return R.ok(info);
    }

    @Operation(summary = "获取积分变动记录")
    @GetMapping("/logs")
    public R<Page<PointLog>> pointLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return R.ok(pointService.getPointLogs(LoginUser.getUserId(),
                new Page<>(page, size)));
    }

    @Operation(summary = "积分排行榜")
    @GetMapping("/ranking")
    public R<List<Map<String, Object>>> ranking(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int topN) {
        return R.ok(pointService.getPointRanking(topN));
    }

    @Operation(summary = "查看他人积分")
    @GetMapping("/user/{id}")
    public R<Map<String, Object>> userPoints(@PathVariable Long id) {
        return R.ok(pointService.getUserPoints(id));
    }
}
