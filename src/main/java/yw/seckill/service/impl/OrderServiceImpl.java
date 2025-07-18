package yw.seckill.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import yw.seckill.entity.Order;
import yw.seckill.mapper.OrderMapper;
import yw.seckill.service.OrderService;
import yw.seckill.dto.CreateOrderRequestDTO;
import org.springframework.kafka.core.KafkaTemplate;
import yw.seckill.config.KafkaConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Order getOrderById(Long orderId) {
        return orderMapper.selectById(orderId);
    }

    @Override
    public void createOrderAsync(CreateOrderRequestDTO request) {
        try {
            // TODO:
            // 参数校验、资格校验
            // 序列化消息体为JSON
            String msg = objectMapper.writeValueAsString(request);
            // 投递Kafka消息
            kafkaTemplate.send(KafkaConfig.ORDER_TOPIC, msg);
            // 记录日志
            System.out.println("[Kafka] 投递下单消息: " + msg);
        } catch (Exception e) {
            // 记录异常日志
            System.err.println("[Kafka] 投递下单消息失败: " + e.getMessage());
            throw new RuntimeException("下单消息投递失败", e);
        }
    }
} 