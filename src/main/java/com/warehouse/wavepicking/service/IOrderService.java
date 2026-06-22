package com.warehouse.wavepicking.service;

import com.warehouse.wavepicking.dto.request.CreateOrderRequest;
import com.warehouse.wavepicking.dto.response.OrderResponse;
import com.warehouse.wavepicking.entity.Order;

import java.util.List;

public interface IOrderService {

    List<OrderResponse> getAllOrders();

    List<OrderResponse> getOrdersByStatus(Order.OrderStatus status);

    List<OrderResponse> getPendingOrders();

    OrderResponse getOrderById(Long id);

    OrderResponse getOrderByOrderNo(String orderNo);

    OrderResponse createOrder(CreateOrderRequest request);

    OrderResponse confirmOrder(Long id);

    OrderResponse updateStatus(Long id, Order.OrderStatus targetStatus);

    OrderResponse cancelOrder(Long id);

    void allocateOrderToWave(Order order, com.warehouse.wavepicking.entity.Wave wave);

    void rollbackOrderFromWave(Order order);
}
