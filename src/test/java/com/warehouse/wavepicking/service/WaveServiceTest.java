package com.warehouse.wavepicking.service;

import com.warehouse.wavepicking.dto.request.CreateWaveRequest;
import com.warehouse.wavepicking.dto.response.WaveResponse;
import com.warehouse.wavepicking.entity.*;
import com.warehouse.wavepicking.exception.BusinessException;
import com.warehouse.wavepicking.repository.OrderItemRepository;
import com.warehouse.wavepicking.repository.OrderRepository;
import com.warehouse.wavepicking.repository.WaveRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WaveServiceTest {

    @Mock
    private WaveRepository waveRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private IInventoryService inventoryService;

    @Mock
    private IOrderService orderService;

    @Mock
    private IPickingTaskService pickingTaskService;

    @InjectMocks
    private WaveService waveService;

    private Wave testWave;
    private Order testOrder1;
    private Order testOrder2;
    private Sku testSku1;
    private Sku testSku2;

    @BeforeEach
    void setUp() {
        testSku1 = new Sku();
        testSku1.setId(1L);
        testSku1.setSkuCode("SKU001");
        testSku1.setSkuName("商品1");

        testSku2 = new Sku();
        testSku2.setId(2L);
        testSku2.setSkuCode("SKU002");
        testSku2.setSkuName("商品2");

        testOrder1 = new Order();
        testOrder1.setId(1L);
        testOrder1.setOrderNo("ORD001");
        testOrder1.setCustomerName("客户1");
        testOrder1.setStatus(Order.OrderStatus.CONFIRMED);
        testOrder1.setUrgent(false);
        testOrder1.setItems(new ArrayList<>());

        OrderItem item1 = new OrderItem();
        item1.setId(1L);
        item1.setOrder(testOrder1);
        item1.setSku(testSku1);
        item1.setQuantity(10);
        testOrder1.getItems().add(item1);

        OrderItem item2 = new OrderItem();
        item2.setId(2L);
        item2.setOrder(testOrder1);
        item2.setSku(testSku2);
        item2.setQuantity(5);
        testOrder1.getItems().add(item2);

        testOrder2 = new Order();
        testOrder2.setId(2L);
        testOrder2.setOrderNo("ORD002");
        testOrder2.setCustomerName("客户2");
        testOrder2.setStatus(Order.OrderStatus.CONFIRMED);
        testOrder2.setUrgent(true);
        testOrder2.setItems(new ArrayList<>());

        OrderItem item3 = new OrderItem();
        item3.setId(3L);
        item3.setOrder(testOrder2);
        item3.setSku(testSku1);
        item3.setQuantity(3);
        testOrder2.getItems().add(item3);

        testWave = new Wave();
        testWave.setId(1L);
        testWave.setWaveNo("WAVE001");
        testWave.setStatus(Wave.WaveStatus.NEW);
        testWave.setWaveType(Wave.WaveType.NORMAL);
        testWave.setTotalOrderCount(2);
        testWave.setTotalSkuCount(2);
        testWave.setTotalQuantity(18);
        testWave.setCreatedAt(LocalDateTime.now());
        testWave.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("获取所有波次")
    void testGetAllWaves() {
        when(waveRepository.findAll()).thenReturn(List.of(testWave));

        List<WaveResponse> result = waveService.getAllWaves();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("WAVE001", result.get(0).getWaveNo());
    }

    @Test
    @DisplayName("根据ID获取波次 - 成功")
    void testGetWaveById_Success() {
        when(waveRepository.findById(1L)).thenReturn(Optional.of(testWave));
        when(orderRepository.findByWaveId(1L)).thenReturn(List.of(testOrder1, testOrder2));

        WaveResponse result = waveService.getWaveById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("WAVE001", result.getWaveNo());
    }

    @Test
    @DisplayName("根据ID获取波次 - 不存在")
    void testGetWaveById_NotFound() {
        when(waveRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> waveService.getWaveById(999L));

        assertEquals("WAVE_NOT_FOUND", exception.getCode());
    }

    @Test
    @DisplayName("创建波次 - 自动选单")
    void testCreateWave_AutoSelect() {
        CreateWaveRequest request = new CreateWaveRequest();
        request.setMaxOrderCount(10);
        request.setWaveType("NORMAL");

        when(orderRepository.findUrgentPendingOrdersWithoutWave(Order.OrderStatus.CONFIRMED))
                .thenReturn(List.of(testOrder2));
        when(orderRepository.findPendingOrdersWithoutWave(Order.OrderStatus.CONFIRMED))
                .thenReturn(List.of(testOrder1));
        when(waveRepository.save(any(Wave.class))).thenAnswer(invocation -> {
            Wave saved = invocation.getArgument(0);
            saved.setId(2L);
            saved.setCreatedAt(LocalDateTime.now());
            saved.setUpdatedAt(LocalDateTime.now());
            return saved;
        });

        WaveResponse result = waveService.createWave(request);

        assertNotNull(result);
        verify(waveRepository).save(any(Wave.class));
        verify(orderService, times(2)).allocateOrderToWave(any(Order.class), any(Wave.class));
    }

    @Test
    @DisplayName("创建波次 - 指定订单ID")
    void testCreateWave_WithOrderIds() {
        CreateWaveRequest request = new CreateWaveRequest();
        request.setOrderIds(Arrays.asList(1L, 2L));

        when(orderRepository.findAllById(Arrays.asList(1L, 2L)))
                .thenReturn(Arrays.asList(testOrder1, testOrder2));
        when(waveRepository.save(any(Wave.class))).thenAnswer(invocation -> {
            Wave saved = invocation.getArgument(0);
            saved.setId(2L);
            saved.setCreatedAt(LocalDateTime.now());
            saved.setUpdatedAt(LocalDateTime.now());
            return saved;
        });

        WaveResponse result = waveService.createWave(request);

        assertNotNull(result);
        verify(orderService, times(2)).allocateOrderToWave(any(Order.class), any(Wave.class));
    }

    @Test
    @DisplayName("创建波次 - 无可用订单")
    void testCreateWave_NoOrders() {
        CreateWaveRequest request = new CreateWaveRequest();

        when(orderRepository.findUrgentPendingOrdersWithoutWave(Order.OrderStatus.CONFIRMED))
                .thenReturn(List.of());
        when(orderRepository.findPendingOrdersWithoutWave(Order.OrderStatus.CONFIRMED))
                .thenReturn(List.of());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> waveService.createWave(request));

        assertEquals("NO_ORDERS", exception.getCode());
    }

    @Test
    @DisplayName("释放波次 - 成功")
    void testReleaseWave_Success() {
        testWave.setStatus(Wave.WaveStatus.NEW);
        List<Order> orders = Arrays.asList(testOrder1, testOrder2);

        when(waveRepository.findById(1L)).thenReturn(Optional.of(testWave));
        when(orderRepository.findByWaveId(1L)).thenReturn(orders);
        when(waveRepository.save(any(Wave.class))).thenReturn(testWave);
        doNothing().when(inventoryService).lockStock(anyLong(), anyInt());
        doNothing().when(pickingTaskService).generatePickingTasks(any(Wave.class), anyList());
        when(orderService.updateStatus(anyLong(), eq(Order.OrderStatus.PICKING))).thenReturn(null);

        WaveResponse result = waveService.releaseWave(1L);

        assertNotNull(result);
        assertEquals(Wave.WaveStatus.PICKING, testWave.getStatus());
        assertNotNull(testWave.getReleasedAt());
        verify(inventoryService, atLeastOnce()).lockStock(anyLong(), anyInt());
        verify(pickingTaskService).generatePickingTasks(any(Wave.class), anyList());
    }

    @Test
    @DisplayName("释放波次 - 状态不正确")
    void testReleaseWave_InvalidStatus() {
        testWave.setStatus(Wave.WaveStatus.PICKING);
        when(waveRepository.findById(1L)).thenReturn(Optional.of(testWave));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> waveService.releaseWave(1L));

        assertEquals("INVALID_STATUS", exception.getCode());
    }

    @Test
    @DisplayName("回滚波次 - 成功")
    void testRollbackWave_Success() {
        testWave.setStatus(Wave.WaveStatus.PICKING);
        List<Order> orders = Arrays.asList(testOrder1, testOrder2);

        when(waveRepository.findById(1L)).thenReturn(Optional.of(testWave));
        when(orderRepository.findByWaveId(1L)).thenReturn(orders);
        when(waveRepository.save(any(Wave.class))).thenReturn(testWave);
        doNothing().when(inventoryService).unlockStock(anyLong(), anyInt());
        doNothing().when(pickingTaskService).cancelTasksByWave(anyLong());
        doNothing().when(orderService).rollbackOrderFromWave(any(Order.class));

        WaveResponse result = waveService.rollbackWave(1L);

        assertNotNull(result);
        assertEquals(Wave.WaveStatus.CANCELLED, testWave.getStatus());
        verify(inventoryService, atLeastOnce()).unlockStock(anyLong(), anyInt());
        verify(pickingTaskService).cancelTasksByWave(1L);
    }

    @Test
    @DisplayName("完成波次 - 成功")
    void testCompleteWave_Success() {
        testWave.setStatus(Wave.WaveStatus.PICKING);
        List<Order> orders = Arrays.asList(testOrder1, testOrder2);

        when(waveRepository.findById(1L)).thenReturn(Optional.of(testWave));
        when(pickingTaskService.countCompletedTasksByWave(1L)).thenReturn(5L);
        when(pickingTaskService.countTotalTasksByWave(1L)).thenReturn(5L);
        when(orderRepository.findByWaveId(1L)).thenReturn(orders);
        when(waveRepository.save(any(Wave.class))).thenReturn(testWave);
        when(orderService.updateStatus(anyLong(), eq(Order.OrderStatus.PICKED))).thenReturn(null);

        WaveResponse result = waveService.completeWave(1L);

        assertNotNull(result);
        assertEquals(Wave.WaveStatus.COMPLETED, testWave.getStatus());
        assertNotNull(testWave.getCompletedAt());
    }

    @Test
    @DisplayName("完成波次 - 任务未完成")
    void testCompleteWave_TasksNotDone() {
        testWave.setStatus(Wave.WaveStatus.PICKING);

        when(waveRepository.findById(1L)).thenReturn(Optional.of(testWave));
        when(pickingTaskService.countCompletedTasksByWave(1L)).thenReturn(2L);
        when(pickingTaskService.countTotalTasksByWave(1L)).thenReturn(5L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> waveService.completeWave(1L));

        assertEquals("TASKS_NOT_COMPLETED", exception.getCode());
    }

    @Test
    @DisplayName("获取进行中波次")
    void testGetActiveWaves() {
        when(waveRepository.findActiveWaves()).thenReturn(List.of(testWave));

        List<WaveResponse> result = waveService.getActiveWaves();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(waveRepository).findActiveWaves();
    }
}
