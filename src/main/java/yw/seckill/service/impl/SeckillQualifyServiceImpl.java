package yw.seckill.service.impl;

import org.springframework.stereotype.Service;
import yw.seckill.service.SeckillQualifyService;

@Service
public class SeckillQualifyServiceImpl implements SeckillQualifyService {
    @Override
    public boolean checkSeckillQualification(Long userId, Long activityId, Long timestamp, String signature) {
        // 1. 校验时间戳，防止重放
        long now = System.currentTimeMillis();
        long expireMillis = 5 * 60 * 1000; // 5分钟有效
        if (Math.abs(now - timestamp) > expireMillis) {
            return false;
        }
        // 2. 校验签名（伪代码，实际应用密钥+参数生成签名）
        String secret = "seckillSecretKey"; // TODO: 放到配置
        String raw = userId + ":" + activityId + ":" + timestamp + ":" + secret;
        String expectedSign = org.springframework.util.DigestUtils.md5DigestAsHex(raw.getBytes());
        if (!expectedSign.equals(signature)) {
            return false;
        }
        // 3. TODO: 可加分布式缓存/Redis防止同一用户重复请求
        // 4. TODO: 其他资格校验（如用户黑名单、活动状态等）
        return true;
    }
} 