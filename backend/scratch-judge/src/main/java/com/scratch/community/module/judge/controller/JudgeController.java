package com.scratch.community.module.judge.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scratch.community.common.auth.LoginUser;
import com.scratch.community.common.idempotent.Idempotent;
import com.scratch.community.common.result.R;
import com.scratch.community.module.judge.dto.SubmitDTO;
import com.scratch.community.module.judge.entity.Submission;
import com.scratch.community.module.judge.service.JudgeService;
import com.scratch.community.module.judge.vo.SubmissionVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 判题 API
 */
@Tag(name = "判题", description = "提交答案/查看结果")
@RestController
@RequestMapping("/api/v1/judge")
@RequiredArgsConstructor
public class JudgeController {

    private final JudgeService judgeService;

    /**
     * 提交答案
     */
    @Operation(summary = "提交答案")
    @Idempotent
    @PostMapping("/submit")
    public R<SubmissionVO> submit(@Valid @RequestBody SubmitDTO dto) {
        return R.ok(judgeService.submit(LoginUser.getUserId(), dto));
    }

    /**
     * 我的提交记录（分页）
     */
    @Operation(summary = "我的提交记录")
    @GetMapping("/submissions")
    public R<?> mySubmissions(@RequestParam(required = false) Long problemId,
                              @RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return R.ok(judgeService.mySubmissions(LoginUser.getUserId(), problemId,
                new Page<>(page, size)));
    }

    /**
     * 查询判题结果
     */
    @Operation(summary = "查询判题结果")
    @GetMapping("/result/{id}")
    public R<SubmissionVO> result(@PathVariable Long id) {
        return R.ok(judgeService.getResult(LoginUser.getUserId(), id));
    }
}
