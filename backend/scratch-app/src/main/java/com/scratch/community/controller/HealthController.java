package com.scratch.community.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scratch.community.common.result.R;
import com.scratch.community.module.editor.entity.Project;
import com.scratch.community.module.editor.mapper.ProjectMapper;
import com.scratch.community.module.user.entity.User;
import com.scratch.community.module.user.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查 & 平台统计
 */
@Tag(name = "健康检查", description = "服务健康检查和平台统计")
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final UserMapper userMapper;
    private final ProjectMapper projectMapper;

    @Operation(summary = "健康检查")
    @GetMapping("/api/v1/health")
    public R<Map<String, Object>> health() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("status", "UP");
        info.put("service", "scratch-community");
        info.put("version", "3.0.0");
        info.put("time", LocalDateTime.now().toString());
        info.put("java", System.getProperty("java.version"));
        info.put("os", System.getProperty("os.name") + " " + System.getProperty("os.arch"));

        // 内存使用情况
        Runtime rt = Runtime.getRuntime();
        Map<String, Object> memory = new LinkedHashMap<>();
        memory.put("maxMB", rt.maxMemory() / 1024 / 1024);
        memory.put("totalMB", rt.totalMemory() / 1024 / 1024);
        memory.put("freeMB", rt.freeMemory() / 1024 / 1024);
        memory.put("usedMB", (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024);
        info.put("memory", memory);

        return R.ok(info);
    }

    /**
     * 向后兼容：/api/health → /api/v1/health
     */
    @Operation(summary = "健康检查（向后兼容）")
    @GetMapping("/api/health")
    public R<Map<String, Object>> healthLegacy() {
        return health();
    }

    /**
     * 平台统计数据（公开接口，无需认证）
     * 用于首页展示用户数、项目数等
     */
    @Operation(summary = "平台统计数据")
    @GetMapping("/api/v1/stats")
    public R<Map<String, Object>> stats() {
        long totalUsers = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getDeleted, 0));
        long totalProjects = projectMapper.selectCount(
                new LambdaQueryWrapper<Project>().eq(Project::getDeleted, 0));
        long publishedProjects = projectMapper.selectCount(
                new LambdaQueryWrapper<Project>()
                        .eq(Project::getStatus, "published")
                        .eq(Project::getDeleted, 0));
        return R.ok(Map.of(
                "totalUsers", totalUsers,
                "totalProjects", totalProjects,
                "publishedProjects", publishedProjects
        ));
    }
}
