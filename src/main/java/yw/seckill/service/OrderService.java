package yw.seckill.service;

import yw.seckill.entity.Order;
import yw.seckill.dto.CreateOrderRequestDTO;

public interface OrderService {
    Order getOrderById(Long orderId);
    void createOrderAsync(CreateOrderRequestDTO request);
} 