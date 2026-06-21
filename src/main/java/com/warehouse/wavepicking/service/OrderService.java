package com.warehouse.wavepicking.service;

import com.warehouse.wavepicking.dto.request.CreateOrderRequest;
import com.warehouse.wavepicking.dto.response.OrderItemResponse;
import com.warehouse.wavepicking.dto.response.OrderResponse;
import com.warehouse.wavepicking.entity.Order;
import com.warehouse.wavepicking.entity.OrderItem;
import com.warehouse.wavepicking.entity.Sku;
import com.warehouse.wavepicking.exception.BusinessException;
import com.warehouse.wavepicking.repository.OrderRepository;
import com.warehouse.wavepicking.repository.SkuRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final SkuRepository skuRepository;
    private final InventoryService inventoryService;

    public OrderService(OrderRepository orderRepository, SkuRepository skuRepository, InventoryService inventoryService) {
        this.orderRepository = orderRepository;
        this.skuRepository = skuRepository;
        this.inventoryService = inventoryService;
    }

    private static final AtomicInteger orderCounter = new AtomicInteger(0);

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<OrderResponse> getOrdersByStatus(Order.OrderStatus status) {
        return orderRepository.findByStatus(status).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<OrderResponse> getPendingOrders() {
        return orderRepository.findPendingOrdersWithoutWave(Order.OrderStatus.CONFIRMED).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "订单不存在: " + id));
        return convertToResponse(order);
    }

    public OrderResponse getOrderByOrderNo(String orderNo) {
        Order order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "订单不存在: " + orderNo));
        return convertToResponse(order);
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        String orderNo = generateOrderNo();

        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setCustomerName(request.getCustomerName());
        order.setAddress(request.getAddress());
        order.setPhone(request.getPhone());
        order.setUrgent(request.getUrgent() != null && request.getUrgent());
        order.setRemark(request.getRemark());
        order.setStatus(Order.OrderStatus.PENDING);

        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            Sku sku = skuRepository.findById(itemRequest.getSkuId())
                    .orElseThrow(() -> new BusinessException("SKU_NOT_FOUND", "SKU不存在: " + itemRequest.getSkuId()));

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setSku(sku);
            item.setQuantity(itemRequest.getQuantity());
            item.setPickedQuantity(0);
            item.setLocation(sku.getLocation());
            order.getItems().add(item);
        }

        Order saved = orderRepository.save(order);
        return convertToResponse(saved);
    }

    @Transactional
    public OrderResponse confirmOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "订单不存在: " + id));

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new BusinessException("INVALID_STATUS", "只有待确认订单才能确认");
        }

        for (OrderItem item : order.getItems()) {
            if (!inventoryService.hasEnoughStock(item.getSku().getId(), item.getQuantity())) {
                throw new BusinessException("INSUFFICIENT_STOCK",
                        "库存不足，无法确认订单。SKU: " + item.getSku().getSkuCode());
            }
        }

        order.setStatus(Order.OrderStatus.CONFIRMED);
        Order saved = orderRepository.save(order);
        return convertToResponse(saved);
    }

    @Transactional
    public OrderResponse updateStatus(Long id, Order.OrderStatus status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "订单不存在: " + id));

        order.setStatus(status);

        if (status == Order.OrderStatus.SHIPPED) {
            order.setCompletedAt(LocalDateTime.now());
        }

        Order saved = orderRepository.save(order);
        return convertToResponse(saved);
    }

    @Transactional
    public OrderResponse cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "订单不存在: " + id));

        if (order.getStatus() == Order.OrderStatus.ALLOCATED ||
                order.getStatus() == Order.OrderStatus.PICKING) {
            throw new BusinessException("CANNOT_CANCEL", "订单已分配波次，无法取消，请先回滚波次");
        }

        if (order.getWave() != null) {
            throw new BusinessException("CANNOT_CANCEL", "订单已在波次中，无法取消");
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        return convertToResponse(saved);
    }

    @Transactional
    public void allocateOrderToWave(Order order, com.warehouse.wavepicking.entity.Wave wave) {
        order.setWave(wave);
        order.setStatus(Order.OrderStatus.ALLOCATED);
        orderRepository.save(order);
    }

    @Transactional
    public void rollbackOrderFromWave(Order order) {
        order.setWave(null);
        order.setStatus(Order.OrderStatus.CONFIRMED);
        orderRepository.save(order);
    }

    private String generateOrderNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int seq = orderCounter.incrementAndGet();
        return "ORD" + date + String.format("%06d", seq);
    }

    private OrderResponse convertToResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(this::convertItemToResponse)
                .collect(Collectors.toList());

        int totalQty = order.getItems().stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();

        return OrderResponse.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .customerName(order.getCustomerName())
                .address(order.getAddress())
                .phone(order.getPhone())
                .status(order.getStatus().name())
                .urgent(order.getUrgent())
                .waveId(order.getWave() != null ? order.getWave().getId() : null)
                .waveNo(order.getWave() != null ? order.getWave().getWaveNo() : null)
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .completedAt(order.getCompletedAt())
                .remark(order.getRemark())
                .totalQuantity(totalQty)
                .build();
    }

    private OrderItemResponse convertItemToResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .skuId(item.getSku().getId())
                .skuCode(item.getSku().getSkuCode())
                .skuName(item.getSku().getSkuName())
                .quantity(item.getQuantity())
                .pickedQuantity(item.getPickedQuantity())
                .location(item.getLocation())
                .build();
    }
}
