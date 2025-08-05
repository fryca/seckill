package yw.seckill.dto;

import lombok.Data;

import java.util.List;

/**
 * 令牌响应DTO
 */
@Data
public class TokenResponseDTO {

    /**
     * 活动ID
     */
    private Long activityId;

    /**
     * 生成的令牌列表
     */
    private List<String> tokens;

    /**
     * 令牌有效期（秒）
     */
    private Long ttlSeconds;

    /**
     * 生成数量
     */
    private Integer count;

    /**
     * 生成时间戳
     */
    private Long generateTime;

    public TokenResponseDTO() {
        this.generateTime = System.currentTimeMillis();
    }
} 