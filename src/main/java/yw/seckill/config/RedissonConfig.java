package yw.seckill.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson配置类
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.database}")
    private int redisDatabase;

    @Value("${spring.data.redis.timeout:3000}")
    private int timeout;

    /**
     * 配置Redisson客户端
     */
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        
        // 单节点配置
        config.useSingleServer()
                .setAddress("redis://" + redisHost + ":" + redisPort)
                .setDatabase(redisDatabase)
                .setConnectTimeout(timeout)
                .setConnectionMinimumIdleSize(1)
                .setConnectionPoolSize(8)
                .setIdleConnectionTimeout(10000)
                .setRetryAttempts(3)
                .setRetryInterval(1500)
                .setSubscriptionsPerConnection(5)
                .setSubscriptionConnectionMinimumIdleSize(1)
                .setSubscriptionConnectionPoolSize(50);

        // 配置锁的看门狗机制
        config.setLockWatchdogTimeout(30000); // 30秒，看门狗超时时间
        config.setKeepPubSubOrder(true); // 保持发布订阅顺序

        return Redisson.create(config);
    }
} 