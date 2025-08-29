package yw.seckill.util;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 库存分桶工具（Redis Hash）
 * - 将单商品库存拆分为固定数量的桶（默认10个），缓解热点Key的并发压力
 * - 自动将请求基于路由键（如userId、orderId等）映射到固定桶
 * - 支持失败时按顺序尝试其它桶
 *
 * Hash结构：key = "stock:" + productId, field = bucketIndex(0..bucketCount-1), value = 剩余库存
 */
@Slf4j
@Component
public class StockBucketUtil {

    @Autowired
    private RedissonUtil redissonUtil;

    private static final String STOCK_HASH_KEY_PREFIX = "stock:";
    private static final int DEFAULT_BUCKET_COUNT = 10;

    // 单桶扣减脚本：对Hash的指定field进行扣减，保证原子性
    private static final String HASH_BUCKET_DEDUCT_SCRIPT =
            "local hash_key = KEYS[1] " +
            "local field = ARGV[1] " +
            "local deduct_amount = tonumber(ARGV[2]) " +
            "local current_stock = tonumber(redis.call('HGET', hash_key, field) or 0) " +
            "if current_stock >= deduct_amount then " +
            "  local new_stock = current_stock - deduct_amount " +
            "  redis.call('HSET', hash_key, field, new_stock) " +
            "  return {1, new_stock, current_stock} " +
            "else " +
            "  return {0, current_stock, current_stock} " +
            "end";

    /**
     * 生成Hash Key
     */
    public String buildHashKey(Long productId) {
        return STOCK_HASH_KEY_PREFIX + productId;
    }

    /**
     * 桶数量（可根据需要扩展成可配置）
     */
    public int getBucketCount() {
        return DEFAULT_BUCKET_COUNT;
    }

    /**
     * 基于路由键计算稳定桶索引（一致性：同一routeKey始终落在同一桶）
     */
    public int routeBucketIndex(Long productId, Object routeKey) {
        int bucketCount = getBucketCount();
        if (routeKey == null) {
            return ThreadLocalRandom.current().nextInt(bucketCount);
        }
        int hash = stableHash(productId, routeKey);
        int idx = Math.floorMod(hash, bucketCount);
        return idx;
    }

    private int stableHash(Long productId, Object routeKey) {
        // 基于 productId + routeKey 的字节计算稳定哈希
        String key = String.valueOf(productId) + "#" + String.valueOf(routeKey);
        byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
        int h = 0;
        for (byte b : bytes) {
            h = 31 * h + (b & 0xff);
        }
        return h;
    }

    /**
     * 初始化分桶库存：将 totalStock 均匀切分到各桶（余数从低索引开始+1）
     */
    public void initBucketStocks(Long productId, int totalStock) {
        String hashKey = buildHashKey(productId);
        int bucketCount = getBucketCount();
        if (totalStock < 0) {
            throw new IllegalArgumentException("totalStock 不能为负数");
        }

        int avg = bucketCount == 0 ? 0 : totalStock / bucketCount;
        int remainder = bucketCount == 0 ? 0 : totalStock % bucketCount;

        for (int i = 0; i < bucketCount; i++) {
            int value = avg + (i < remainder ? 1 : 0);
            try {
                redissonUtil.getRedissonClient().getMap(hashKey).fastPut(String.valueOf(i), value);
            } catch (Exception e) {
                log.error("初始化分桶库存失败: productId={}, bucket={}, value={}", productId, i, value, e);
                throw e;
            }
        }
        log.info("初始化分桶库存成功: productId={}, totalStock={}, bucketCount={}", productId, totalStock, bucketCount);
    }

    /**
     * 获取总库存（聚合所有桶）
     */
    public int getTotalStock(Long productId) {
        try {
            String hashKey = buildHashKey(productId);
            List<Object> values = new ArrayList<>(redissonUtil.getRedissonClient().getMap(hashKey).values());
            int sum = 0;
            for (Object v : values) {
                if (v != null) {
                    sum += Integer.parseInt(String.valueOf(v));
                }
            }
            return sum;
        } catch (Exception e) {
            log.error("获取总库存异常: productId={}", productId, e);
            return 0;
        }
    }

    /**
     * 获取单个桶库存
     */
    public int getBucketStock(Long productId, int bucketIndex) {
        try {
            String hashKey = buildHashKey(productId);
            Object v = redissonUtil.getRedissonClient().getMap(hashKey).get(String.valueOf(bucketIndex));
            return v == null ? 0 : Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            log.error("获取桶库存异常: productId={}, bucketIndex={}", productId, bucketIndex, e);
            return 0;
        }
    }

    /**
     * 自动分配到路由桶进行扣减；若失败，按顺序尝试其余桶
     * @param productId 商品ID
     * @param quantity 扣减数量（正整数）
     * @param routeKey 路由键（如userId、orderId等）；可为null
     * @return 扣减结果
     */
    public DeductResult deductAuto(Long productId, int quantity, Object routeKey) {
        Objects.requireNonNull(productId, "productId不能为空");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity必须为正数");
        }

        String hashKey = buildHashKey(productId);
        int bucketCount = getBucketCount();
        int primary = routeBucketIndex(productId, routeKey);

        // 构造尝试顺序：primary -> 其他桶（从primary+1开始循环）
        List<Integer> order = new ArrayList<>(bucketCount);
        for (int i = 0; i < bucketCount; i++) {
            order.add((primary + i) % bucketCount);
        }

        for (int bucketIndex : order) {
            List<Object> result = evalHashBucketDeduct(hashKey, String.valueOf(bucketIndex), quantity);
            boolean success = ((Number) result.get(0)).intValue() == 1;
            int newBucketStock = ((Number) result.get(1)).intValue();
            int beforeBucketStock = ((Number) result.get(2)).intValue();

            if (success) {
                log.info("扣减成功: productId={}, bucketIndex={}, quantity={}, newBucketStock={}",
                        productId, bucketIndex, quantity, newBucketStock);
                return new DeductResult(true, productId, bucketIndex, quantity, newBucketStock, beforeBucketStock);
            }
        }

        log.warn("扣减失败（所有桶不足）: productId={}, quantity={}", productId, quantity);
        return new DeductResult(false, productId, -1, quantity, 0, 0);
    }

    /**
     * 仅在指定桶上扣减（不会尝试其它桶）
     */
    public DeductResult deductInBucket(Long productId, int bucketIndex, int quantity) {
        Objects.requireNonNull(productId, "productId不能为空");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity必须为正数");
        }

        String hashKey = buildHashKey(productId);
        List<Object> result = evalHashBucketDeduct(hashKey, String.valueOf(bucketIndex), quantity);
        boolean success = ((Number) result.get(0)).intValue() == 1;
        int newBucketStock = ((Number) result.get(1)).intValue();
        int beforeBucketStock = ((Number) result.get(2)).intValue();
        if (success) {
            log.info("扣减成功: productId={}, bucketIndex={}, quantity={}, newBucketStock={}",
                    productId, bucketIndex, quantity, newBucketStock);
        } else {
            log.warn("扣减失败: productId={}, bucketIndex={}, quantity={}, currentBucketStock={}",
                    productId, bucketIndex, quantity, beforeBucketStock);
        }
        return new DeductResult(success, productId, bucketIndex, quantity, newBucketStock, beforeBucketStock);
    }

    private List<Object> evalHashBucketDeduct(String hashKey, String field, int quantity) {
        return redissonUtil.getRedissonClient().getScript().eval(
                RScript.Mode.READ_WRITE,
                HASH_BUCKET_DEDUCT_SCRIPT,
                RScript.ReturnType.MULTI,
                Collections.singletonList(hashKey),
                field, String.valueOf(quantity)
        );
    }

    /**
     * 扣减结果
     */
    public static class DeductResult {
        private final boolean success;
        private final Long productId;
        private final int bucketIndex;
        private final int deductQuantity;
        private final int newBucketStock;
        private final int beforeBucketStock;

        public DeductResult(boolean success, Long productId, int bucketIndex, int deductQuantity,
                            int newBucketStock, int beforeBucketStock) {
            this.success = success;
            this.productId = productId;
            this.bucketIndex = bucketIndex;
            this.deductQuantity = deductQuantity;
            this.newBucketStock = newBucketStock;
            this.beforeBucketStock = beforeBucketStock;
        }

        public boolean isSuccess() { return success; }
        public Long getProductId() { return productId; }
        public int getBucketIndex() { return bucketIndex; }
        public int getDeductQuantity() { return deductQuantity; }
        public int getNewBucketStock() { return newBucketStock; }
        public int getBeforeBucketStock() { return beforeBucketStock; }
    }
}

