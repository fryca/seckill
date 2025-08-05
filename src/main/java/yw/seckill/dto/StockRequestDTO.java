package yw.seckill.dto;

import lombok.Data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 库存请求DTO
 */
@Data
public class StockRequestDTO {

    /**
     * 商品ID
     */
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /**
     * 数量
     */
    @Min(value = 1, message = "数量必须大于0")
    private Integer quantity;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 用户ID
     */
    private Long userId;
}

/**
 * 批量库存请求DTO
 */
@Data
class BatchStockRequestDTO {

    /**
     * 库存请求列表
     */
    @NotNull(message = "库存请求列表不能为空")
    private List<StockRequestDTO> requests;
} 