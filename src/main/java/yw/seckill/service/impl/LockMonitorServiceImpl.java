package yw.seckill.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import yw.seckill.entity.LockStatistics;
import yw.seckill.service.LockMonitorService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 锁监控服务实现类
 */
@Slf4j
@Service
public class LockMonitorServiceImpl implements LockMonitorService {
    
    // 使用内存存储锁统计信息，实际项目中可以存储到Redis或数据库
    private final Map<String, List<LockStatistics>> lockStatisticsMap = new ConcurrentHashMap<>();
    
    // 锁续期统计
    private final Map<String, AtomicInteger> renewalCountMap = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> totalRenewalDurationMap = new ConcurrentHashMap<>();
    
    /**
     * 记录一次锁获取尝试（无论成功与否都会调用）
     * @param lockKey 锁的key
     * @param threadId 线程ID
     * @return 本次尝试的统计对象
     */
    @Override
    public LockStatistics recordLockAttempt(String lockKey, String threadId) {
        LockStatistics statistics = LockStatistics.builder()
                .lockKey(lockKey)
                .threadId(threadId)
                .waitStartTime(LocalDateTime.now())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .success(false)
                .renewalCount(0)
                .totalRenewalDuration(0L)
                .build();
        
        lockStatisticsMap.computeIfAbsent(lockKey, k -> new ArrayList<>()).add(statistics);
        log.info("记录锁获取尝试: lockKey={}, threadId={}", lockKey, threadId);
        
        return statistics;
    }
    
    /**
     * 记录锁获取成功（会补充等待时长、成功标记等信息）
     * @param lockKey 锁的key
     * @param threadId 线程ID
     * @param waitDuration 等待时长（毫秒）
     */
    @Override
    public void recordLockAcquired(String lockKey, String threadId, Long waitDuration) {
        List<LockStatistics> statisticsList = lockStatisticsMap.get(lockKey);
        if (statisticsList != null) {
            LockStatistics statistics = statisticsList.stream()
                    .filter(s -> s.getThreadId().equals(threadId))
                    .filter(s -> s.getAcquireTime() == null)
                    .findFirst()
                    .orElse(null);
            
            if (statistics != null) {
                statistics.setAcquireTime(LocalDateTime.now());
                statistics.setWaitDuration(waitDuration);
                statistics.setSuccess(true);
                statistics.setUpdateTime(LocalDateTime.now());
                
                log.info("记录锁获取成功: lockKey={}, threadId={}, waitDuration={}ms", 
                        lockKey, threadId, waitDuration);
            }
        }
    }
    
    /**
     * 记录锁释放（会补充持有时长、续期次数等信息）
     * @param lockKey 锁的key
     * @param threadId 线程ID
     * @param holdDuration 持有时长（毫秒）
     */
    @Override
    public void recordLockReleased(String lockKey, String threadId, Long holdDuration) {
        List<LockStatistics> statisticsList = lockStatisticsMap.get(lockKey);
        if (statisticsList != null) {
            LockStatistics statistics = statisticsList.stream()
                    .filter(s -> s.getThreadId().equals(threadId))
                    .filter(s -> s.getReleaseTime() == null)
                    .findFirst()
                    .orElse(null);
            
            if (statistics != null) {
                statistics.setReleaseTime(LocalDateTime.now());
                statistics.setHoldDuration(holdDuration);
                statistics.setUpdateTime(LocalDateTime.now());
                
                // 更新续期统计
                AtomicInteger renewalCount = renewalCountMap.get(lockKey + ":" + threadId);
                AtomicInteger totalRenewalDuration = totalRenewalDurationMap.get(lockKey + ":" + threadId);
                if (renewalCount != null && totalRenewalDuration != null) {
                    statistics.setRenewalCount(renewalCount.get());
                    statistics.setTotalRenewalDuration((long) totalRenewalDuration.get());
                }
                
                log.info("记录锁释放: lockKey={}, threadId={}, holdDuration={}ms, renewalCount={}", 
                        lockKey, threadId, holdDuration, statistics.getRenewalCount());
            }
        }
    }
    
    /**
     * 记录锁续期（每次Redisson自动续期时调用）
     * @param lockKey 锁的key
     * @param threadId 线程ID
     * @param renewalDuration 本次续期时长（毫秒）
     */
    @Override
    public void recordLockRenewal(String lockKey, String threadId, Long renewalDuration) {
        String key = lockKey + ":" + threadId;
        renewalCountMap.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
        totalRenewalDurationMap.computeIfAbsent(key, k -> new AtomicInteger(0))
                .addAndGet(renewalDuration.intValue());
        
        log.debug("记录锁续期: lockKey={}, threadId={}, renewalDuration={}ms", 
                lockKey, threadId, renewalDuration);
    }
    
    /**
     * 记录锁获取失败（会补充等待时长、失败标记等信息）
     * @param lockKey 锁的key
     * @param threadId 线程ID
     * @param waitDuration 等待时长（毫秒）
     */
    @Override
    public void recordLockFailed(String lockKey, String threadId, Long waitDuration) {
        List<LockStatistics> statisticsList = lockStatisticsMap.get(lockKey);
        if (statisticsList != null) {
            LockStatistics statistics = statisticsList.stream()
                    .filter(s -> s.getThreadId().equals(threadId))
                    .filter(s -> s.getAcquireTime() == null)
                    .findFirst()
                    .orElse(null);
            
            if (statistics != null) {
                statistics.setWaitDuration(waitDuration);
                statistics.setSuccess(false);
                statistics.setUpdateTime(LocalDateTime.now());
                
                log.info("记录锁获取失败: lockKey={}, threadId={}, waitDuration={}ms", 
                        lockKey, threadId, waitDuration);
            }
        }
    }
    
    /**
     * 获取指定锁的所有统计信息（详细列表）
     * @param lockKey 锁的key
     * @return 该锁的所有统计信息
     */
    @Override
    public List<LockStatistics> getLockStatistics(String lockKey) {
        return lockStatisticsMap.getOrDefault(lockKey, new ArrayList<>());
    }
    
    /**
     * 获取所有锁的统计信息（Map结构）
     * @return 所有锁的统计信息
     */
    @Override
    public Map<String, List<LockStatistics>> getAllLockStatistics() {
        return new HashMap<>(lockStatisticsMap);
    }
    
    /**
     * 获取指定锁的竞争统计（聚合分析）
     * @param lockKey 锁的key
     * @return 该锁的竞争统计信息（如成功率、平均等待/持有时长等）
     */
    @Override
    public Map<String, Object> getLockCompetitionStats(String lockKey) {
        List<LockStatistics> statisticsList = getLockStatistics(lockKey);
        
        Map<String, Object> stats = new HashMap<>();
        
        if (statisticsList.isEmpty()) {
            stats.put("message", "暂无锁统计信息");
            return stats;
        }
        
        // 计算基本统计信息
        long totalAttempts = statisticsList.size();
        long successfulAttempts = statisticsList.stream()
                .filter(LockStatistics::getSuccess)
                .count();
        long failedAttempts = totalAttempts - successfulAttempts;
        
        // 计算等待时间统计
        DoubleSummaryStatistics waitStats = statisticsList.stream()
                .filter(s -> s.getWaitDuration() != null)
                .mapToDouble(s -> s.getWaitDuration())
                .summaryStatistics();
        
        // 计算持有时长统计
        DoubleSummaryStatistics holdStats = statisticsList.stream()
                .filter(s -> s.getHoldDuration() != null)
                .mapToDouble(s -> s.getHoldDuration())
                .summaryStatistics();
        
        // 计算续期统计
        int totalRenewals = statisticsList.stream()
                .filter(s -> s.getRenewalCount() != null)
                .mapToInt(LockStatistics::getRenewalCount)
                .sum();
        
        long totalRenewalDuration = statisticsList.stream()
                .filter(s -> s.getTotalRenewalDuration() != null)
                .mapToLong(LockStatistics::getTotalRenewalDuration)
                .sum();
        
        stats.put("lockKey", lockKey);
        stats.put("totalAttempts", totalAttempts);
        stats.put("successfulAttempts", successfulAttempts);
        stats.put("failedAttempts", failedAttempts);
        stats.put("successRate", totalAttempts > 0 ? (double) successfulAttempts / totalAttempts : 0.0);
        
        // 等待时间统计
        Map<String, Object> waitTimeStats = new HashMap<>();
        waitTimeStats.put("min", waitStats.getMin());
        waitTimeStats.put("max", waitStats.getMax());
        waitTimeStats.put("average", waitStats.getAverage());
        waitTimeStats.put("count", waitStats.getCount());
        stats.put("waitTimeStats", waitTimeStats);
        
        // 持有时长统计
        Map<String, Object> holdTimeStats = new HashMap<>();
        holdTimeStats.put("min", holdStats.getMin());
        holdTimeStats.put("max", holdStats.getMax());
        holdTimeStats.put("average", holdStats.getAverage());
        holdTimeStats.put("count", holdStats.getCount());
        stats.put("holdTimeStats", holdTimeStats);
        
        // 续期统计
        stats.put("totalRenewals", totalRenewals);
        stats.put("totalRenewalDuration", totalRenewalDuration);
        stats.put("averageRenewalsPerLock", successfulAttempts > 0 ? (double) totalRenewals / successfulAttempts : 0.0);
        
        return stats;
    }
    
    /**
     * 清理过期的统计信息（默认清理24小时以前的数据）
     */
    @Override
    public void cleanupExpiredStatistics() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24); // 清理24小时前的数据
        
        lockStatisticsMap.forEach((lockKey, statisticsList) -> {
            statisticsList.removeIf(statistics -> 
                    statistics.getCreateTime() != null && 
                    statistics.getCreateTime().isBefore(cutoffTime));
        });
        
        // 清理空的统计列表
        lockStatisticsMap.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        
        log.info("清理过期锁统计信息完成");
    }
} 