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

### 分布式锁监控接口

#### 1. 测试基本锁功能
```bash
curl -X POST "http://localhost:8080/api/lock-monitor/test-lock?lockKey=test-lock-1&waitTime=5&leaseTime=10"
```

#### 2. 测试看门狗机制（自动续期）
```bash
curl -X POST "http://localhost:8080/api/lock-monitor/test-watchdog?lockKey=test-watchdog-1&waitTime=5"
```

#### 3. 并发测试锁监控
```bash
curl -X POST "http://localhost:8080/api/lock-monitor/test-concurrent?lockKey=test-concurrent-1&threadCount=5"
```

#### 4. 获取锁竞争统计
```bash
curl -X GET "http://localhost:8080/api/lock-monitor/competition/test-lock-1"
```

#### 5. 获取所有锁统计信息
```bash
curl -X GET "http://localhost:8080/api/lock-monitor/statistics"
```

#### 6. 获取锁信息
```bash
curl -X GET "http://localhost:8080/api/lock-monitor/lock-info/test-lock-1"
```

#### 7. 强制释放锁
```bash
curl -X POST "http://localhost:8080/api/lock-monitor/force-unlock/test-lock-1"
```

#### 8. 清理过期统计信息
```bash
curl -X POST "http://localhost:8080/api/lock-monitor/cleanup"
```

