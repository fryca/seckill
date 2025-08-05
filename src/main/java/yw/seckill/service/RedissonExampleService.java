package yw.seckill.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import yw.seckill.util.RedissonUtil;

import java.util.concurrent.TimeUnit;

/**
 * Redisson使用示例服务
 */
@Slf4j
@Service
public class RedissonExampleService {

    @Autowired
    private RedissonUtil redissonUtil;

    /**
     * 使用分布式锁保护临界区
     *
     * @param lockKey 锁的key
     * @param businessLogic 业务逻辑
     * @return 执行结果
     */
    public <T> T executeWithLock(String lockKey, BusinessLogic<T> businessLogic) {
        boolean locked = false;
        try {
            // 尝试获取锁，等待5秒，持有锁30秒
            locked = redissonUtil.tryLock(lockKey, 5, 30, TimeUnit.SECONDS);
            if (locked) {
                log.info("成功获取分布式锁: {}", lockKey);
                return businessLogic.execute();
            } else {
                log.warn("获取分布式锁失败: {}", lockKey);
                throw new RuntimeException("获取锁失败");
            }
        } finally {
            if (locked) {
                redissonUtil.unlock(lockKey);
                log.info("释放分布式锁: {}", lockKey);
            }
        }
    }

    /**
     * 使用限流器控制访问频率
     *
     * @param rateLimiterKey 限流器key
     * @param rate 速率
     * @param rateInterval 时间间隔
     * @param timeUnit 时间单位
     * @param businessLogic 业务逻辑
     * @return 执行结果
     */
    public <T> T executeWithRateLimit(String rateLimiterKey, long rate, long rateInterval, 
                                     TimeUnit timeUnit, BusinessLogic<T> businessLogic) {
        // 设置限流器
        redissonUtil.setRateLimiter(rateLimiterKey, rate, rateInterval, timeUnit);
        
        // 尝试获取令牌
        if (redissonUtil.tryAcquire(rateLimiterKey)) {
            log.info("成功获取令牌，执行业务逻辑: {}", rateLimiterKey);
            return businessLogic.execute();
        } else {
            log.warn("限流器拒绝请求: {}", rateLimiterKey);
            throw new RuntimeException("请求过于频繁，请稍后重试");
        }
    }

    /**
     * 业务逻辑接口
     */
    @FunctionalInterface
    public interface BusinessLogic<T> {
        T execute();
    }

    /**
     * 示例：使用分布式锁保护库存扣减
     */
    public boolean deductStockWithLock(Long productId, int quantity) {
        String lockKey = "stock_lock:" + productId;
        
        return executeWithLock(lockKey, () -> {
            // 这里应该是实际的库存扣减逻辑
            log.info("执行库存扣减，商品ID: {}, 数量: {}", productId, quantity);
            // 模拟业务处理
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("库存扣减线程中断", e);
            }
            return true;
        });
    }

    /**
     * 示例：使用限流器控制秒杀接口访问
     */
    public boolean seckillWithRateLimit(Long activityId, Long userId) {
        String rateLimiterKey = "seckill_rate_limit:" + activityId;
        
        return executeWithRateLimit(rateLimiterKey, 100, 1, TimeUnit.SECONDS, () -> {
            // 这里应该是实际的秒杀逻辑
            log.info("执行秒杀逻辑，活动ID: {}, 用户ID: {}", activityId, userId);
            // 模拟业务处理
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("秒杀线程中断", e);
            }
            return true;
        });
    }
} 