package com.scratch.community.module.classroom.controller;

import com.scratch.community.common.auth.LoginUser;
import com.scratch.community.common.auth.RequireRole;
import com.scratch.community.common.result.R;
import com.scratch.community.module.classroom.service.AnalyticsService;
import com.scratch.community.module.classroom.vo.ClassAnalyticsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 学情分析 API
 */
@Tag(name = "学情分析", description = "班级学情报告/学生详情")
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Operation(summary = "班级学情报告（教师）")
    @GetMapping("/class/{classId}")
    @RequireRole({"TEACHER", "ADMIN"})
    public R<ClassAnalyticsVO> classAnalytics(@PathVariable Long classId) {
        return R.ok(analyticsService.getClassAnalytics(classId, LoginUser.getUserId()));
    }

    @Operation(summary = "学生个人学情详情（教师查看）")
    @GetMapping("/class/{classId}/student/{studentId}")
    @RequireRole({"TEACHER", "ADMIN"})
    public R<Map<String, Object>> studentAnalytics(
            @PathVariable Long classId,
            @PathVariable Long studentId) {
        return R.ok(analyticsService.getStudentAnalytics(studentId, classId));
    }

    @Operation(summary = "我的学情详情（学生查看自己）")
    @GetMapping("/class/{classId}/me")
    public R<Map<String, Object>> myAnalytics(@PathVariable Long classId) {
        return R.ok(analyticsService.getStudentAnalytics(LoginUser.getUserId(), classId));
    }
}
