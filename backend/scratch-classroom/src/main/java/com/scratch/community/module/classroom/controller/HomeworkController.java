package com.scratch.community.module.classroom.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scratch.community.common.auth.LoginUser;
import com.scratch.community.common.auth.RequireRole;
import com.scratch.community.common.result.R;
import com.scratch.community.module.classroom.dto.CreateHomeworkDTO;
import com.scratch.community.module.classroom.dto.GradeHomeworkDTO;
import com.scratch.community.module.classroom.dto.SubmitHomeworkDTO;
import com.scratch.community.module.classroom.entity.Homework;
import com.scratch.community.module.classroom.service.HomeworkGradingService;
import com.scratch.community.module.classroom.service.HomeworkService;
import com.scratch.community.module.classroom.vo.HomeworkSubmissionVO;
import com.scratch.community.module.classroom.vo.HomeworkVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 作业 API
 */
@Tag(name = "作业", description = "作业布置/提交/批改")
@RestController
@RequestMapping("/api/v1/homework")
@RequiredArgsConstructor
public class HomeworkController {

    private final HomeworkService homeworkService;
    private final HomeworkGradingService homeworkGradingService;

    // ==================== 教师端 ====================

    /**
     * 创建作业
     */
    @Operation(summary = "创建作业")
    @PostMapping
    @RequireRole({"TEACHER", "ADMIN"})
    public R<HomeworkVO> create(@Valid @RequestBody CreateHomeworkDTO dto) {
        return R.ok(homeworkService.create(LoginUser.getUserId(), dto));
    }

    /**
     * 发布作业
     */
    @Operation(summary = "发布作业")
    @PostMapping("/{id}/publish")
    @RequireRole({"TEACHER", "ADMIN"})
    public R<Void> publish(@PathVariable Long id) {
        homeworkService.publish(LoginUser.getUserId(), id);
        return R.ok();
    }

    /**
     * 关闭作业
     */
    @Operation(summary = "关闭作业")
    @PostMapping("/{id}/close")
    @RequireRole({"TEACHER", "ADMIN"})
    public R<Void> close(@PathVariable Long id) {
        homeworkService.close(LoginUser.getUserId(), id);
        return R.ok();
    }

    /**
     * 查看作业提交列表
     */
    @Operation(summary = "查看作业提交列表")
    @GetMapping("/{id}/submissions")
    @RequireRole({"TEACHER", "ADMIN"})
    public R<?> submissions(@PathVariable Long id,
                            @RequestParam(defaultValue = "1") @Min(1) int page,
                            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return R.ok(homeworkService.getSubmissions(LoginUser.getUserId(), id,
                new Page<>(page, size)));
    }

    /**
     * 批改作业
     */
    @Operation(summary = "批改作业")
    @PostMapping("/grade")
    @RequireRole({"TEACHER", "ADMIN"})
    public R<Void> grade(@Valid @RequestBody GradeHomeworkDTO dto) {
        homeworkGradingService.grade(LoginUser.getUserId(), dto);
        return R.ok();
    }

    // ==================== 学生端 ====================

    /**
     * 提交作业
     */
    @Operation(summary = "提交作业")
    @PostMapping("/submit")
    public R<HomeworkSubmissionVO> submit(@Valid @RequestBody SubmitHomeworkDTO dto) {
        return R.ok(homeworkService.submit(LoginUser.getUserId(), dto));
    }

    /**
     * 我的提交记录
     */
    @Operation(summary = "我的提交记录")
    @GetMapping("/my")
    public R<?> mySubmissions(@RequestParam Long classId,
                              @RequestParam(defaultValue = "1") @Min(1) int page,
                              @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return R.ok(homeworkService.mySubmissions(LoginUser.getUserId(), classId,
                new Page<>(page, size)));
    }

    // ==================== 通用 ====================

    /**
     * 班级作业列表
     */
    @Operation(summary = "班级作业列表")
    @GetMapping("/class/{classId}")
    public R<?> listByClass(@PathVariable Long classId,
                            @RequestParam(defaultValue = "1") @Min(1) int page,
                            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return R.ok(homeworkService.listByClass(classId, new Page<>(page, size)));
    }

    /**
     * 作业详情
     */
    @Operation(summary = "作业详情")
    @GetMapping("/{id}")
    public R<HomeworkVO> detail(@PathVariable Long id) {
        return R.ok(homeworkService.getDetail(id));
    }
}
