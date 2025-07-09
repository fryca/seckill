package yw.seckill.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import yw.seckill.config.KafkaConfig;
import yw.seckill.dto.CreateOrderRequestDTO;
import yw.seckill.entity.Order;
import yw.seckill.mapper.OrderMapper;

import java.time.LocalDateTime;

@Service
public class OrderConsumerService {
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private OrderMapper orderMapper;

    @KafkaListener(topics = KafkaConfig.ORDER_TOPIC, groupId = "seckill-order-group")
    public void handleOrderMessage(String message, Acknowledgment ack) {
        try {
            System.out.println("[Kafka] 收到下单消息: " + message);
            
            // 1. 反序列化消息
            CreateOrderRequestDTO request = objectMapper.readValue(message, CreateOrderRequestDTO.class);
            
            // 2. 创建订单实体
            Order order = new Order();
            order.setUserId(request.userId());
            order.setActivityId(request.activityId());
            order.setProductId(request.productId());
            order.setStatus(0); // 0: 待支付
            order.setCreateTime(LocalDateTime.now());
            
            // 3. 保存订单到数据库
            orderMapper.insert(order);
            
            // 4. 记录日志
            System.out.println("[Kafka] 订单创建成功，订单ID: " + order.getId());
            
            // 5. 手动提交偏移量
            ack.acknowledge();
            
        } catch (JsonProcessingException e) {
            System.err.println("[Kafka] 消息反序列化失败: " + e.getMessage());
            throw new RuntimeException("消息反序列化失败", e);
        } catch (Exception e) {
            System.err.println("[Kafka] 处理下单消息失败: " + e.getMessage());
            // 异常时不提交偏移量，消息会被重新消费
            throw e;
        }
    }
} 