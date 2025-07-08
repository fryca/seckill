package yw.seckill.dto;

public record CreateOrderRequestDTO(Long userId, Long activityId, Long productId, Long timestamp) {
    public CreateOrderRequestDTO {
        if (userId == null || activityId == null || productId == null || timestamp == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
    }
} 