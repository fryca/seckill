package yw.seckill.service;

public interface SeckillQualifyService {
    /**
     * 校验用户是否有资格参与秒杀
     */
    boolean checkSeckillQualification(Long userId, Long activityId, Long timestamp, String signature);
} 