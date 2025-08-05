package yw.seckill.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import yw.seckill.util.RedissonUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 库存服务
 * 使用Lua脚本实现原子化的库存扣减操作
 */
@Slf4j
@Service
public class StockService {

    @Autowired
    private RedissonUtil redissonUtil;

    // Redis键前缀
    private static final String STOCK_KEY_PREFIX = "stock:";
    private static final String STOCK_LOCK_PREFIX = "stock_lock:";
    private static final String STOCK_LOG_PREFIX = "stock_log:";

    // Lua脚本：原子化库存扣减
    private static final String STOCK_DEDUCT_SCRIPT = 
        "local stock_key = KEYS[1] " +
        "local deduct_amount = tonumber(ARGV[1]) " +
        "local log_key = KEYS[2] " +
        "local log_data = ARGV[2] " +
        "local current_stock = tonumber(redis.call('GET', stock_key) or 0) " +
        "if current_stock >= deduct_amount then " +
        "  local new_stock = current_stock - deduct_amount " +
        "  redis.call('SET', stock_key, new_stock) " +
        "  redis.call('LPUSH', log_key, log_data) " +
        "  redis.call('EXPIRE', log_key, 86400) " +  // 日志保存24小时
        "  return {1, new_stock, current_stock} " +   // 成功：{1, 新库存, 原库存}
        "else " +
        "  return {0, current_stock, current_stock} " + // 失败：{0, 当前库存, 当前库存}
        "end";

    // Lua脚本：批量库存扣减
    private static final String BATCH_STOCK_DEDUCT_SCRIPT = 
        "local results = {} " +
        "for i = 1, #KEYS do " +
        "  local stock_key = KEYS[i] " +
        "  local deduct_amount = tonumber(ARGV[i]) " +
        "  local log_key = 'stock_log:' .. stock_key " +
        "  local log_data = ARGV[i + #KEYS] " +
        "  local current_stock = tonumber(redis.call('GET', stock_key) or 0) " +
        "  if current_stock >= deduct_amount then " +
        "    local new_stock = current_stock - deduct_amount " +
        "    redis.call('SET', stock_key, new_stock) " +
        "    redis.call('LPUSH', log_key, log_data) " +
        "    redis.call('EXPIRE', log_key, 86400) " +
        "    table.insert(results, {1, new_stock, current_stock}) " +
        "  else " +
        "    table.insert(results, {0, current_stock, current_stock}) " +
        "  end " +
        "end " +
        "return results";

    // Lua脚本：库存预占
    private static final String STOCK_PRE_OCCUPY_SCRIPT = 
        "local stock_key = KEYS[1] " +
        "local occupy_amount = tonumber(ARGV[1]) " +
        "local expire_time = tonumber(ARGV[2]) " +
        "local occupy_key = KEYS[2] " +
        "local current_stock = tonumber(redis.call('GET', stock_key) or 0) " +
        "local occupied_stock = tonumber(redis.call('GET', occupy_key) or 0) " +
        "local available_stock = current_stock - occupied_stock " +
        "if available_stock >= occupy_amount then " +
        "  redis.call('INCRBY', occupy_key, occupy_amount) " +
        "  redis.call('EXPIRE', occupy_key, expire_time) " +
        "  return {1, available_stock - occupy_amount, current_stock} " +
        "else " +
        "  return {0, available_stock, current_stock} " +
        "end";

    /**
     * 原子化库存扣减
     *
     * @param productId 商品ID
     * @param quantity 扣减数量
     * @param orderId 订单ID（用于日志）
     * @param userId 用户ID（用于日志）
     * @return 扣减结果
     */
    public StockDeductResult deductStock(Long productId, int quantity, Long orderId, Long userId) {
        try {
            String stockKey = STOCK_KEY_PREFIX + productId;
            String logKey = STOCK_LOG_PREFIX + productId;
            String logData = String.format("{\"orderId\":%d,\"userId\":%d,\"quantity\":%d,\"timestamp\":%d}", 
                orderId, userId, quantity, System.currentTimeMillis());

            // 执行Lua脚本
            List<Object> result = redissonUtil.getRedissonClient().getScript().eval(
                org.redisson.api.RScript.Mode.READ_WRITE,
                STOCK_DEDUCT_SCRIPT,
                org.redisson.api.RScript.ReturnType.MULTI,
                Arrays.asList(stockKey, logKey),
                quantity, logData
            );

            boolean success = ((Number) result.get(0)).intValue() == 1;
            int newStock = ((Number) result.get(1)).intValue();
            int originalStock = ((Number) result.get(2)).intValue();

            StockDeductResult deductResult = new StockDeductResult(success, newStock, originalStock, quantity);
            
            if (success) {
                log.info("库存扣减成功: productId={}, quantity={}, newStock={}, orderId={}", 
                    productId, quantity, newStock, orderId);
            } else {
                log.warn("库存扣减失败: productId={}, quantity={}, currentStock={}, orderId={}", 
                    productId, quantity, originalStock, orderId);
            }

            return deductResult;
        } catch (Exception e) {
            log.error("库存扣减异常: productId={}, quantity={}", productId, quantity, e);
            return new StockDeductResult(false, 0, 0, quantity);
        }
    }

    /**
     * 批量库存扣减
     *
     * @param deductRequests 扣减请求列表
     * @return 扣减结果列表
     */
    public List<StockDeductResult> batchDeductStock(List<StockDeductRequest> deductRequests) {
        try {
            if (deductRequests.isEmpty()) {
                return List.of();
            }

            // 准备Lua脚本参数
            List<String> keys = new ArrayList<>();
            List<String> args = new ArrayList<>();
            
            for (StockDeductRequest request : deductRequests) {
                String stockKey = STOCK_KEY_PREFIX + request.getProductId();
                keys.add(stockKey);
                args.add(String.valueOf(request.getQuantity()));
                
                String logData = String.format("{\"orderId\":%d,\"userId\":%d,\"quantity\":%d,\"timestamp\":%d}", 
                    request.getOrderId(), request.getUserId(), request.getQuantity(), System.currentTimeMillis());
                args.add(logData);
            }

            // 将 keys 转为 List<Object>
            List<Object> keyObjects = new ArrayList<>(keys);

            // 执行批量Lua脚本
            List<Object> results = redissonUtil.getRedissonClient().getScript().eval(
                org.redisson.api.RScript.Mode.READ_WRITE,
                BATCH_STOCK_DEDUCT_SCRIPT,
                org.redisson.api.RScript.ReturnType.MULTI,
                keyObjects,
                args.toArray(new String[0])
            );

            // 解析结果
            List<StockDeductResult> deductResults = new ArrayList<>();
            for (int i = 0; i < deductRequests.size(); i++) {
                StockDeductRequest request = deductRequests.get(i);
                List<Object> result = (List<Object>) results.get(i);
                
                boolean success = ((Number) result.get(0)).intValue() == 1;
                int newStock = ((Number) result.get(1)).intValue();
                int originalStock = ((Number) result.get(2)).intValue();
                
                StockDeductResult deductResult = new StockDeductResult(success, newStock, originalStock, request.getQuantity());
                deductResults.add(deductResult);
                
                if (success) {
                    log.info("批量库存扣减成功: productId={}, quantity={}, newStock={}", 
                        request.getProductId(), request.getQuantity(), newStock);
                } else {
                    log.warn("批量库存扣减失败: productId={}, quantity={}, currentStock={}", 
                        request.getProductId(), request.getQuantity(), originalStock);
                }
            }

            return deductResults;
        } catch (Exception e) {
            log.error("批量库存扣减异常", e);
            return deductRequests.stream()
                .map(request -> new StockDeductResult(false, 0, 0, request.getQuantity()))
                .toList();
        }
    }

    /**
     * 库存预占（用于秒杀场景）
     *
     * @param productId 商品ID
     * @param quantity 预占数量
     * @param expireSeconds 预占过期时间（秒）
     * @return 预占结果
     */
    public StockPreOccupyResult preOccupyStock(Long productId, int quantity, long expireSeconds) {
        try {
            String stockKey = STOCK_KEY_PREFIX + productId;
            String occupyKey = "stock_occupy:" + productId;

            // 执行Lua脚本
            List<Object> result = redissonUtil.getRedissonClient().getScript().eval(
                org.redisson.api.RScript.Mode.READ_WRITE,
                STOCK_PRE_OCCUPY_SCRIPT,
                org.redisson.api.RScript.ReturnType.MULTI,
                Arrays.asList(stockKey, occupyKey),
                quantity, expireSeconds
            );

            boolean success = ((Number) result.get(0)).intValue() == 1;
            int availableStock = ((Number) result.get(1)).intValue();
            int totalStock = ((Number) result.get(2)).intValue();

            StockPreOccupyResult occupyResult = new StockPreOccupyResult(success, availableStock, totalStock, quantity);
            
            if (success) {
                log.info("库存预占成功: productId={}, quantity={}, availableStock={}", 
                    productId, quantity, availableStock);
            } else {
                log.warn("库存预占失败: productId={}, quantity={}, availableStock={}", 
                    productId, quantity, availableStock);
            }

            return occupyResult;
        } catch (Exception e) {
            log.error("库存预占异常: productId={}, quantity={}", productId, quantity, e);
            return new StockPreOccupyResult(false, 0, 0, quantity);
        }
    }

    /**
     * 获取商品库存
     *
     * @param productId 商品ID
     * @return 库存数量
     */
    public int getStock(Long productId) {
        try {
            String stockKey = STOCK_KEY_PREFIX + productId;
            Object value = redissonUtil.getRedissonClient().getBucket(stockKey).get();
            return value != null ? Integer.parseInt(value.toString()) : 0;
        } catch (Exception e) {
            log.error("获取库存异常: productId={}", productId, e);
            return 0;
        }
    }

    /**
     * 设置商品库存
     *
     * @param productId 商品ID
     * @param stock 库存数量
     */
    public void setStock(Long productId, int stock) {
        try {
            String stockKey = STOCK_KEY_PREFIX + productId;
            redissonUtil.getRedissonClient().getBucket(stockKey).set(stock);
            log.info("设置库存成功: productId={}, stock={}", productId, stock);
        } catch (Exception e) {
            log.error("设置库存异常: productId={}, stock={}", productId, stock, e);
        }
    }

    /**
     * 增加商品库存
     *
     * @param productId 商品ID
     * @param quantity 增加数量
     */
    public void increaseStock(Long productId, int quantity) {
        try {
            String stockKey = STOCK_KEY_PREFIX + productId;
            Long newStock = redissonUtil.getRedissonClient().getAtomicLong(stockKey).addAndGet(quantity);
            log.info("增加库存成功: productId={}, quantity={}, newStock={}", productId, quantity, newStock);
        } catch (Exception e) {
            log.error("增加库存异常: productId={}, quantity={}", productId, quantity, e);
        }
    }

    /**
     * 获取库存扣减日志
     *
     * @param productId 商品ID
     * @param limit 限制数量
     * @return 日志列表
     */
    public List<String> getStockLogs(Long productId, int limit) {
        try {
            String logKey = STOCK_LOG_PREFIX + productId;
            List<Object> logs = redissonUtil.getRedissonClient().getList(logKey).range(0, limit - 1);
            return logs.stream()
                .map(Object::toString)
                .toList();
        } catch (Exception e) {
            log.error("获取库存日志异常: productId={}", productId, e);
            return List.of();
        }
    }

    /**
     * 库存扣减结果
     */
    public static class StockDeductResult {
        private boolean success;
        private int newStock;
        private int originalStock;
        private int deductQuantity;

        public StockDeductResult(boolean success, int newStock, int originalStock, int deductQuantity) {
            this.success = success;
            this.newStock = newStock;
            this.originalStock = originalStock;
            this.deductQuantity = deductQuantity;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public int getNewStock() { return newStock; }
        public int getOriginalStock() { return originalStock; }
        public int getDeductQuantity() { return deductQuantity; }
    }

    /**
     * 库存扣减请求
     */
    public static class StockDeductRequest {
        private Long productId;
        private int quantity;
        private Long orderId;
        private Long userId;

        public StockDeductRequest(Long productId, int quantity, Long orderId, Long userId) {
            this.productId = productId;
            this.quantity = quantity;
            this.orderId = orderId;
            this.userId = userId;
        }

        // Getters
        public Long getProductId() { return productId; }
        public int getQuantity() { return quantity; }
        public Long getOrderId() { return orderId; }
        public Long getUserId() { return userId; }
    }

    /**
     * 库存预占结果
     */
    public static class StockPreOccupyResult {
        private boolean success;
        private int availableStock;
        private int totalStock;
        private int occupyQuantity;

        public StockPreOccupyResult(boolean success, int availableStock, int totalStock, int occupyQuantity) {
            this.success = success;
            this.availableStock = availableStock;
            this.totalStock = totalStock;
            this.occupyQuantity = occupyQuantity;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public int getAvailableStock() { return availableStock; }
        public int getTotalStock() { return totalStock; }
        public int getOccupyQuantity() { return occupyQuantity; }
    }
} 