package yw.seckill.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import yw.seckill.util.RedissonUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 资格令牌服务
 * 使用Redis INCR生成全局唯一令牌，设置TTL防止重复使用
 */
@Slf4j
@Service
public class TokenService {

    @Autowired
    private RedissonUtil redissonUtil;

    private static final String TOKEN_COUNTER_KEY = "token:counter";
    private static final String TOKEN_PREFIX = "token:";
    private static final String ACTIVITY_TOKEN_COUNTER_KEY = "activity:token:counter:";
    private static final String ACTIVITY_TOKEN_PREFIX = "activity:token:";

    /**
     * 生成单个全局唯一令牌
     *
     * @param activityId 活动ID
     * @param ttlSeconds 令牌有效期（秒）
     * @return 令牌字符串
     */
    public String generateToken(Long activityId, long ttlSeconds) {
        try {
            // 使用Redis INCR生成全局唯一ID
            String counterKey = ACTIVITY_TOKEN_COUNTER_KEY + activityId;
            Long tokenId = redissonUtil.getRedissonClient().getAtomicLong(counterKey).incrementAndGet();
            
            // 生成令牌字符串
            String token = String.format("T%d_%d_%d", activityId, System.currentTimeMillis(), tokenId);
            
            // 将令牌存储到Redis，设置TTL
            String tokenKey = ACTIVITY_TOKEN_PREFIX + token;
            redissonUtil.getRedissonClient().getBucket(tokenKey).set("1", ttlSeconds, TimeUnit.SECONDS);
            
            log.info("生成令牌成功: activityId={}, token={}, ttl={}s", activityId, token, ttlSeconds);
            return token;
        } catch (Exception e) {
            log.error("生成令牌失败: activityId={}", activityId, e);
            throw new RuntimeException("生成令牌失败", e);
        }
    }

    /**
     * 批量预生成令牌
     *
     * @param activityId 活动ID
     * @param count 生成数量
     * @param ttlSeconds 令牌有效期（秒）
     * @return 令牌列表
     */
    public List<String> generateBatchTokens(Long activityId, int count, long ttlSeconds) {
        List<String> tokens = new ArrayList<>();
        
        try {
            // 使用分布式锁确保批量生成的原子性
            String lockKey = "token_batch_lock:" + activityId;
            boolean locked = redissonUtil.tryLock(lockKey, 10, 30, TimeUnit.SECONDS);
            
            if (locked) {
                try {
                    for (int i = 0; i < count; i++) {
                        String token = generateToken(activityId, ttlSeconds);
                        tokens.add(token);
                    }
                    log.info("批量生成令牌成功: activityId={}, count={}, ttl={}s", activityId, count, ttlSeconds);
                } finally {
                    redissonUtil.unlock(lockKey);
                }
            } else {
                throw new RuntimeException("获取批量生成锁失败");
            }
        } catch (Exception e) {
            log.error("批量生成令牌失败: activityId={}, count={}", activityId, count, e);
            throw new RuntimeException("批量生成令牌失败", e);
        }
        
        return tokens;
    }

    /**
     * 验证令牌是否有效
     *
     * @param token 令牌字符串
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            String tokenKey = ACTIVITY_TOKEN_PREFIX + token;
            Object value = redissonUtil.getRedissonClient().getBucket(tokenKey).get();
            boolean isValid = value != null;
            
            if (isValid) {
                log.info("令牌验证成功: token={}", token);
            } else {
                log.warn("令牌验证失败: token={}", token);
            }
            
            return isValid;
        } catch (Exception e) {
            log.error("令牌验证异常: token={}", token, e);
            return false;
        }
    }

    /**
     * 使用令牌（消费后删除）
     *
     * @param token 令牌字符串
     * @return 是否使用成功
     */
    public boolean consumeToken(String token) {
        try {
            String tokenKey = ACTIVITY_TOKEN_PREFIX + token;
            Object value = redissonUtil.getRedissonClient().getBucket(tokenKey).getAndDelete();
            boolean consumed = value != null;
            
            if (consumed) {
                log.info("令牌使用成功: token={}", token);
            } else {
                log.warn("令牌使用失败: token={}", token);
            }
            
            return consumed;
        } catch (Exception e) {
            log.error("令牌使用异常: token={}", token, e);
            return false;
        }
    }

    /**
     * 获取令牌剩余有效期
     *
     * @param token 令牌字符串
     * @return 剩余秒数，-1表示不存在，-2表示永久
     */
    public long getTokenTTL(String token) {
        try {
            String tokenKey = ACTIVITY_TOKEN_PREFIX + token;
            return redissonUtil.getRedissonClient().getBucket(tokenKey).remainTimeToLive();
        } catch (Exception e) {
            log.error("获取令牌TTL异常: token={}", token, e);
            return -1;
        }
    }

    /**
     * 刷新令牌有效期
     *
     * @param token 令牌字符串
     * @param ttlSeconds 新的有效期（秒）
     * @return 是否刷新成功
     */
    public boolean refreshTokenTTL(String token, long ttlSeconds) {
        try {
            String tokenKey = ACTIVITY_TOKEN_PREFIX + token;
            Object value = redissonUtil.getRedissonClient().getBucket(tokenKey).get();
            
            if (value != null) {
                redissonUtil.getRedissonClient().getBucket(tokenKey).expire(ttlSeconds, TimeUnit.SECONDS);
                log.info("刷新令牌TTL成功: token={}, ttl={}s", token, ttlSeconds);
                return true;
            } else {
                log.warn("令牌不存在，无法刷新TTL: token={}", token);
                return false;
            }
        } catch (Exception e) {
            log.error("刷新令牌TTL异常: token={}", token, e);
            return false;
        }
    }

    /**
     * 获取活动令牌统计信息
     *
     * @param activityId 活动ID
     * @return 统计信息
     */
    public TokenStatistics getTokenStatistics(Long activityId) {
        try {
            String counterKey = ACTIVITY_TOKEN_COUNTER_KEY + activityId;
            Long totalGenerated = redissonUtil.getRedissonClient().getAtomicLong(counterKey).get();
            
            // 这里可以添加更多统计信息的逻辑
            // 比如已使用的令牌数量、剩余有效令牌数量等
            
            return new TokenStatistics(activityId, totalGenerated != null ? totalGenerated : 0L);
        } catch (Exception e) {
            log.error("获取令牌统计信息异常: activityId={}", activityId, e);
            return new TokenStatistics(activityId, 0L);
        }
    }

    /**
     * 令牌统计信息
     */
    public static class TokenStatistics {
        private Long activityId;
        private Long totalGenerated;

        public TokenStatistics(Long activityId, Long totalGenerated) {
            this.activityId = activityId;
            this.totalGenerated = totalGenerated;
        }

        public Long getActivityId() {
            return activityId;
        }

        public Long getTotalGenerated() {
            return totalGenerated;
        }

        @Override
        public String toString() {
            return "TokenStatistics{" +
                    "activityId=" + activityId +
                    ", totalGenerated=" + totalGenerated +
                    '}';
        }
    }
} 