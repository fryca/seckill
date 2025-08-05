package yw.seckill.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import yw.seckill.config.ApiResponse;
import yw.seckill.dto.TokenRequestDTO;
import yw.seckill.dto.TokenResponseDTO;
import yw.seckill.service.TokenService;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * 令牌控制器
 * 提供令牌生成、验证、使用等API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/token")
public class TokenController {

    @Autowired
    private TokenService tokenService;

    /**
     * 生成单个令牌
     */
    @PostMapping("/generate")
    public ApiResponse<TokenResponseDTO> generateToken(@Valid @RequestBody TokenRequestDTO request) {
        try {
            String token = tokenService.generateToken(request.getActivityId(), request.getTtlSeconds());
            
            TokenResponseDTO response = new TokenResponseDTO();
            response.setActivityId(request.getActivityId());
            response.setTokens(java.util.List.of(token));
            response.setTtlSeconds(request.getTtlSeconds());
            response.setCount(1);
            
            log.info("生成单个令牌成功: activityId={}, token={}", request.getActivityId(), token);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("生成单个令牌失败: activityId={}", request.getActivityId(), e);
            return ApiResponse.error("生成令牌失败: " + e.getMessage());
        }
    }

    /**
     * 批量生成令牌
     */
    @PostMapping("/generate/batch")
    public ApiResponse<TokenResponseDTO> generateBatchTokens(@Valid @RequestBody TokenRequestDTO request) {
        try {
            if (request.getCount() == null || request.getCount() <= 0) {
                return ApiResponse.error("生成数量必须大于0");
            }
            
            if (request.getCount() > 1000) {
                return ApiResponse.error("批量生成数量不能超过1000");
            }
            
            var tokens = tokenService.generateBatchTokens(request.getActivityId(), request.getCount(), request.getTtlSeconds());
            
            TokenResponseDTO response = new TokenResponseDTO();
            response.setActivityId(request.getActivityId());
            response.setTokens(tokens);
            response.setTtlSeconds(request.getTtlSeconds());
            response.setCount(request.getCount());
            
            log.info("批量生成令牌成功: activityId={}, count={}", request.getActivityId(), request.getCount());
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("批量生成令牌失败: activityId={}, count={}", request.getActivityId(), request.getCount(), e);
            return ApiResponse.error("批量生成令牌失败: " + e.getMessage());
        }
    }

    /**
     * 验证令牌
     */
    @PostMapping("/validate")
    public ApiResponse<Map<String, Object>> validateToken(@RequestParam String token) {
        try {
            boolean isValid = tokenService.validateToken(token);
            long ttl = tokenService.getTokenTTL(token);
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("valid", isValid);
            response.put("ttl", ttl);
            
            if (isValid) {
                log.info("令牌验证成功: token={}, ttl={}s", token, ttl);
                return ApiResponse.success(response);
            } else {
                log.warn("令牌验证失败: token={}", token);
                return ApiResponse.error("令牌无效或已过期");
            }
        } catch (Exception e) {
            log.error("令牌验证异常: token={}", token, e);
            return ApiResponse.error("令牌验证异常: " + e.getMessage());
        }
    }

    /**
     * 使用令牌
     */
    @PostMapping("/consume")
    public ApiResponse<Map<String, Object>> consumeToken(@RequestParam String token) {
        try {
            boolean consumed = tokenService.consumeToken(token);
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("consumed", consumed);
            
            if (consumed) {
                log.info("令牌使用成功: token={}", token);
                return ApiResponse.success(response);
            } else {
                log.warn("令牌使用失败: token={}", token);
                return ApiResponse.error("令牌无效或已被使用");
            }
        } catch (Exception e) {
            log.error("令牌使用异常: token={}", token, e);
            return ApiResponse.error("令牌使用异常: " + e.getMessage());
        }
    }

    /**
     * 刷新令牌有效期
     */
    @PostMapping("/refresh")
    public ApiResponse<Map<String, Object>> refreshTokenTTL(@RequestParam String token, 
                                                           @RequestParam Long ttlSeconds) {
        try {
            boolean refreshed = tokenService.refreshTokenTTL(token, ttlSeconds);
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("refreshed", refreshed);
            response.put("newTtl", ttlSeconds);
            
            if (refreshed) {
                log.info("刷新令牌TTL成功: token={}, ttl={}s", token, ttlSeconds);
                return ApiResponse.success(response);
            } else {
                log.warn("刷新令牌TTL失败: token={}", token);
                return ApiResponse.error("令牌不存在或已过期");
            }
        } catch (Exception e) {
            log.error("刷新令牌TTL异常: token={}", token, e);
            return ApiResponse.error("刷新令牌TTL异常: " + e.getMessage());
        }
    }

    /**
     * 获取令牌统计信息
     */
    @GetMapping("/statistics/{activityId}")
    public ApiResponse<TokenService.TokenStatistics> getTokenStatistics(@PathVariable Long activityId) {
        try {
            TokenService.TokenStatistics statistics = tokenService.getTokenStatistics(activityId);
            log.info("获取令牌统计信息成功: activityId={}, statistics={}", activityId, statistics);
            return ApiResponse.success(statistics);
        } catch (Exception e) {
            log.error("获取令牌统计信息失败: activityId={}", activityId, e);
            return ApiResponse.error("获取令牌统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取令牌剩余有效期
     */
    @GetMapping("/ttl")
    public ApiResponse<Map<String, Object>> getTokenTTL(@RequestParam String token) {
        try {
            long ttl = tokenService.getTokenTTL(token);
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("ttl", ttl);
            response.put("ttlDescription", getTtlDescription(ttl));
            
            log.info("获取令牌TTL成功: token={}, ttl={}s", token, ttl);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取令牌TTL异常: token={}", token, e);
            return ApiResponse.error("获取令牌TTL异常: " + e.getMessage());
        }
    }

    /**
     * 获取TTL描述
     */
    private String getTtlDescription(long ttl) {
        if (ttl == -1) {
            return "令牌不存在";
        } else if (ttl == -2) {
            return "令牌永久有效";
        } else if (ttl < 0) {
            return "令牌已过期";
        } else {
            return String.format("剩余%d秒", ttl);
        }
    }
} 