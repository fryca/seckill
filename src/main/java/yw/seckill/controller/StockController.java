package yw.seckill.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import yw.seckill.config.ApiResponse;
import yw.seckill.dto.StockRequestDTO;
import yw.seckill.service.StockService;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 库存控制器
 * 提供库存管理的REST API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/stock")
public class StockController {

    @Autowired
    private StockService stockService;

    private final AtomicLong orderIdGenerator = new AtomicLong(1);

    /**
     * 库存扣减
     */
    @PostMapping("/deduct")
    public ApiResponse<Map<String, Object>> deductStock(@Valid @RequestBody StockRequestDTO request) {
        try {
            Long orderId = orderIdGenerator.incrementAndGet();
            StockService.StockDeductResult result = stockService.deductStock(
                request.getProductId(), 
                request.getQuantity(), 
                orderId, 
                request.getUserId()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("productId", request.getProductId());
            response.put("quantity", request.getQuantity());
            response.put("newStock", result.getNewStock());
            response.put("originalStock", result.getOriginalStock());
            response.put("orderId", orderId);

            if (result.isSuccess()) {
                log.info("库存扣减成功: productId={}, quantity={}, newStock={}", 
                    request.getProductId(), request.getQuantity(), result.getNewStock());
                return ApiResponse.success(response);
            } else {
                log.warn("库存扣减失败: productId={}, quantity={}, currentStock={}", 
                    request.getProductId(), request.getQuantity(), result.getOriginalStock());
                return ApiResponse.error("库存不足");
            }
        } catch (Exception e) {
            log.error("库存扣减异常: productId={}, quantity={}", request.getProductId(), request.getQuantity(), e);
            return ApiResponse.error("库存扣减异常: " + e.getMessage());
        }
    }

    /**
     * 批量库存扣减
     */
    @PostMapping("/deduct/batch")
    public ApiResponse<List<Map<String, Object>>> batchDeductStock(@Valid @RequestBody List<StockRequestDTO> requests) {
        try {
            if (requests.size() > 100) {
                return ApiResponse.error("批量扣减数量不能超过100");
            }

            List<StockService.StockDeductRequest> deductRequests = requests.stream()
                .map(request -> new StockService.StockDeductRequest(
                    request.getProductId(),
                    request.getQuantity(),
                    orderIdGenerator.incrementAndGet(),
                    request.getUserId()
                ))
                .toList();

            List<StockService.StockDeductResult> results = stockService.batchDeductStock(deductRequests);

            List<Map<String, Object>> response = results.stream()
                .map(result -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("success", result.isSuccess());
                    item.put("newStock", result.getNewStock());
                    item.put("originalStock", result.getOriginalStock());
                    item.put("deductQuantity", result.getDeductQuantity());
                    return item;
                })
                .toList();

            log.info("批量库存扣减完成: 请求数量={}, 成功数量={}", 
                requests.size(), results.stream().filter(StockService.StockDeductResult::isSuccess).count());
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("批量库存扣减异常", e);
            return ApiResponse.error("批量库存扣减异常: " + e.getMessage());
        }
    }

    /**
     * 库存预占
     */
    @PostMapping("/pre-occupy")
    public ApiResponse<Map<String, Object>> preOccupyStock(@Valid @RequestBody StockRequestDTO request,
                                                          @RequestParam(defaultValue = "300") Long expireSeconds) {
        try {
            StockService.StockPreOccupyResult result = stockService.preOccupyStock(
                request.getProductId(), 
                request.getQuantity(), 
                expireSeconds
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("productId", request.getProductId());
            response.put("quantity", request.getQuantity());
            response.put("availableStock", result.getAvailableStock());
            response.put("totalStock", result.getTotalStock());
            response.put("expireSeconds", expireSeconds);

            if (result.isSuccess()) {
                log.info("库存预占成功: productId={}, quantity={}, availableStock={}", 
                    request.getProductId(), request.getQuantity(), result.getAvailableStock());
                return ApiResponse.success(response);
            } else {
                log.warn("库存预占失败: productId={}, quantity={}, availableStock={}", 
                    request.getProductId(), request.getQuantity(), result.getAvailableStock());
                return ApiResponse.error("库存不足");
            }
        } catch (Exception e) {
            log.error("库存预占异常: productId={}, quantity={}", request.getProductId(), request.getQuantity(), e);
            return ApiResponse.error("库存预占异常: " + e.getMessage());
        }
    }

    /**
     * 获取商品库存
     */
    @GetMapping("/{productId}")
    public ApiResponse<Map<String, Object>> getStock(@PathVariable Long productId) {
        try {
            int stock = stockService.getStock(productId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("productId", productId);
            response.put("stock", stock);
            
            log.info("获取库存成功: productId={}, stock={}", productId, stock);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取库存异常: productId={}", productId, e);
            return ApiResponse.error("获取库存异常: " + e.getMessage());
        }
    }

    /**
     * 设置商品库存
     */
    @PostMapping("/set")
    public ApiResponse<Map<String, Object>> setStock(@RequestParam Long productId, 
                                                    @RequestParam Integer stock) {
        try {
            stockService.setStock(productId, stock);
            
            Map<String, Object> response = new HashMap<>();
            response.put("productId", productId);
            response.put("stock", stock);
            
            log.info("设置库存成功: productId={}, stock={}", productId, stock);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("设置库存异常: productId={}, stock={}", productId, stock, e);
            return ApiResponse.error("设置库存异常: " + e.getMessage());
        }
    }

    /**
     * 增加商品库存
     */
    @PostMapping("/increase")
    public ApiResponse<Map<String, Object>> increaseStock(@RequestParam Long productId, 
                                                        @RequestParam Integer quantity) {
        try {
            stockService.increaseStock(productId, quantity);
            
            Map<String, Object> response = new HashMap<>();
            response.put("productId", productId);
            response.put("quantity", quantity);
            
            log.info("增加库存成功: productId={}, quantity={}", productId, quantity);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("增加库存异常: productId={}, quantity={}", productId, quantity, e);
            return ApiResponse.error("增加库存异常: " + e.getMessage());
        }
    }

    /**
     * 获取库存日志
     */
    @GetMapping("/logs/{productId}")
    public ApiResponse<Map<String, Object>> getStockLogs(@PathVariable Long productId,
                                                        @RequestParam(defaultValue = "10") Integer limit) {
        try {
            List<String> logs = stockService.getStockLogs(productId, limit);
            
            Map<String, Object> response = new HashMap<>();
            response.put("productId", productId);
            response.put("logs", logs);
            response.put("count", logs.size());
            
            log.info("获取库存日志成功: productId={}, count={}", productId, logs.size());
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取库存日志异常: productId={}", productId, e);
            return ApiResponse.error("获取库存日志异常: " + e.getMessage());
        }
    }

    /**
     * 性能测试接口
     */
    @PostMapping("/test/performance")
    public ApiResponse<Map<String, Object>> testPerformance(@RequestParam(defaultValue = "1000") Integer count) {
        try {
            long startTime = System.currentTimeMillis();
            int successCount = 0;
            int failCount = 0;

            for (int i = 0; i < count; i++) {
                Long productId = (long) (i % 10 + 1); // 10个商品循环
                StockService.StockDeductResult result = stockService.deductStock(
                    productId, 1, orderIdGenerator.incrementAndGet(), (long) (i + 1)
                );
                
                if (result.isSuccess()) {
                    successCount++;
                } else {
                    failCount++;
                }
            }

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            double qps = (count * 1000.0) / duration;

            Map<String, Object> response = new HashMap<>();
            response.put("totalCount", count);
            response.put("successCount", successCount);
            response.put("failCount", failCount);
            response.put("duration", duration);
            response.put("qps", String.format("%.2f", qps));

            log.info("性能测试完成: 总数={}, 成功={}, 失败={}, 耗时={}ms, QPS={}", 
                count, successCount, failCount, duration, String.format("%.2f", qps));
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("性能测试异常", e);
            return ApiResponse.error("性能测试异常: " + e.getMessage());
        }
    }
} 