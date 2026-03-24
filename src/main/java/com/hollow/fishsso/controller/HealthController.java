package com.hollow.fishsso.controller;

import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查控制器
 */
@RestController
public class HealthController {

    /**
     * 健康检查接口
     * @return 包含状态和时间的健康信息
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "time", Instant.now().toString()
        );
    }
}