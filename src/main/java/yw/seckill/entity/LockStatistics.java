package yw.seckill.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 锁统计信息实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LockStatistics {
    
    /**
     * 锁的key
     */
    private String lockKey;
    
    /**
     * 线程ID
     */
    private String threadId;
    
    /**
     * 锁等待开始时间
     */
    private LocalDateTime waitStartTime;
    
    /**
     * 锁获取时间
     */
    private LocalDateTime acquireTime;
    
    /**
     * 锁释放时间
     */
    private LocalDateTime releaseTime;
    
    /**
     * 锁等待时长（毫秒）
     */
    private Long waitDuration;
    
    /**
     * 锁持有时长（毫秒）
     */
    private Long holdDuration;
    
    /**
     * 是否成功获取锁
     */
    private Boolean success;
    
    /**
     * 锁续期次数
     */
    private Integer renewalCount;
    
    /**
     * 锁续期总时长（毫秒）
     */
    private Long totalRenewalDuration;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
} 