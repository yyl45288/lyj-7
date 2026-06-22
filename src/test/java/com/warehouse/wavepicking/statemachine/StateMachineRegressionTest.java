package com.warehouse.wavepicking.statemachine;

import com.warehouse.wavepicking.dto.request.CreateOrderRequest;
import com.warehouse.wavepicking.dto.response.OrderResponse;
import com.warehouse.wavepicking.entity.*;
import com.warehouse.wavepicking.exception.BusinessException;
import com.warehouse.wavepicking.repository.OrderRepository;
import com.warehouse.wavepicking.repository.SkuRepository;
import com.warehouse.wavepicking.repository.WaveRepository;
import com.warehouse.wavepicking.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("状态机重构回归测试 - 验证业务行为未改变")
@ExtendWith(MockitoExtension.class)
class StateMachineRegressionTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private SkuRepository skuRepository;

    @Mock
    private IInventoryLockService inventoryLockService;

    @Mock
    private WaveRepository waveRepository;

    @Mock
    private IInventoryService inventoryService;

    @Mock
    private IPickingTaskService pickingTaskService;

    @InjectMocks
    private OrderService orderService;

    @InjectMocks
    private WaveService waveService;

    private Sku testSku1;
    private Sku testSku2;
    private Order testOrder;
    private Wave testWave;

    @BeforeEach
    void setUp() {
        testSku1 = new Sku();
        testSku1.setId(1L);
        testSku1.setSkuCode("SKU001");
        testSku1.setSkuName("商品1");
        testSku1.setLocation("A01");

        testSku2 = new Sku();
        testSku2.setId(2L);
        testSku2.setSkuCode("SKU002");
        testSku2.setSkuName("商品2");
        testSku2.setLocation("A02");

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setOrderNo("ORD20240101000001");
        testOrder.setCustomerName("测试客户");
        testOrder.setStatus(Order.OrderStatus.PENDING);
        testOrder.setUrgent(false);
        testOrder.setCreatedAt(LocalDateTime.now());
        testOrder.setUpdatedAt(LocalDateTime.now());

        OrderItem item1 = new OrderItem();
        item1.setId(1L);
        item1.setOrder(testOrder);
        item1.setSku(testSku1);
        item1.setQuantity(5);
        item1.setPickedQuantity(0);
        item1.setLocation("A01");

        testOrder.setItems(List.of(item1));

        testWave = new Wave();
        testWave.setId(1L);
        testWave.setWaveNo("WAVE20240101000001");
        testWave.setStatus(Wave.WaveStatus.NEW);
        testWave.setWaveType(Wave.WaveType.NORMAL);
        testWave.setCreatedAt(LocalDateTime.now());
        testWave.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("回归测试：订单创建后自动变为CONFIRMED - 行为与重构前一致")
    void testCreateOrder_StatusTransition_Regression() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerName("新客户");
        CreateOrderRequest.OrderItemRequest itemRequest = new CreateOrderRequest.OrderItemRequest();
        itemRequest.setSkuId(1L);
        itemRequest.setQuantity(10);
        request.setItems(List.of(itemRequest));

        when(skuRepository.findById(1L)).thenReturn(Optional.of(testSku1));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            saved.setId(2L);
            saved.setCreatedAt(LocalDateTime.now());
            saved.setUpdatedAt(LocalDateTime.now());
            return saved;
        });
        when(inventoryLockService.lockStockForOrder(any(Order.class))).thenReturn(List.of());

        OrderResponse result = orderService.createOrder(request);

        assertEquals(Order.OrderStatus.CONFIRMED.name(), result.getStatus());
        verify(orderRepository, times(2)).save(any(Order.class));
        verify(inventoryLockService).lockStockForOrder(any(Order.class));
    }

    @Test
    @DisplayName("回归测试：确认订单 - 错误码和消息与重构前一致")
    void testConfirmOrder_InvalidStatus_ErrorCode_Regression() {
        testOrder.setStatus(Order.OrderStatus.ALLOCATED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.confirmOrder(1L));

        assertEquals("INVALID_STATUS", exception.getCode());
        assertEquals("只有待确认订单才能确认", exception.getMessage());
    }

    @Test
    @DisplayName("回归测试：取消订单 - 已分配波次的错误码与重构前一致")
    void testCancelOrder_HasWave_ErrorCode_Regression() {
        testOrder.setStatus(Order.OrderStatus.CONFIRMED);
        testOrder.setWave(testWave);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.cancelOrder(1L));

        assertEquals("CANNOT_CANCEL", exception.getCode());
        assertEquals("订单已在波次中，无法取消，请先回滚波次", exception.getMessage());
        verify(inventoryLockService, never()).releaseStockForCancelledOrder(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("回归测试：更新订单状态 - 非法跳转变换错误码与重构前一致")
    void testUpdateStatus_InvalidTransition_ErrorCode_Regression() {
        testOrder.setStatus(Order.OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.updateStatus(1L, Order.OrderStatus.SHIPPED));

        assertEquals("INVALID_STATUS_TRANSITION", exception.getCode());
        assertTrue(exception.getMessage().contains("订单状态无法从 PENDING 转换为 SHIPPED"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("回归测试：波次释放 - 非NEW状态错误码与重构前一致")
    void testReleaseWave_InvalidStatus_ErrorCode_Regression() {
        testWave.setStatus(Wave.WaveStatus.RELEASED);
        when(waveRepository.findById(1L)).thenReturn(Optional.of(testWave));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> waveService.releaseWave(1L));

        assertEquals("INVALID_STATUS", exception.getCode());
        assertEquals("只有新建状态的波次才能释放", exception.getMessage());
    }

    @Test
    @DisplayName("回归测试：波次回滚 - 状态错误码与重构前一致")
    void testRollbackWave_InvalidStatus_ErrorCode_Regression() {
        testWave.setStatus(Wave.WaveStatus.NEW);
        when(waveRepository.findById(1L)).thenReturn(Optional.of(testWave));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> waveService.rollbackWave(1L));

        assertEquals("INVALID_STATUS", exception.getCode());
        assertEquals("只有已释放或拣货中的波次才能回滚", exception.getMessage());
    }

    @Test
    @DisplayName("回归测试：波次完成 - 任务未完成错误码与重构前一致")
    void testCompleteWave_TasksNotDone_ErrorCode_Regression() {
        testWave.setStatus(Wave.WaveStatus.PICKING);
        when(waveRepository.findById(1L)).thenReturn(Optional.of(testWave));
        when(pickingTaskService.countCompletedTasksByWave(1L)).thenReturn(2L);
        when(pickingTaskService.countTotalTasksByWave(1L)).thenReturn(5L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> waveService.completeWave(1L));

        assertEquals("TASKS_NOT_COMPLETED", exception.getCode());
        assertEquals("拣货任务未全部完成，已完成: 2，总计: 5", exception.getMessage());
    }

    @Test
    @DisplayName("回归测试：紧急插单 - 订单非紧急错误码与重构前一致")
    void testAddUrgentOrder_NotUrgent_ErrorCode_Regression() {
        testWave.setStatus(Wave.WaveStatus.PICKING);
        testOrder.setStatus(Order.OrderStatus.CONFIRMED);
        testOrder.setUrgent(false);

        when(waveRepository.findById(1L)).thenReturn(Optional.of(testWave));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> waveService.addUrgentOrderToWave(1L, 1L));

        assertEquals("NOT_URGENT", exception.getCode());
        assertEquals("只有紧急订单才能插入正在进行的波次", exception.getMessage());
    }

    @Test
    @DisplayName("回归测试：完整订单状态流转链路 - 行为与重构前完全一致")
    void testFullOrderStatusFlow_Regression() {
        testOrder.setStatus(Order.OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        orderService.updateStatus(1L, Order.OrderStatus.CONFIRMED);
        assertEquals(Order.OrderStatus.CONFIRMED, testOrder.getStatus());

        orderService.updateStatus(1L, Order.OrderStatus.ALLOCATED);
        assertEquals(Order.OrderStatus.ALLOCATED, testOrder.getStatus());

        orderService.updateStatus(1L, Order.OrderStatus.PICKING);
        assertEquals(Order.OrderStatus.PICKING, testOrder.getStatus());

        orderService.updateStatus(1L, Order.OrderStatus.PICKED);
        assertEquals(Order.OrderStatus.PICKED, testOrder.getStatus());

        orderService.updateStatus(1L, Order.OrderStatus.PACKED);
        assertEquals(Order.OrderStatus.PACKED, testOrder.getStatus());

        orderService.updateStatus(1L, Order.OrderStatus.SHIPPED);
        assertEquals(Order.OrderStatus.SHIPPED, testOrder.getStatus());
        assertNotNull(testOrder.getCompletedAt());

        verify(orderRepository, times(6)).save(testOrder);
        verify(inventoryLockService).deductStockForShippedOrder(testOrder);
    }

    @Test
    @DisplayName("回归测试：订单分配到波次 - 状态校验行为一致")
    void testAllocateOrderToWave_StatusCheck_Regression() {
        testOrder.setStatus(Order.OrderStatus.CONFIRMED);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        orderService.allocateOrderToWave(testOrder, testWave);

        assertEquals(Order.OrderStatus.ALLOCATED, testOrder.getStatus());
        assertSame(testWave, testOrder.getWave());
        verify(orderRepository).save(testOrder);
    }

    @Test
    @DisplayName("回归测试：订单分配到波次 - PENDING状态应失败")
    void testAllocateOrderToWave_PendingStatus_ShouldFail_Regression() {
        testOrder.setStatus(Order.OrderStatus.PENDING);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.allocateOrderToWave(testOrder, testWave));

        assertEquals("INVALID_STATUS_TRANSITION", exception.getCode());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("回归测试：订单从波次回滚 - ALLOCATED状态成功")
    void testRollbackOrderFromWave_Allocated_Success_Regression() {
        testOrder.setStatus(Order.OrderStatus.ALLOCATED);
        testOrder.setWave(testWave);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        orderService.rollbackOrderFromWave(testOrder);

        assertEquals(Order.OrderStatus.CONFIRMED, testOrder.getStatus());
        assertNull(testOrder.getWave());
        verify(orderRepository).save(testOrder);
    }

    @Test
    @DisplayName("回归测试：订单从波次回滚 - PICKING状态应失败")
    void testRollbackOrderFromWave_PickingStatus_ShouldFail_Regression() {
        testOrder.setStatus(Order.OrderStatus.PICKING);
        testOrder.setWave(testWave);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.rollbackOrderFromWave(testOrder));

        assertEquals("INVALID_STATUS_TRANSITION", exception.getCode());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("回归测试：CANCELLED状态不能转换为任何状态 - 行为一致")
    void testCancelledStateCannotTransition_Regression() {
        testOrder.setStatus(Order.OrderStatus.CANCELLED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        for (Order.OrderStatus target : Order.OrderStatus.values()) {
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> orderService.updateStatus(1L, target),
                    "CANCELLED 状态转换为 " + target + " 应该失败");
            assertEquals("INVALID_STATUS_TRANSITION", exception.getCode());
        }
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("回归测试：SHIPPED状态不能转换为任何状态 - 行为一致")
    void testShippedStateCannotTransition_Regression() {
        testOrder.setStatus(Order.OrderStatus.SHIPPED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        for (Order.OrderStatus target : Order.OrderStatus.values()) {
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> orderService.updateStatus(1L, target),
                    "SHIPPED 状态转换为 " + target + " 应该失败");
            assertEquals("INVALID_STATUS_TRANSITION", exception.getCode());
        }
        verify(orderRepository, never()).save(any());
    }
}
