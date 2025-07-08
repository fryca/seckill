package yw.seckill.service;

import yw.seckill.entity.Order;
import yw.seckill.dto.CreateOrderRequestDTO;

public interface OrderService {
    // 示例：可根据业务需求扩展
    Order getOrderById(Long orderId);
    void createOrderAsync(CreateOrderRequestDTO request);
} 