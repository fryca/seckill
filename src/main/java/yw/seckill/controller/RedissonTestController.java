package yw.seckill.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import yw.seckill.config.ApiResponse;
import yw.seckill.service.RedissonExampleService;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Redisson测试控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/redisson")
public class RedissonTestController {

    @Autowired
    private RedissonExampleService redissonExampleService;

    private final AtomicInteger counter = new AtomicInteger(0);

    /**
     * 测试分布式锁
     */
    @PostMapping("/test-lock")
    public ApiResponse<Map<String, Object>> testLock(@RequestParam Long productId, 
                                                    @RequestParam(defaultValue = "1") int quantity) {
        try {
            boolean result = redissonExampleService.deductStockWithLock(productId, quantity);
            Map<String, Object> response = new HashMap<>();
            response.put("success", result);
            response.put("productId", productId);
            response.put("quantity", quantity);
            response.put("message", "分布式锁测试完成");
            
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("分布式锁测试失败", e);
            return ApiResponse.error("分布式锁测试失败: " + e.getMessage());
        }
    }

    /**
     * 测试限流器
     */
    @PostMapping("/test-rate-limit")
    public ApiResponse<Map<String, Object>> testRateLimit(@RequestParam Long activityId, 
                                                        @RequestParam Long userId) {
        try {
            boolean result = redissonExampleService.seckillWithRateLimit(activityId, userId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", result);
            response.put("activityId", activityId);
            response.put("userId", userId);
            response.put("message", "限流器测试完成");
            
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("限流器测试失败", e);
            return ApiResponse.error("限流器测试失败: " + e.getMessage());
        }
    }

    /**
     * 并发测试分布式锁
     */
    @PostMapping("/test-concurrent-lock")
    public ApiResponse<Map<String, Object>> testConcurrentLock(@RequestParam Long productId) {
        Map<String, Object> response = new HashMap<>();
        response.put("productId", productId);
        response.put("message", "并发测试分布式锁");
        
        // 模拟并发请求
        for (int i = 0; i < 5; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    boolean result = redissonExampleService.deductStockWithLock(productId, 1);
                    log.info("线程 {} 执行结果: {}", threadId, result);
                } catch (Exception e) {
                    log.error("线程 {} 执行失败", threadId, e);
                }
            }).start();
        }
        
        return ApiResponse.success(response);
    }

    /**
     * 测试限流器并发
     */
    @PostMapping("/test-concurrent-rate-limit")
    public ApiResponse<Map<String, Object>> testConcurrentRateLimit(@RequestParam Long activityId) {
        Map<String, Object> response = new HashMap<>();
        response.put("activityId", activityId);
        response.put("message", "并发测试限流器");
        
        // 模拟并发请求
        for (int i = 0; i < 10; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    Long userId = (long) counter.incrementAndGet();
                    boolean result = redissonExampleService.seckillWithRateLimit(activityId, userId);
                    log.info("线程 {} 用户 {} 执行结果: {}", threadId, userId, result);
                } catch (Exception e) {
                    log.error("线程 {} 执行失败", threadId, e);
                }
            }).start();
        }
        
        return ApiResponse.success(response);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("Redisson配置正常");
    }
} 