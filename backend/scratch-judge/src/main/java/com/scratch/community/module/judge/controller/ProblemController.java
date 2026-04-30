package com.scratch.community.module.judge.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scratch.community.common.auth.LoginUser;
import com.scratch.community.common.auth.RequireRole;
import com.scratch.community.common.result.R;
import com.scratch.community.module.judge.dto.CreateProblemDTO;
import com.scratch.community.module.judge.service.ProblemService;
import com.scratch.community.module.judge.vo.ProblemDetailVO;
import com.scratch.community.module.judge.vo.ProblemVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 题目管理 API
 */
@Tag(name = "题目", description = "题目 CRUD/发布")
@RestController
@RequestMapping("/api/v1/problem")
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemService problemService;

    /**
     * 创建题目（教师/管理员）
     */
    @Operation(summary = "创建题目")
    @PostMapping
    @RequireRole({"TEACHER", "ADMIN"})
    public R<ProblemVO> create(@Valid @RequestBody CreateProblemDTO dto) {
        return R.ok(problemService.create(LoginUser.getUserId(), dto));
    }

    /**
     * 题目列表（分页，限制单页最大 100）
     */
    @Operation(summary = "题目列表")
    @GetMapping
    public R<?> list(@RequestParam(required = false) String type,
                     @RequestParam(required = false) String difficulty,
                     @RequestParam(defaultValue = "1") @Min(1) int page,
                     @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return R.ok(problemService.listProblems(type, difficulty,
                new Page<>(page, size)));
    }

    /**
     * 题目详情
     */
    @Operation(summary = "题目详情")
    @GetMapping("/{id}")
    public R<ProblemDetailVO> detail(@PathVariable Long id) {
        return R.ok(problemService.getDetail(id));
    }

    /**
     * 更新题目
     */
    @Operation(summary = "更新题目")
    @PutMapping("/{id}")
    @RequireRole({"TEACHER", "ADMIN"})
    public R<Void> update(@PathVariable Long id,
                          @Valid @RequestBody CreateProblemDTO dto) {
        problemService.update(LoginUser.getUserId(), id, dto);
        return R.ok();
    }

    /**
     * 发布题目
     */
    @Operation(summary = "发布题目")
    @PostMapping("/{id}/publish")
    @RequireRole({"TEACHER", "ADMIN"})
    public R<Void> publish(@PathVariable Long id) {
        problemService.publish(LoginUser.getUserId(), id);
        return R.ok();
    }

    /**
     * 删除题目
     */
    @Operation(summary = "删除题目")
    @DeleteMapping("/{id}")
    @RequireRole({"TEACHER", "ADMIN"})
    public R<Void> delete(@PathVariable Long id) {
        problemService.delete(LoginUser.getUserId(), id);
        return R.ok();
    }
}
