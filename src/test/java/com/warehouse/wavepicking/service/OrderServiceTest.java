package com.warehouse.wavepicking.service;

import com.warehouse.wavepicking.dto.request.CreateOrderRequest;
import com.warehouse.wavepicking.dto.response.OrderResponse;
import com.warehouse.wavepicking.entity.Order;
import com.warehouse.wavepicking.entity.OrderItem;
import com.warehouse.wavepicking.entity.Sku;
import com.warehouse.wavepicking.exception.BusinessException;
import com.warehouse.wavepicking.repository.OrderRepository;
import com.warehouse.wavepicking.repository.SkuRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private SkuRepository skuRepository;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private OrderService orderService;

    private Sku testSku1;
    private Sku testSku2;
    private Order testOrder;

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
        testOrder.setAddress("测试地址");
        testOrder.setPhone("13800138000");
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

        OrderItem item2 = new OrderItem();
        item2.setId(2L);
        item2.setOrder(testOrder);
        item2.setSku(testSku2);
        item2.setQuantity(3);
        item2.setPickedQuantity(0);
        item2.setLocation("A02");

        testOrder.setItems(Arrays.asList(item1, item2));
    }

    @Test
    @DisplayName("获取所有订单")
    void testGetAllOrders() {
        when(orderRepository.findAll()).thenReturn(List.of(testOrder));

        List<OrderResponse> result = orderService.getAllOrders();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ORD20240101000001", result.get(0).getOrderNo());
        assertEquals(2, result.get(0).getItems().size());
        assertEquals(8, result.get(0).getTotalQuantity());
    }

    @Test
    @DisplayName("根据ID获取订单 - 成功")
    void testGetOrderById_Success() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        OrderResponse result = orderService.getOrderById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("测试客户", result.getCustomerName());
    }

    @Test
    @DisplayName("根据ID获取订单 - 不存在")
    void testGetOrderById_NotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.getOrderById(999L));

        assertEquals("ORDER_NOT_FOUND", exception.getCode());
    }

    @Test
    @DisplayName("创建订单 - 成功")
    void testCreateOrder_Success() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerName("新客户");
        request.setAddress("新地址");
        request.setPhone("13900139000");
        request.setUrgent(false);

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

        OrderResponse result = orderService.createOrder(request);

        assertNotNull(result);
        assertEquals("新客户", result.getCustomerName());
        assertEquals(Order.OrderStatus.PENDING.name(), result.getStatus());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("创建订单 - SKU不存在")
    void testCreateOrder_SkuNotFound() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerName("测试");

        CreateOrderRequest.OrderItemRequest itemRequest = new CreateOrderRequest.OrderItemRequest();
        itemRequest.setSkuId(999L);
        itemRequest.setQuantity(10);
        request.setItems(List.of(itemRequest));

        when(skuRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.createOrder(request));

        assertEquals("SKU_NOT_FOUND", exception.getCode());
    }

    @Test
    @DisplayName("确认订单 - 成功")
    void testConfirmOrder_Success() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(inventoryService.hasEnoughStock(1L, 5)).thenReturn(true);
        when(inventoryService.hasEnoughStock(2L, 3)).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        OrderResponse result = orderService.confirmOrder(1L);

        assertNotNull(result);
        verify(orderRepository).save(testOrder);
    }

    @Test
    @DisplayName("确认订单 - 状态不正确")
    void testConfirmOrder_InvalidStatus() {
        testOrder.setStatus(Order.OrderStatus.CONFIRMED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.confirmOrder(1L));

        assertEquals("INVALID_STATUS", exception.getCode());
    }

    @Test
    @DisplayName("取消订单 - 成功")
    void testCancelOrder_Success() {
        testOrder.setStatus(Order.OrderStatus.CONFIRMED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        OrderResponse result = orderService.cancelOrder(1L);

        assertNotNull(result);
        assertEquals(Order.OrderStatus.CANCELLED, testOrder.getStatus());
    }

    @Test
    @DisplayName("取消订单 - 已分配波次")
    void testCancelOrder_Allocated() {
        testOrder.setStatus(Order.OrderStatus.ALLOCATED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.cancelOrder(1L));

        assertEquals("CANNOT_CANCEL", exception.getCode());
    }

    @Test
    @DisplayName("获取待处理订单")
    void testGetPendingOrders() {
        when(orderRepository.findPendingOrdersWithoutWave(Order.OrderStatus.CONFIRMED))
                .thenReturn(List.of(testOrder));

        List<OrderResponse> result = orderService.getPendingOrders();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(orderRepository).findPendingOrdersWithoutWave(Order.OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("更新订单状态 - PENDING 到 CONFIRMED - 合法")
    void testUpdateStatus_PendingToConfirmed_Success() {
        testOrder.setStatus(Order.OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        OrderResponse result = orderService.updateStatus(1L, Order.OrderStatus.CONFIRMED);

        assertNotNull(result);
        assertEquals(Order.OrderStatus.CONFIRMED, testOrder.getStatus());
        verify(orderRepository).save(testOrder);
    }

    @Test
    @DisplayName("更新订单状态 - CONFIRMED 到 ALLOCATED - 合法")
    void testUpdateStatus_ConfirmedToAllocated_Success() {
        testOrder.setStatus(Order.OrderStatus.CONFIRMED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        OrderResponse result = orderService.updateStatus(1L, Order.OrderStatus.ALLOCATED);

        assertNotNull(result);
        assertEquals(Order.OrderStatus.ALLOCATED, testOrder.getStatus());
        verify(orderRepository).save(testOrder);
    }

    @Test
    @DisplayName("更新订单状态 - ALLOCATED 到 PICKING - 合法")
    void testUpdateStatus_AllocatedToPicking_Success() {
        testOrder.setStatus(Order.OrderStatus.ALLOCATED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        OrderResponse result = orderService.updateStatus(1L, Order.OrderStatus.PICKING);

        assertNotNull(result);
        assertEquals(Order.OrderStatus.PICKING, testOrder.getStatus());
        verify(orderRepository).save(testOrder);
    }

    @Test
    @DisplayName("更新订单状态 - PICKING 到 PICKED - 合法")
    void testUpdateStatus_PickingToPicked_Success() {
        testOrder.setStatus(Order.OrderStatus.PICKING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        OrderResponse result = orderService.updateStatus(1L, Order.OrderStatus.PICKED);

        assertNotNull(result);
        assertEquals(Order.OrderStatus.PICKED, testOrder.getStatus());
        verify(orderRepository).save(testOrder);
    }

    @Test
    @DisplayName("更新订单状态 - PICKED 到 PACKED - 合法")
    void testUpdateStatus_PickedToPacked_Success() {
        testOrder.setStatus(Order.OrderStatus.PICKED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        OrderResponse result = orderService.updateStatus(1L, Order.OrderStatus.PACKED);

        assertNotNull(result);
        assertEquals(Order.OrderStatus.PACKED, testOrder.getStatus());
        verify(orderRepository).save(testOrder);
    }

    @Test
    @DisplayName("更新订单状态 - PACKED 到 SHIPPED - 合法")
    void testUpdateStatus_PackedToShipped_Success() {
        testOrder.setStatus(Order.OrderStatus.PACKED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        OrderResponse result = orderService.updateStatus(1L, Order.OrderStatus.SHIPPED);

        assertNotNull(result);
        assertEquals(Order.OrderStatus.SHIPPED, testOrder.getStatus());
        assertNotNull(testOrder.getCompletedAt());
        verify(orderRepository).save(testOrder);
    }

    @Test
    @DisplayName("更新订单状态 - PENDING 到 SHIPPED - 非法跳转变换")
    void testUpdateStatus_PendingToShipped_InvalidTransition() {
        testOrder.setStatus(Order.OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.updateStatus(1L, Order.OrderStatus.SHIPPED));

        assertEquals("INVALID_STATUS_TRANSITION", exception.getCode());
        assertTrue(exception.getMessage().contains("订单状态无法从 PENDING 转换为 SHIPPED"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("更新订单状态 - SHIPPED 不能转换任何状态")
    void testUpdateStatus_ShippedCannotTransition_Invalid() {
        testOrder.setStatus(Order.OrderStatus.SHIPPED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.updateStatus(1L, Order.OrderStatus.PENDING));

        assertEquals("INVALID_STATUS_TRANSITION", exception.getCode());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("更新订单状态 - CANCELLED 不能转换任何状态")
    void testUpdateStatus_CancelledCannotTransition_Invalid() {
        testOrder.setStatus(Order.OrderStatus.CANCELLED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.updateStatus(1L, Order.OrderStatus.PENDING));

        assertEquals("INVALID_STATUS_TRANSITION", exception.getCode());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("更新订单状态 - ALLOCATED 回退到 CONFIRMED - 合法回退")
    void testUpdateStatus_AllocatedToConfirmed_Success() {
        testOrder.setStatus(Order.OrderStatus.ALLOCATED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        OrderResponse result = orderService.updateStatus(1L, Order.OrderStatus.CONFIRMED);

        assertNotNull(result);
        assertEquals(Order.OrderStatus.CONFIRMED, testOrder.getStatus());
        verify(orderRepository).save(testOrder);
    }

    @Test
    @DisplayName("更新订单状态 - PICKING 不能直接到 PACKED - 非法跳转变换")
    void testUpdateStatus_PickingToPacked_Invalid() {
        testOrder.setStatus(Order.OrderStatus.PICKING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.updateStatus(1L, Order.OrderStatus.PACKED));

        assertEquals("INVALID_STATUS_TRANSITION", exception.getCode());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("更新订单状态 - 订单不存在")
    void testUpdateStatus_OrderNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.updateStatus(999L, Order.OrderStatus.CONFIRMED));

        assertEquals("ORDER_NOT_FOUND", exception.getCode());
    }

    @Test
    @DisplayName("取消订单 - PENDING 状态可以取消")
    void testCancelOrder_PendingStatus_Success() {
        testOrder.setStatus(Order.OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        OrderResponse result = orderService.cancelOrder(1L);

        assertNotNull(result);
        assertEquals(Order.OrderStatus.CANCELLED, testOrder.getStatus());
        verify(orderRepository).save(testOrder);
    }

    @Test
    @DisplayName("取消订单 - PICKING 状态不能取消")
    void testCancelOrder_PickingStatus_Invalid() {
        testOrder.setStatus(Order.OrderStatus.PICKING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.cancelOrder(1L));

        assertEquals("CANNOT_CANCEL", exception.getCode());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("完整状态流转链路 - 从创建到发货")
    void testFullStatusFlow_Success() {
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
    }
}
