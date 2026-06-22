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
import com.warehouse.wavepicking.statemachine.OrderStateMachine;
import com.warehouse.wavepicking.statemachine.StateTransitionResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class OrderService implements IOrderService {

    private final OrderRepository orderRepository;
    private final SkuRepository skuRepository;
    private final IInventoryLockService inventoryLockService;

    public OrderService(OrderRepository orderRepository,
                        SkuRepository skuRepository,
                        IInventoryLockService inventoryLockService) {
        this.orderRepository = orderRepository;
        this.skuRepository = skuRepository;
        this.inventoryLockService = inventoryLockService;
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
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("INVALID_ORDER_ITEMS", "订单项不能为空");
        }

        int rowIndex = 1;
        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            if (itemRequest.getSkuId() == null) {
                throw new BusinessException("INVALID_ORDER_ITEMS",
                        String.format("第 %d 行：SKU不能为空", rowIndex));
            }
            if (itemRequest.getQuantity() == null) {
                throw new BusinessException("INVALID_ORDER_ITEMS",
                        String.format("第 %d 行：数量不能为空", rowIndex));
            }
            if (itemRequest.getQuantity() <= 0) {
                throw new BusinessException("INVALID_ORDER_ITEMS",
                        String.format("第 %d 行：数量必须大于0", rowIndex));
            }
            rowIndex++;
        }

        String orderNo = generateOrderNo();

        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setCustomerName(request.getCustomerName());
        order.setAddress(request.getAddress());
        order.setPhone(request.getPhone());
        order.setUrgent(request.getUrgent() != null && request.getUrgent());
        order.setRemark(request.getRemark());
        order.setStatus(Order.OrderStatus.PENDING);

        rowIndex = 1;
        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            final int currentRow = rowIndex;
            Sku sku = skuRepository.findById(itemRequest.getSkuId())
                    .orElseThrow(() -> new BusinessException("SKU_NOT_FOUND",
                            String.format("第 %d 行：SKU不存在: %s", currentRow, itemRequest.getSkuId())));

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setSku(sku);
            item.setQuantity(itemRequest.getQuantity());
            item.setPickedQuantity(0);
            item.setLocation(sku.getLocation());
            order.getItems().add(item);
            rowIndex++;
        }

        Order saved = orderRepository.save(order);

        inventoryLockService.lockStockForOrder(saved);

        saved.setStatus(Order.OrderStatus.CONFIRMED);
        Order confirmed = orderRepository.save(saved);

        return convertToResponse(confirmed);
    }

    @Transactional
    public OrderResponse confirmOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "订单不存在: " + id));

        if (order.getStatus() == Order.OrderStatus.CONFIRMED) {
            return convertToResponse(order);
        }

        StateTransitionResult result = OrderStateMachine.canConfirm(order.getStatus());
        throwIfNotAllowed(result);

        inventoryLockService.lockStockForOrder(order);

        order.setStatus(Order.OrderStatus.CONFIRMED);
        Order saved = orderRepository.save(order);
        return convertToResponse(saved);
    }

    @Transactional
    public OrderResponse updateStatus(Long id, Order.OrderStatus targetStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "订单不存在: " + id));

        StateTransitionResult result = OrderStateMachine.canTransition(order.getStatus(), targetStatus);
        throwIfNotAllowed(result);

        order.setStatus(targetStatus);

        if (targetStatus == Order.OrderStatus.SHIPPED) {
            order.setCompletedAt(LocalDateTime.now());
            inventoryLockService.deductStockForShippedOrder(order);
        }

        Order saved = orderRepository.save(order);
        return convertToResponse(saved);
    }

    @Transactional
    public OrderResponse cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "订单不存在: " + id));

        boolean hasWave = order.getWave() != null;
        StateTransitionResult result = OrderStateMachine.canCancel(order.getStatus(), hasWave);
        throwIfNotAllowed(result);

        inventoryLockService.releaseStockForCancelledOrder(order);

        order.setStatus(Order.OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        return convertToResponse(saved);
    }

    @Transactional
    public void allocateOrderToWave(Order order, com.warehouse.wavepicking.entity.Wave wave) {
        StateTransitionResult result = OrderStateMachine.canAllocateToWave(order.getStatus());
        throwIfNotAllowed(result);
        order.setWave(wave);
        order.setStatus(Order.OrderStatus.ALLOCATED);
        orderRepository.save(order);
    }

    @Transactional
    public void rollbackOrderFromWave(Order order) {
        StateTransitionResult result = OrderStateMachine.canRollbackFromWave(order.getStatus());
        throwIfNotAllowed(result);
        order.setWave(null);
        order.setStatus(Order.OrderStatus.CONFIRMED);
        orderRepository.save(order);
    }

    private void throwIfNotAllowed(StateTransitionResult result) {
        if (!result.isAllowed()) {
            throw new BusinessException(result.getErrorCode(), result.getErrorMessage());
        }
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
