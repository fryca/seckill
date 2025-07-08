package yw.seckill.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import yw.seckill.entity.Order;
import yw.seckill.mapper.OrderMapper;
import yw.seckill.service.OrderService;
import yw.seckill.dto.CreateOrderRequestDTO;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;

    @Override
    public Order getOrderById(Long orderId) {
        return orderMapper.selectById(orderId);
    }

    @Override
    public void createOrderAsync(CreateOrderRequestDTO request) {
        // TODO: 校验资格、库存、幂等性、Redisson分布式锁
        // TODO: 投递Kafka消息，异步下单
        // TODO: 记录日志
    }
} 