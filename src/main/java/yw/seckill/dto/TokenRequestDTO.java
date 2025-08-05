package yw.seckill.dto;

import lombok.Data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 令牌请求DTO
 */
@Data
public class TokenRequestDTO {

    /**
     * 活动ID
     */
    @NotNull(message = "活动ID不能为空")
    private Long activityId;

    /**
     * 生成数量（批量生成时使用）
     */
    @Min(value = 1, message = "生成数量必须大于0")
    private Integer count = 1;

    /**
     * 令牌有效期（秒）
     */
    @Min(value = 1, message = "有效期必须大于0")
    private Long ttlSeconds = 3600L; // 默认1小时
} 