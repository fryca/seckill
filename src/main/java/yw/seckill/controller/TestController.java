package yw.seckill.controller;

import org.springframework.web.bind.annotation.*;
import yw.seckill.config.ApiResponse;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController {
    
    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("秒杀系统运行正常");
    }
    
    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> getSystemInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("service", "秒杀系统");
        info.put("version", "1.0.0");
        info.put("timestamp", LocalDateTime.now());
        info.put("status", "running");
        return ApiResponse.success(info);
    }
    
    @PostMapping("/echo")
    public ApiResponse<Map<String, Object>> echo(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "收到请求");
        response.put("data", request);
        response.put("timestamp", LocalDateTime.now());
        return ApiResponse.success(response);
    }
} 