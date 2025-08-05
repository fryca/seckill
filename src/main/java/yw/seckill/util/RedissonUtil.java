package yw.seckill.util;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RedissonClient;
import org.redisson.api.RateType;
import org.redisson.api.RateIntervalUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redisson工具类
 */
@Slf4j
@Component
public class RedissonUtil {

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 获取分布式锁
     *
     * @param lockKey 锁的key
     * @return 分布式锁对象
     */
    public RLock getLock(String lockKey) {
        return redissonClient.getLock(lockKey);
    }

    /**
     * 尝试获取分布式锁
     *
     * @param lockKey    锁的key
     * @param waitTime   等待时间
     * @param leaseTime  持有锁的时间
     * @param timeUnit   时间单位
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit) {
        RLock lock = getLock(lockKey);
        try {
            return lock.tryLock(waitTime, leaseTime, timeUnit);
        } catch (InterruptedException e) {
            log.error("获取分布式锁失败: {}", lockKey, e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 释放分布式锁
     *
     * @param lockKey 锁的key
     */
    public void unlock(String lockKey) {
        RLock lock = getLock(lockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * 获取限流器
     *
     * @param rateLimiterKey 限流器key
     * @return 限流器对象
     */
    public RRateLimiter getRateLimiter(String rateLimiterKey) {
        return redissonClient.getRateLimiter(rateLimiterKey);
    }

    /**
     * 设置限流器速率
     *
     * @param rateLimiterKey 限流器key
     * @param rate           速率
     * @param rateInterval   时间间隔
     * @param timeUnit       时间单位
     */
    public void setRateLimiter(String rateLimiterKey, long rate, long rateInterval, TimeUnit timeUnit) {
        RRateLimiter rateLimiter = getRateLimiter(rateLimiterKey);
        RateIntervalUnit intervalUnit = convertTimeUnit(timeUnit);
        rateLimiter.trySetRate(RateType.OVERALL, rate, rateInterval, intervalUnit);
    }

    /**
     * 转换TimeUnit到RateIntervalUnit
     */
    private RateIntervalUnit convertTimeUnit(TimeUnit timeUnit) {
        switch (timeUnit) {
            case SECONDS:
                return RateIntervalUnit.SECONDS;
            case MINUTES:
                return RateIntervalUnit.MINUTES;
            case HOURS:
                return RateIntervalUnit.HOURS;
            case DAYS:
                return RateIntervalUnit.DAYS;
            default:
                return RateIntervalUnit.SECONDS;
        }
    }

    /**
     * 尝试获取令牌
     *
     * @param rateLimiterKey 限流器key
     * @return 是否获取成功
     */
    public boolean tryAcquire(String rateLimiterKey) {
        RRateLimiter rateLimiter = getRateLimiter(rateLimiterKey);
        return rateLimiter.tryAcquire();
    }

    /**
     * 尝试获取指定数量的令牌
     *
     * @param rateLimiterKey 限流器key
     * @param permits        令牌数量
     * @return 是否获取成功
     */
    public boolean tryAcquire(String rateLimiterKey, long permits) {
        RRateLimiter rateLimiter = getRateLimiter(rateLimiterKey);
        return rateLimiter.tryAcquire(permits);
    }

    /**
     * 获取Redisson客户端
     *
     * @return Redisson客户端
     */
    public RedissonClient getRedissonClient() {
        return redissonClient;
    }
} 