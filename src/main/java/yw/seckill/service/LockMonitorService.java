package yw.seckill.service;

import yw.seckill.entity.LockStatistics;

import java.util.List;
import java.util.Map;

/**
 * 锁监控服务接口
 */
public interface LockMonitorService {
    
    /**
     * 记录锁获取尝试
     *
     * @param lockKey 锁的key
     * @param threadId 线程ID
     * @return 锁统计对象
     */
    LockStatistics recordLockAttempt(String lockKey, String threadId);
    
    /**
     * 记录锁获取成功
     *
     * @param lockKey 锁的key
     * @param threadId 线程ID
     * @param waitDuration 等待时长
     */
    void recordLockAcquired(String lockKey, String threadId, Long waitDuration);
    
    /**
     * 记录锁释放
     *
     * @param lockKey 锁的key
     * @param threadId 线程ID
     * @param holdDuration 持有时长
     */
    void recordLockReleased(String lockKey, String threadId, Long holdDuration);
    
    /**
     * 记录锁续期
     *
     * @param lockKey 锁的key
     * @param threadId 线程ID
     * @param renewalDuration 续期时长
     */
    void recordLockRenewal(String lockKey, String threadId, Long renewalDuration);
    
    /**
     * 记录锁获取失败
     *
     * @param lockKey 锁的key
     * @param threadId 线程ID
     * @param waitDuration 等待时长
     */
    void recordLockFailed(String lockKey, String threadId, Long waitDuration);
    
    /**
     * 获取锁统计信息
     *
     * @param lockKey 锁的key
     * @return 锁统计信息
     */
    List<LockStatistics> getLockStatistics(String lockKey);
    
    /**
     * 获取所有锁的统计信息
     *
     * @return 所有锁的统计信息
     */
    Map<String, List<LockStatistics>> getAllLockStatistics();
    
    /**
     * 获取锁竞争统计
     *
     * @param lockKey 锁的key
     * @return 竞争统计信息
     */
    Map<String, Object> getLockCompetitionStats(String lockKey);
    
    /**
     * 清理过期的统计信息
     */
    void cleanupExpiredStatistics();
} 