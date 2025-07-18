# 秒杀系统

一个基于Spring Boot + MyBatis-Plus + Kafka + Redis的秒杀系统。

## 技术栈

- Spring Boot 3.3.13
- MyBatis-Plus 3.5.12
- MySQL
- Redis (Redisson)
- Kafka
- Maven

### 秒杀活动接口

1. **获取活动列表**
```bash
curl http://localhost:8080/seckill/activity/list
```

2. **获取指定活动**
```bash
curl http://localhost:8080/seckill/activity/1
```

3. **活动服务测试**
```bash
curl -X POST http://localhost:8080/seckill/activity/test
```

### 订单接口

1. **获取订单**
```bash
curl http://localhost:8080/seckill/order/1
```

2. **创建订单**
```bash
curl -X POST http://localhost:8080/seckill/order \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "activityId": 1,
    "productId": 1
  }'
```

### 资格检查接口

```bash
curl -X POST "http://localhost:8080/seckill/qualify?signature=test123" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "activityId": 1,
    "timestamp": 1640995200000
  }'
```

