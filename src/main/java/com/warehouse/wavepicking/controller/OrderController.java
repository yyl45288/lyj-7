package com.warehouse.wavepicking.controller;

import com.warehouse.wavepicking.common.ApiResponse;
import com.warehouse.wavepicking.dto.request.CreateOrderRequest;
import com.warehouse.wavepicking.dto.request.UpdateOrderStatusRequest;
import com.warehouse.wavepicking.dto.response.OrderResponse;
import com.warehouse.wavepicking.entity.Order;
import com.warehouse.wavepicking.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ApiResponse<List<OrderResponse>> getAllOrders() {
        return ApiResponse.success(orderService.getAllOrders());
    }

    @GetMapping("/status/{status}")
    public ApiResponse<List<OrderResponse>> getOrdersByStatus(@PathVariable String status) {
        Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
        return ApiResponse.success(orderService.getOrdersByStatus(orderStatus));
    }

    @GetMapping("/pending")
    public ApiResponse<List<OrderResponse>> getPendingOrders() {
        return ApiResponse.success(orderService.getPendingOrders());
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getOrderById(@PathVariable Long id) {
        return ApiResponse.success(orderService.getOrderById(id));
    }

    @GetMapping("/no/{orderNo}")
    public ApiResponse<OrderResponse> getOrderByOrderNo(@PathVariable String orderNo) {
        return ApiResponse.success(orderService.getOrderByOrderNo(orderNo));
    }

    @PostMapping
    public ApiResponse<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.success(orderService.createOrder(request));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<OrderResponse> updateOrderStatus(@PathVariable Long id,
                                                         @Valid @RequestBody UpdateOrderStatusRequest request) {
        Order.OrderStatus status = Order.OrderStatus.valueOf(request.getStatus().toUpperCase());
        return ApiResponse.success(orderService.updateStatus(id, status));
    }

    @PutMapping("/{id}/confirm")
    public ApiResponse<OrderResponse> confirmOrder(@PathVariable Long id) {
        return ApiResponse.success(orderService.confirmOrder(id));
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<OrderResponse> cancelOrder(@PathVariable Long id) {
        return ApiResponse.success(orderService.cancelOrder(id));
    }
}
