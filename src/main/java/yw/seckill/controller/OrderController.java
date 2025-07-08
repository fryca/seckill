package yw.seckill.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import yw.seckill.config.ApiResponse;
import yw.seckill.entity.Order;
import yw.seckill.service.OrderService;
import yw.seckill.dto.CreateOrderRequestDTO;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/seckill/order")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @GetMapping("/{orderId}")
    public ApiResponse<Order> getOrderById(@PathVariable Long orderId) {
        try {
            Order order = orderService.getOrderById(orderId);
            return ApiResponse.success(order);
        } catch (Exception e) {
            throw e; // 交由全局异常处理
        }
    }

    @PostMapping("")
    public ApiResponse<String> createOrder(@RequestBody @Validated CreateOrderRequestDTO request) {
        try {
            orderService.createOrderAsync(request);
            return ApiResponse.success("下单请求已受理");
        } catch (Exception e) {
            throw e;
        }
    }
} 