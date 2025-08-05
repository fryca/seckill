package yw.seckill.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import yw.seckill.service.LockMonitorService;

/**
 * 定时任务配置类
 */
@Slf4j
@Configuration
@EnableScheduling
public class ScheduleConfig {

    @Autowired
    private LockMonitorService lockMonitorService;

    /**
     * 每小时清理一次过期的锁统计信息
     */
    @Scheduled(fixedRate = 3600000) // 1小时 = 3600000毫秒
    public void cleanupExpiredLockStatistics() {
        try {
            log.info("开始清理过期的锁统计信息");
            lockMonitorService.cleanupExpiredStatistics();
            log.info("清理过期的锁统计信息完成");
        } catch (Exception e) {
            log.error("清理过期的锁统计信息失败", e);
        }
    }
} 