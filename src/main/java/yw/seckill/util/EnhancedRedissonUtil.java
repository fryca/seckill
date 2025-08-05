package yw.seckill.util;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import yw.seckill.service.LockMonitorService;

import java.util.concurrent.TimeUnit;

/**
 * 增强的Redisson工具类，支持锁监控和看门狗功能
 */
@Slf4j
@Component
public class EnhancedRedissonUtil {

    @Autowired
    private RedissonClient redissonClient;
    
    @Autowired
    private LockMonitorService lockMonitorService;

    /**
     * 获取分布式锁（带监控）
     *
     * @param lockKey 锁的key
     * @return 分布式锁对象
     */
    public RLock getLock(String lockKey) {
        return redissonClient.getLock(lockKey);
    }

    /**
     * 尝试获取分布式锁（带监控和看门狗）
     *
     * @param lockKey    锁的key
     * @param waitTime   等待时间
     * @param leaseTime  持有锁的时间（看门狗会自动续期）
     * @param timeUnit   时间单位
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit) {
        String threadId = Thread.currentThread().getName() + "-" + Thread.currentThread().getId();
        long startTime = System.currentTimeMillis();
        
        // 记录锁获取尝试
        lockMonitorService.recordLockAttempt(lockKey, threadId);
        
        RLock lock = getLock(lockKey);
        try {
            boolean acquired = lock.tryLock(waitTime, leaseTime, timeUnit);
            
            long waitDuration = System.currentTimeMillis() - startTime;
            
            if (acquired) {
                // 记录锁获取成功
                lockMonitorService.recordLockAcquired(lockKey, threadId, waitDuration);
                log.info("成功获取锁: lockKey={}, threadId={}, waitDuration={}ms", 
                        lockKey, threadId, waitDuration);
            } else {
                // 记录锁获取失败
                lockMonitorService.recordLockFailed(lockKey, threadId, waitDuration);
                log.warn("获取锁失败: lockKey={}, threadId={}, waitDuration={}ms", 
                        lockKey, threadId, waitDuration);
            }
            
            return acquired;
        } catch (InterruptedException e) {
            long waitDuration = System.currentTimeMillis() - startTime;
            lockMonitorService.recordLockFailed(lockKey, threadId, waitDuration);
            log.error("获取分布式锁被中断: lockKey={}, threadId={}", lockKey, threadId, e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 释放分布式锁（带监控）
     *
     * @param lockKey 锁的key
     */
    public void unlock(String lockKey) {
        String threadId = Thread.currentThread().getName() + "-" + Thread.currentThread().getId();
        RLock lock = getLock(lockKey);
        
        if (lock.isHeldByCurrentThread()) {
            long holdStartTime = System.currentTimeMillis();
            try {
                lock.unlock();
                long holdDuration = System.currentTimeMillis() - holdStartTime;
                lockMonitorService.recordLockReleased(lockKey, threadId, holdDuration);
                log.info("成功释放锁: lockKey={}, threadId={}, holdDuration={}ms", 
                        lockKey, threadId, holdDuration);
            } catch (Exception e) {
                log.error("释放锁失败: lockKey={}, threadId={}", lockKey, threadId, e);
            }
        } else {
            log.warn("尝试释放不属于当前线程的锁: lockKey={}, threadId={}", lockKey, threadId);
        }
    }

    /**
     * 使用看门狗机制获取锁（自动续期）
     *
     * @param lockKey    锁的key
     * @param waitTime   等待时间
     * @param timeUnit   时间单位
     * @return 是否获取成功
     */
    public boolean tryLockWithWatchdog(String lockKey, long waitTime, TimeUnit timeUnit) {
        // 使用Redisson的看门狗机制，leaseTime设为-1表示自动续期
        return tryLock(lockKey, waitTime, -1, timeUnit);
    }

    /**
     * 获取锁信息
     *
     * @param lockKey 锁的key
     * @return 锁信息
     */
    public String getLockInfo(String lockKey) {
        RLock lock = getLock(lockKey);
        return String.format("锁信息: key=%s, isLocked=%s, isHeldByCurrentThread=%s", 
                lockKey, lock.isLocked(), lock.isHeldByCurrentThread());
    }

    /**
     * 强制释放锁（谨慎使用）
     *
     * @param lockKey 锁的key
     */
    public void forceUnlock(String lockKey) {
        RLock lock = getLock(lockKey);
        try {
            lock.forceUnlock();
            log.warn("强制释放锁: lockKey={}", lockKey);
        } catch (Exception e) {
            log.error("强制释放锁失败: lockKey={}", lockKey, e);
        }
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