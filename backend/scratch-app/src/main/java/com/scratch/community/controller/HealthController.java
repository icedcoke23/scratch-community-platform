package com.scratch.community.controller;

import com.scratch.community.common.result.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 健康检查
 */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public R<Map<String, Object>> health() {
        return R.ok(Map.of(
                "status", "UP",
                "service", "scratch-community",
                "version", "0.1.0",
                "time", LocalDateTime.now().toString()
        ));
    }
}
