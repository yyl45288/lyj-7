package com.warehouse.wavepicking.service;

import com.warehouse.wavepicking.dto.response.DashboardResponse;
import com.warehouse.wavepicking.dto.response.InventoryResponse;
import com.warehouse.wavepicking.dto.response.OrderSummaryResponse;
import com.warehouse.wavepicking.entity.Order;
import com.warehouse.wavepicking.entity.OrderItem;
import com.warehouse.wavepicking.entity.PickingTask;
import com.warehouse.wavepicking.entity.Wave;
import com.warehouse.wavepicking.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final OrderRepository orderRepository;
    private final WaveRepository waveRepository;
    private final PickingTaskRepository pickingTaskRepository;
    private final InventoryRepository inventoryRepository;
    private final SkuRepository skuRepository;
    private final IInventoryService inventoryService;

    public DashboardService(OrderRepository orderRepository, WaveRepository waveRepository, PickingTaskRepository pickingTaskRepository, InventoryRepository inventoryRepository, SkuRepository skuRepository, IInventoryService inventoryService) {
        this.orderRepository = orderRepository;
        this.waveRepository = waveRepository;
        this.pickingTaskRepository = pickingTaskRepository;
        this.inventoryRepository = inventoryRepository;
        this.skuRepository = skuRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboardData() {
        long pendingOrderCount = orderRepository.countByStatus(Order.OrderStatus.CONFIRMED);
        long totalOrderCount = orderRepository.count();
        long activeWaveCount = waveRepository.findActiveWaves().size();
        long completedWaveCount = waveRepository.countByStatus(Wave.WaveStatus.COMPLETED);
        long pendingTaskCount = pickingTaskRepository.countByStatus(PickingTask.TaskStatus.PENDING)
                + pickingTaskRepository.countByStatus(PickingTask.TaskStatus.ASSIGNED);
        long completedTaskCount = pickingTaskRepository.countByStatus(PickingTask.TaskStatus.COMPLETED);
        long outOfStockSkuCount = inventoryRepository.findOutOfStockItems().size();
        long totalSkuCount = skuRepository.count();

        DashboardResponse.DashboardStats stats = DashboardResponse.DashboardStats.builder()
                .pendingOrderCount(pendingOrderCount)
                .totalOrderCount(totalOrderCount)
                .activeWaveCount(activeWaveCount)
                .completedWaveCount(completedWaveCount)
                .pendingTaskCount(pendingTaskCount)
                .completedTaskCount(completedTaskCount)
                .outOfStockSkuCount(outOfStockSkuCount)
                .totalSkuCount(totalSkuCount)
                .build();

        List<Order> pendingOrders = orderRepository.findPendingOrdersWithoutWave(Order.OrderStatus.CONFIRMED)
                .stream()
                .limit(10)
                .collect(Collectors.toList());

        List<OrderSummaryResponse> pendingOrderResponses = pendingOrders.stream()
                .map(order -> OrderSummaryResponse.builder()
                        .id(order.getId())
                        .orderNo(order.getOrderNo())
                        .customerName(order.getCustomerName())
                        .status(order.getStatus().name())
                        .itemCount(order.getItems().size())
                        .totalQuantity(order.getItems().stream().mapToInt(OrderItem::getQuantity).sum())
                        .build())
                .collect(Collectors.toList());

        List<Wave> activeWaves = waveRepository.findActiveWaves();

        List<DashboardResponse.WaveSummaryResponse> waveResponses = activeWaves.stream()
                .map(wave -> {
                    long completedTasks = pickingTaskRepository.countByWaveIdAndStatus(wave.getId(), PickingTask.TaskStatus.COMPLETED);
                    int totalTasks = pickingTaskRepository.findByWaveId(wave.getId()).size();
                    return DashboardResponse.WaveSummaryResponse.builder()
                            .id(wave.getId())
                            .waveNo(wave.getWaveNo())
                            .status(wave.getStatus().name())
                            .waveType(wave.getWaveType().name())
                            .orderCount(wave.getTotalOrderCount())
                            .taskCount(totalTasks)
                            .completedTaskCount((int) completedTasks)
                            .build();
                })
                .collect(Collectors.toList());

        List<InventoryResponse> lowStockItems = inventoryService.getOutOfStockItems()
                .stream()
                .limit(10)
                .collect(Collectors.toList());

        return DashboardResponse.builder()
                .stats(stats)
                .pendingOrders(pendingOrderResponses)
                .activeWaves(waveResponses)
                .lowStockItems(lowStockItems)
                .build();
    }
}
