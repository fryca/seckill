package yw.seckill.dto;

public record QualifyRequestDTO(Long userId, Long activityId, Long timestamp) {
    public QualifyRequestDTO {
        if (userId == null || activityId == null || timestamp == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
    }
} 