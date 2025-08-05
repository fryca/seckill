package yw.seckill.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import yw.seckill.config.ApiResponse;
import yw.seckill.entity.LockStatistics;
import yw.seckill.service.LockMonitorService;
import yw.seckill.util.EnhancedRedissonUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 锁监控控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/lock-monitor")
public class LockMonitorController {

    @Autowired
    private LockMonitorService lockMonitorService;
    
    @Autowired
    private EnhancedRedissonUtil enhancedRedissonUtil;

    /**
     * 获取指定锁的统计信息
     */
    @GetMapping("/statistics/{lockKey}")
    public ApiResponse<List<LockStatistics>> getLockStatistics(@PathVariable String lockKey) {
        try {
            List<LockStatistics> statistics = lockMonitorService.getLockStatistics(lockKey);
            return ApiResponse.success(statistics);
        } catch (Exception e) {
            log.error("获取锁统计信息失败: lockKey={}", lockKey, e);
            return ApiResponse.error("获取锁统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有锁的统计信息
     */
    @GetMapping("/statistics")
    public ApiResponse<Map<String, List<LockStatistics>>> getAllLockStatistics() {
        try {
            Map<String, List<LockStatistics>> allStatistics = lockMonitorService.getAllLockStatistics();
            return ApiResponse.success(allStatistics);
        } catch (Exception e) {
            log.error("获取所有锁统计信息失败", e);
            return ApiResponse.error("获取所有锁统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取锁竞争统计
     */
    @GetMapping("/competition/{lockKey}")
    public ApiResponse<Map<String, Object>> getLockCompetitionStats(@PathVariable String lockKey) {
        try {
            Map<String, Object> stats = lockMonitorService.getLockCompetitionStats(lockKey);
            return ApiResponse.success(stats);
        } catch (Exception e) {
            log.error("获取锁竞争统计失败: lockKey={}", lockKey, e);
            return ApiResponse.error("获取锁竞争统计失败: " + e.getMessage());
        }
    }

    /**
     * 测试带监控的分布式锁
     */
    @PostMapping("/test-lock")
    public ApiResponse<Map<String, Object>> testLock(@RequestParam String lockKey,
                                                    @RequestParam(defaultValue = "5") long waitTime,
                                                    @RequestParam(defaultValue = "10") long leaseTime) {
        try {
            long startTime = System.currentTimeMillis();
            boolean acquired = enhancedRedissonUtil.tryLock(lockKey, waitTime, leaseTime, TimeUnit.SECONDS);
            long totalTime = System.currentTimeMillis() - startTime;
            
            Map<String, Object> response = new HashMap<>();
            response.put("lockKey", lockKey);
            response.put("acquired", acquired);
            response.put("totalTime", totalTime);
            response.put("waitTime", waitTime);
            response.put("leaseTime", leaseTime);
            
            if (acquired) {
                // 模拟业务处理
                Thread.sleep(2000);
                enhancedRedissonUtil.unlock(lockKey);
                response.put("message", "锁测试完成，已释放锁");
            } else {
                response.put("message", "获取锁失败");
            }
            
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("锁测试失败: lockKey={}", lockKey, e);
            return ApiResponse.error("锁测试失败: " + e.getMessage());
        }
    }

    /**
     * 测试看门狗机制
     */
    @PostMapping("/test-watchdog")
    public ApiResponse<Map<String, Object>> testWatchdog(@RequestParam String lockKey,
                                                        @RequestParam(defaultValue = "5") long waitTime) {
        try {
            long startTime = System.currentTimeMillis();
            boolean acquired = enhancedRedissonUtil.tryLockWithWatchdog(lockKey, waitTime, TimeUnit.SECONDS);
            long totalTime = System.currentTimeMillis() - startTime;
            
            Map<String, Object> response = new HashMap<>();
            response.put("lockKey", lockKey);
            response.put("acquired", acquired);
            response.put("totalTime", totalTime);
            response.put("waitTime", waitTime);
            response.put("watchdog", true);
            
            if (acquired) {
                // 模拟长时间业务处理，看门狗会自动续期
                Thread.sleep(15000); // 15秒，超过默认的锁超时时间
                enhancedRedissonUtil.unlock(lockKey);
                response.put("message", "看门狗测试完成，锁已自动续期并释放");
            } else {
                response.put("message", "获取锁失败");
            }
            
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("看门狗测试失败: lockKey={}", lockKey, e);
            return ApiResponse.error("看门狗测试失败: " + e.getMessage());
        }
    }

    /**
     * 并发测试锁监控
     */
    @PostMapping("/test-concurrent")
    public ApiResponse<Map<String, Object>> testConcurrent(@RequestParam String lockKey,
                                                          @RequestParam(defaultValue = "5") int threadCount) {
        Map<String, Object> response = new HashMap<>();
        response.put("lockKey", lockKey);
        response.put("threadCount", threadCount);
        response.put("message", "开始并发测试");
        
        // 模拟并发请求
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    boolean acquired = enhancedRedissonUtil.tryLock(lockKey, 3, 5, TimeUnit.SECONDS);
                    if (acquired) {
                        log.info("线程 {} 成功获取锁", threadId);
                        Thread.sleep(1000); // 模拟业务处理
                        enhancedRedissonUtil.unlock(lockKey);
                        log.info("线程 {} 释放锁", threadId);
                    } else {
                        log.warn("线程 {} 获取锁失败", threadId);
                    }
                } catch (Exception e) {
                    log.error("线程 {} 执行失败", threadId, e);
                }
            }).start();
        }
        
        return ApiResponse.success(response);
    }

    /**
     * 获取锁信息
     */
    @GetMapping("/lock-info/{lockKey}")
    public ApiResponse<String> getLockInfo(@PathVariable String lockKey) {
        try {
            String lockInfo = enhancedRedissonUtil.getLockInfo(lockKey);
            return ApiResponse.success(lockInfo);
        } catch (Exception e) {
            log.error("获取锁信息失败: lockKey={}", lockKey, e);
            return ApiResponse.error("获取锁信息失败: " + e.getMessage());
        }
    }

    /**
     * 强制释放锁
     */
    @PostMapping("/force-unlock/{lockKey}")
    public ApiResponse<String> forceUnlock(@PathVariable String lockKey) {
        try {
            enhancedRedissonUtil.forceUnlock(lockKey);
            return ApiResponse.success("强制释放锁成功: " + lockKey);
        } catch (Exception e) {
            log.error("强制释放锁失败: lockKey={}", lockKey, e);
            return ApiResponse.error("强制释放锁失败: " + e.getMessage());
        }
    }

    /**
     * 清理过期统计信息
     */
    @PostMapping("/cleanup")
    public ApiResponse<String> cleanupExpiredStatistics() {
        try {
            lockMonitorService.cleanupExpiredStatistics();
            return ApiResponse.success("清理过期统计信息完成");
        } catch (Exception e) {
            log.error("清理过期统计信息失败", e);
            return ApiResponse.error("清理过期统计信息失败: " + e.getMessage());
        }
    }
} 