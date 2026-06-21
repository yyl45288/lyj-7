package com.warehouse.wavepicking.service;

import com.warehouse.wavepicking.dto.request.CreateWaveRequest;
import com.warehouse.wavepicking.dto.response.OrderSummaryResponse;
import com.warehouse.wavepicking.dto.response.PickingTaskResponse;
import com.warehouse.wavepicking.dto.response.WaveResponse;
import com.warehouse.wavepicking.entity.*;
import com.warehouse.wavepicking.exception.BusinessException;
import com.warehouse.wavepicking.repository.OrderItemRepository;
import com.warehouse.wavepicking.repository.OrderRepository;
import com.warehouse.wavepicking.repository.WaveRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class WaveService {

    private final WaveRepository waveRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryService inventoryService;
    private final OrderService orderService;
    private final PickingTaskService pickingTaskService;

    public WaveService(WaveRepository waveRepository, OrderRepository orderRepository, OrderItemRepository orderItemRepository, InventoryService inventoryService, OrderService orderService, PickingTaskService pickingTaskService) {
        this.waveRepository = waveRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.inventoryService = inventoryService;
        this.orderService = orderService;
        this.pickingTaskService = pickingTaskService;
    }

    private static final AtomicInteger waveCounter = new AtomicInteger(0);

    public List<WaveResponse> getAllWaves() {
        return waveRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<WaveResponse> getActiveWaves() {
        return waveRepository.findActiveWaves().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<WaveResponse> getWavesByStatus(Wave.WaveStatus status) {
        return waveRepository.findByStatus(status).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public WaveResponse getWaveById(Long id) {
        Wave wave = waveRepository.findById(id)
                .orElseThrow(() -> new BusinessException("WAVE_NOT_FOUND", "波次不存在: " + id));
        return convertToResponse(wave);
    }

    public WaveResponse getWaveByWaveNo(String waveNo) {
        Wave wave = waveRepository.findByWaveNo(waveNo)
                .orElseThrow(() -> new BusinessException("WAVE_NOT_FOUND", "波次不存在: " + waveNo));
        return convertToResponse(wave);
    }

    @Transactional
    public WaveResponse createWave(CreateWaveRequest request) {
        List<Order> orders;

        if (request.getOrderIds() != null && !request.getOrderIds().isEmpty()) {
            orders = orderRepository.findAllById(request.getOrderIds()).stream()
                    .filter(o -> o.getStatus() == Order.OrderStatus.CONFIRMED && o.getWave() == null)
                    .collect(Collectors.toList());
        } else {
            int maxCount = request.getMaxOrderCount() != null ? request.getMaxOrderCount() : 10;

            List<Order> urgentOrders = orderRepository.findUrgentPendingOrdersWithoutWave(Order.OrderStatus.CONFIRMED);
            List<Order> normalOrders = orderRepository.findPendingOrdersWithoutWave(Order.OrderStatus.CONFIRMED);

            orders = new ArrayList<>();
            orders.addAll(urgentOrders);
            orders.addAll(normalOrders);

            if (orders.size() > maxCount) {
                orders = orders.subList(0, maxCount);
            }
        }

        if (orders.isEmpty()) {
            throw new BusinessException("NO_ORDERS", "没有可分配的订单");
        }

        Wave.WaveType waveType = Wave.WaveType.valueOf(request.getWaveType() != null ? request.getWaveType() : "NORMAL");

        Wave wave = new Wave();
        wave.setWaveNo(generateWaveNo());
        wave.setStatus(Wave.WaveStatus.NEW);
        wave.setWaveType(waveType);
        wave.setZone(request.getZone());
        wave.setTotalOrderCount(orders.size());

        Set<Long> skuIds = new HashSet<>();
        int totalQuantity = 0;
        for (Order order : orders) {
            for (OrderItem item : order.getItems()) {
                skuIds.add(item.getSku().getId());
                totalQuantity += item.getQuantity();
            }
        }
        wave.setTotalSkuCount(skuIds.size());
        wave.setTotalQuantity(totalQuantity);

        Wave savedWave = waveRepository.save(wave);

        for (Order order : orders) {
            orderService.allocateOrderToWave(order, savedWave);
        }

        return convertToResponse(savedWave);
    }

    @Transactional
    public WaveResponse releaseWave(Long id) {
        Wave wave = waveRepository.findById(id)
                .orElseThrow(() -> new BusinessException("WAVE_NOT_FOUND", "波次不存在: " + id));

        if (wave.getStatus() != Wave.WaveStatus.NEW) {
            throw new BusinessException("INVALID_STATUS", "只有新建状态的波次才能释放");
        }

        List<Order> orders = orderRepository.findByWaveId(wave.getId());
        if (orders.isEmpty()) {
            throw new BusinessException("NO_ORDERS", "波次中没有订单");
        }

        Map<Long, Integer> skuQuantityMap = new HashMap<>();
        for (Order order : orders) {
            for (OrderItem item : order.getItems()) {
                skuQuantityMap.merge(item.getSku().getId(), item.getQuantity(), Integer::sum);
            }
        }

        for (Map.Entry<Long, Integer> entry : skuQuantityMap.entrySet()) {
            inventoryService.lockStock(entry.getKey(), entry.getValue());
        }

        wave.setStatus(Wave.WaveStatus.RELEASED);
        wave.setReleasedAt(LocalDateTime.now());
        waveRepository.save(wave);

        pickingTaskService.generatePickingTasks(wave, orders);

        for (Order order : orders) {
            orderService.updateStatus(order.getId(), Order.OrderStatus.PICKING);
        }

        wave.setStatus(Wave.WaveStatus.PICKING);
        waveRepository.save(wave);

        return convertToResponse(wave);
    }

    @Transactional
    public WaveResponse rollbackWave(Long id) {
        Wave wave = waveRepository.findById(id)
                .orElseThrow(() -> new BusinessException("WAVE_NOT_FOUND", "波次不存在: " + id));

        if (wave.getStatus() != Wave.WaveStatus.RELEASED && wave.getStatus() != Wave.WaveStatus.PICKING) {
            throw new BusinessException("INVALID_STATUS", "只有已释放或拣货中的波次才能回滚");
        }

        List<Order> orders = orderRepository.findByWaveId(wave.getId());

        Map<Long, Integer> skuQuantityMap = new HashMap<>();
        for (Order order : orders) {
            for (OrderItem item : order.getItems()) {
                int remainingQty = item.getQuantity() - (item.getPickedQuantity() != null ? item.getPickedQuantity() : 0);
                if (remainingQty > 0) {
                    skuQuantityMap.merge(item.getSku().getId(), remainingQty, Integer::sum);
                }
            }
        }

        for (Map.Entry<Long, Integer> entry : skuQuantityMap.entrySet()) {
            inventoryService.unlockStock(entry.getKey(), entry.getValue());
        }

        pickingTaskService.cancelTasksByWave(wave.getId());

        for (Order order : orders) {
            orderService.rollbackOrderFromWave(order);
        }

        wave.setStatus(Wave.WaveStatus.CANCELLED);
        waveRepository.save(wave);

        return convertToResponse(wave);
    }

    @Transactional
    public WaveResponse completeWave(Long id) {
        Wave wave = waveRepository.findById(id)
                .orElseThrow(() -> new BusinessException("WAVE_NOT_FOUND", "波次不存在: " + id));

        if (wave.getStatus() != Wave.WaveStatus.PICKING) {
            throw new BusinessException("INVALID_STATUS", "只有拣货中的波次才能完成");
        }

        long completedTasks = pickingTaskService.countCompletedTasksByWave(wave.getId());
        long totalTasks = pickingTaskService.countTotalTasksByWave(wave.getId());

        if (completedTasks < totalTasks) {
            throw new BusinessException("TASKS_NOT_COMPLETED",
                    "拣货任务未全部完成，已完成: " + completedTasks + "，总计: " + totalTasks);
        }

        List<Order> orders = orderRepository.findByWaveId(wave.getId());
        for (Order order : orders) {
            orderService.updateStatus(order.getId(), Order.OrderStatus.PICKED);
        }

        wave.setStatus(Wave.WaveStatus.COMPLETED);
        wave.setCompletedAt(LocalDateTime.now());
        waveRepository.save(wave);

        return convertToResponse(wave);
    }

    @Transactional
    public WaveResponse addUrgentOrderToWave(Long waveId, Long orderId) {
        Wave wave = waveRepository.findById(waveId)
                .orElseThrow(() -> new BusinessException("WAVE_NOT_FOUND", "波次不存在: " + waveId));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "订单不存在: " + orderId));

        if (order.getStatus() != Order.OrderStatus.CONFIRMED || order.getWave() != null) {
            throw new BusinessException("INVALID_ORDER", "订单状态不满足紧急插单条件");
        }

        if (!Boolean.TRUE.equals(order.getUrgent())) {
            throw new BusinessException("NOT_URGENT", "只有紧急订单才能插入正在进行的波次");
        }

        if (wave.getStatus() == Wave.WaveStatus.COMPLETED || wave.getStatus() == Wave.WaveStatus.CANCELLED) {
            throw new BusinessException("INVALID_STATUS", "波次已完成或已取消，无法插入订单");
        }

        for (OrderItem item : order.getItems()) {
            inventoryService.lockStock(item.getSku().getId(), item.getQuantity());
        }

        orderService.allocateOrderToWave(order, wave);
        orderService.updateStatus(orderId, Order.OrderStatus.PICKING);

        pickingTaskService.generatePickingTasksForOrder(wave, order);

        wave.setTotalOrderCount(wave.getTotalOrderCount() + 1);
        wave.setTotalQuantity(wave.getTotalQuantity() +
                order.getItems().stream().mapToInt(OrderItem::getQuantity).sum());
        waveRepository.save(wave);

        return convertToResponse(wave);
    }

    private String generateWaveNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int seq = waveCounter.incrementAndGet();
        return "WAVE" + date + String.format("%06d", seq);
    }

    private WaveResponse convertToResponse(Wave wave) {
        List<OrderSummaryResponse> orderSummaries = orderRepository.findByWaveId(wave.getId()).stream()
                .map(order -> OrderSummaryResponse.builder()
                        .id(order.getId())
                        .orderNo(order.getOrderNo())
                        .customerName(order.getCustomerName())
                        .status(order.getStatus().name())
                        .itemCount(order.getItems().size())
                        .totalQuantity(order.getItems().stream().mapToInt(OrderItem::getQuantity).sum())
                        .build())
                .collect(Collectors.toList());

        return WaveResponse.builder()
                .id(wave.getId())
                .waveNo(wave.getWaveNo())
                .status(wave.getStatus().name())
                .waveType(wave.getWaveType().name())
                .totalOrderCount(wave.getTotalOrderCount())
                .totalSkuCount(wave.getTotalSkuCount())
                .totalQuantity(wave.getTotalQuantity())
                .zone(wave.getZone())
                .operator(wave.getOperator())
                .orders(orderSummaries)
                .createdAt(wave.getCreatedAt())
                .updatedAt(wave.getUpdatedAt())
                .releasedAt(wave.getReleasedAt())
                .completedAt(wave.getCompletedAt())
                .build();
    }
}
