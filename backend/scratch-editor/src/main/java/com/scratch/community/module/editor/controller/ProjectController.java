package com.scratch.community.module.editor.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scratch.community.common.auth.LoginUser;
import com.scratch.community.common.exception.BizException;
import com.scratch.community.common.result.ErrorCode;
import com.scratch.community.common.result.R;
import com.scratch.community.common.util.FileConstants;
import com.scratch.community.common.idempotent.Idempotent;
import com.scratch.community.module.editor.dto.CreateProjectDTO;
import com.scratch.community.module.editor.dto.UpdateProjectDTO;
import com.scratch.community.module.editor.service.ProjectService;
import com.scratch.community.module.editor.vo.ProjectDetailVO;
import com.scratch.community.module.editor.vo.ProjectVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 项目模块 API
 */
@Tag(name = "项目", description = "Scratch 项目 CRUD/上传/发布")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProjectController {

    /** 最大文件大小 100MB（引用 FileConstants 统一常量） */
    private static final long MAX_SB3_SIZE = FileConstants.SB3_MAX_SIZE;

    private final ProjectService projectService;

    @Operation(summary = "创建项目")
    @Idempotent
    @PostMapping("/project")
    public R<ProjectVO> create(@Valid @RequestBody CreateProjectDTO dto) {
        return R.ok(projectService.create(LoginUser.getUserId(), dto));
    }

    @Operation(summary = "我的项目列表")
    @GetMapping("/project")
    public R<?> list(@RequestParam(defaultValue = "1") @Min(1) int page,
                     @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return R.ok(projectService.myProjects(LoginUser.getUserId(), new Page<>(page, size)));
    }

    @Operation(summary = "项目详情")
    @GetMapping("/project/{id}")
    public R<ProjectDetailVO> detail(@PathVariable Long id) {
        return R.ok(projectService.getDetail(LoginUser.getUserId(), id));
    }

    @Operation(summary = "更新项目信息")
    @PutMapping("/project/{id}")
    public R<Void> update(@PathVariable Long id,
                          @Valid @RequestBody UpdateProjectDTO dto) {
        projectService.update(LoginUser.getUserId(), id, dto);
        return R.ok();
    }

    @Operation(summary = "删除项目")
    @DeleteMapping("/project/{id}")
    public R<Void> delete(@PathVariable Long id) {
        projectService.delete(LoginUser.getUserId(), id);
        return R.ok();
    }

    @Operation(summary = "上传 sb3 文件（multipart）")
    @PostMapping("/project/{id}/sb3")
    public R<Void> uploadSb3(@PathVariable Long id,
                              @RequestParam("file") MultipartFile file) {
        // 文件校验在 Controller 层
        if (file.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "文件不能为空");
        }
        if (file.getSize() > MAX_SB3_SIZE) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "文件大小超过 100MB 限制");
        }
        String contentType = file.getContentType();
        if (contentType != null
                && !contentType.equals("application/zip")
                && !contentType.equals("application/octet-stream")
                && !contentType.equals("application/x-zip-compressed")) {
            throw new BizException(ErrorCode.SB3_FORMAT_ERROR);
        }

        projectService.uploadSb3(LoginUser.getUserId(), id, file);
        return R.ok();
    }

    @Operation(summary = "自动保存 sb3（base64）",
               description = "编辑器自动保存时调用，接受 base64 编码的 sb3 数据")
    @PostMapping("/project/{id}/sb3/auto-save")
    public R<Void> autoSaveSb3(@PathVariable Long id,
                                @RequestBody java.util.Map<String, String> body) {
        String base64Data = body.get("data");
        if (base64Data == null || base64Data.isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "缺少 sb3 数据");
        }
        // 去掉可能的 data:...;base64, 前缀
        if (base64Data.contains(",")) {
            base64Data = base64Data.substring(base64Data.indexOf(',') + 1);
        }
        projectService.autoSaveSb3(LoginUser.getUserId(), id, base64Data);
        return R.ok();
    }

    @Operation(summary = "下载 sb3 文件（presigned URL，需登录）")
    @GetMapping("/project/{id}/sb3")
    public R<String> downloadSb3(@PathVariable Long id) {
        return R.ok(projectService.getSb3Url(LoginUser.getUserId(), id));
    }

    @Operation(summary = "公开 sb3 下载（CORS 友好，供 TurboWarp 等外部编辑器加载）",
               description = "返回 sb3 文件流，带 CORS 头，无需登录。仅限已发布项目。")
    @GetMapping("/project/{id}/sb3/download")
    public void downloadSb3Public(@PathVariable Long id, HttpServletResponse response) {
        projectService.streamSb3(id, response);
    }

    @Operation(summary = "发布项目")
    @Idempotent
    @PostMapping("/project/{id}/publish")
    public R<Void> publish(@PathVariable Long id) {
        projectService.publish(LoginUser.getUserId(), id);
        return R.ok();
    }

    // ==================== Remix ====================

    @Operation(summary = "Remix 项目（二次创作）")
    @Idempotent
    @PostMapping("/project/{id}/remix")
    public R<ProjectVO> remix(@PathVariable Long id) {
        return R.ok(projectService.remix(LoginUser.getUserId(), id));
    }

    @Operation(summary = "获取项目的 Remix 列表")
    @GetMapping("/project/{id}/remixes")
    public R<?> remixes(@PathVariable Long id,
                        @RequestParam(defaultValue = "1") @Min(1) int page,
                        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return R.ok(projectService.getRemixes(id, new Page<>(page, size)));
    }

    @Operation(summary = "获取项目的 Remix 归属链")
    @GetMapping("/project/{id}/remix-chain")
    public R<?> remixChain(@PathVariable Long id) {
        return R.ok(projectService.getRemixChain(id));
    }
}
