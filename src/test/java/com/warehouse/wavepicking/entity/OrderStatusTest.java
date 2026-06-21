package com.warehouse.wavepicking.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("订单状态机测试")
class OrderStatusTest {

    @ParameterizedTest
    @MethodSource("validTransitions")
    @DisplayName("合法状态转换 - 应该返回 true")
    void testValidTransitions(Order.OrderStatus from, Order.OrderStatus to) {
        assertTrue(from.canTransitionTo(to),
                String.format("状态 %s 应该可以转换为 %s", from, to));
    }

    @ParameterizedTest
    @MethodSource("invalidTransitions")
    @DisplayName("非法状态转换 - 应该返回 false")
    void testInvalidTransitions(Order.OrderStatus from, Order.OrderStatus to) {
        assertFalse(from.canTransitionTo(to),
                String.format("状态 %s 不应该可以转换为 %s", from, to));
    }

    @Test
    @DisplayName("错误消息格式验证")
    void testGetTransitionErrorMessage() {
        String message = Order.OrderStatus.PENDING.getTransitionErrorMessage(Order.OrderStatus.SHIPPED);
        assertEquals("订单状态无法从 PENDING 转换为 SHIPPED", message);
    }

    @Test
    @DisplayName("终态 SHIPPED 不能转换为任何状态")
    void testShippedIsTerminalState() {
        for (Order.OrderStatus target : Order.OrderStatus.values()) {
            assertFalse(Order.OrderStatus.SHIPPED.canTransitionTo(target),
                    "SHIPPED 状态不能转换为任何状态，包括 " + target);
        }
    }

    @Test
    @DisplayName("终态 CANCELLED 不能转换为任何状态")
    void testCancelledIsTerminalState() {
        for (Order.OrderStatus target : Order.OrderStatus.values()) {
            assertFalse(Order.OrderStatus.CANCELLED.canTransitionTo(target),
                    "CANCELLED 状态不能转换为任何状态，包括 " + target);
        }
    }

    @Test
    @DisplayName("PENDING 状态的合法转换")
    void testPendingTransitions() {
        assertTrue(Order.OrderStatus.PENDING.canTransitionTo(Order.OrderStatus.CONFIRMED));
        assertTrue(Order.OrderStatus.PENDING.canTransitionTo(Order.OrderStatus.CANCELLED));
        assertFalse(Order.OrderStatus.PENDING.canTransitionTo(Order.OrderStatus.ALLOCATED));
        assertFalse(Order.OrderStatus.PENDING.canTransitionTo(Order.OrderStatus.PICKING));
        assertFalse(Order.OrderStatus.PENDING.canTransitionTo(Order.OrderStatus.PICKED));
        assertFalse(Order.OrderStatus.PENDING.canTransitionTo(Order.OrderStatus.PACKED));
        assertFalse(Order.OrderStatus.PENDING.canTransitionTo(Order.OrderStatus.SHIPPED));
    }

    @Test
    @DisplayName("CONFIRMED 状态的合法转换")
    void testConfirmedTransitions() {
        assertTrue(Order.OrderStatus.CONFIRMED.canTransitionTo(Order.OrderStatus.ALLOCATED));
        assertTrue(Order.OrderStatus.CONFIRMED.canTransitionTo(Order.OrderStatus.CANCELLED));
        assertFalse(Order.OrderStatus.CONFIRMED.canTransitionTo(Order.OrderStatus.PENDING));
        assertFalse(Order.OrderStatus.CONFIRMED.canTransitionTo(Order.OrderStatus.PICKING));
        assertFalse(Order.OrderStatus.CONFIRMED.canTransitionTo(Order.OrderStatus.SHIPPED));
    }

    @Test
    @DisplayName("ALLOCATED 状态的合法转换")
    void testAllocatedTransitions() {
        assertTrue(Order.OrderStatus.ALLOCATED.canTransitionTo(Order.OrderStatus.PICKING));
        assertTrue(Order.OrderStatus.ALLOCATED.canTransitionTo(Order.OrderStatus.CONFIRMED));
        assertFalse(Order.OrderStatus.ALLOCATED.canTransitionTo(Order.OrderStatus.CANCELLED));
        assertFalse(Order.OrderStatus.ALLOCATED.canTransitionTo(Order.OrderStatus.PICKED));
        assertFalse(Order.OrderStatus.ALLOCATED.canTransitionTo(Order.OrderStatus.SHIPPED));
    }

    @Test
    @DisplayName("PICKING 状态的合法转换")
    void testPickingTransitions() {
        assertTrue(Order.OrderStatus.PICKING.canTransitionTo(Order.OrderStatus.PICKED));
        assertFalse(Order.OrderStatus.PICKING.canTransitionTo(Order.OrderStatus.PENDING));
        assertFalse(Order.OrderStatus.PICKING.canTransitionTo(Order.OrderStatus.CONFIRMED));
        assertFalse(Order.OrderStatus.PICKING.canTransitionTo(Order.OrderStatus.ALLOCATED));
        assertFalse(Order.OrderStatus.PICKING.canTransitionTo(Order.OrderStatus.PACKED));
        assertFalse(Order.OrderStatus.PICKING.canTransitionTo(Order.OrderStatus.SHIPPED));
        assertFalse(Order.OrderStatus.PICKING.canTransitionTo(Order.OrderStatus.CANCELLED));
    }

    @Test
    @DisplayName("PICKED 状态的合法转换")
    void testPickedTransitions() {
        assertTrue(Order.OrderStatus.PICKED.canTransitionTo(Order.OrderStatus.PACKED));
        assertFalse(Order.OrderStatus.PICKED.canTransitionTo(Order.OrderStatus.PICKING));
        assertFalse(Order.OrderStatus.PICKED.canTransitionTo(Order.OrderStatus.SHIPPED));
        assertFalse(Order.OrderStatus.PICKED.canTransitionTo(Order.OrderStatus.CANCELLED));
    }

    @Test
    @DisplayName("PACKED 状态的合法转换")
    void testPackedTransitions() {
        assertTrue(Order.OrderStatus.PACKED.canTransitionTo(Order.OrderStatus.SHIPPED));
        assertFalse(Order.OrderStatus.PACKED.canTransitionTo(Order.OrderStatus.PICKED));
        assertFalse(Order.OrderStatus.PACKED.canTransitionTo(Order.OrderStatus.CANCELLED));
    }

    private static Stream<Arguments> validTransitions() {
        return Stream.of(
                Arguments.of(Order.OrderStatus.PENDING, Order.OrderStatus.CONFIRMED),
                Arguments.of(Order.OrderStatus.PENDING, Order.OrderStatus.CANCELLED),
                Arguments.of(Order.OrderStatus.CONFIRMED, Order.OrderStatus.ALLOCATED),
                Arguments.of(Order.OrderStatus.CONFIRMED, Order.OrderStatus.CANCELLED),
                Arguments.of(Order.OrderStatus.ALLOCATED, Order.OrderStatus.PICKING),
                Arguments.of(Order.OrderStatus.ALLOCATED, Order.OrderStatus.CONFIRMED),
                Arguments.of(Order.OrderStatus.PICKING, Order.OrderStatus.PICKED),
                Arguments.of(Order.OrderStatus.PICKED, Order.OrderStatus.PACKED),
                Arguments.of(Order.OrderStatus.PACKED, Order.OrderStatus.SHIPPED)
        );
    }

    private static Stream<Arguments> invalidTransitions() {
        return Stream.of(
                Arguments.of(Order.OrderStatus.PENDING, Order.OrderStatus.PENDING),
                Arguments.of(Order.OrderStatus.PENDING, Order.OrderStatus.ALLOCATED),
                Arguments.of(Order.OrderStatus.PENDING, Order.OrderStatus.PICKING),
                Arguments.of(Order.OrderStatus.PENDING, Order.OrderStatus.PICKED),
                Arguments.of(Order.OrderStatus.PENDING, Order.OrderStatus.PACKED),
                Arguments.of(Order.OrderStatus.PENDING, Order.OrderStatus.SHIPPED),
                Arguments.of(Order.OrderStatus.CONFIRMED, Order.OrderStatus.PENDING),
                Arguments.of(Order.OrderStatus.CONFIRMED, Order.OrderStatus.PICKING),
                Arguments.of(Order.OrderStatus.CONFIRMED, Order.OrderStatus.PICKED),
                Arguments.of(Order.OrderStatus.CONFIRMED, Order.OrderStatus.PACKED),
                Arguments.of(Order.OrderStatus.CONFIRMED, Order.OrderStatus.SHIPPED),
                Arguments.of(Order.OrderStatus.ALLOCATED, Order.OrderStatus.PENDING),
                Arguments.of(Order.OrderStatus.ALLOCATED, Order.OrderStatus.PICKED),
                Arguments.of(Order.OrderStatus.ALLOCATED, Order.OrderStatus.PACKED),
                Arguments.of(Order.OrderStatus.ALLOCATED, Order.OrderStatus.SHIPPED),
                Arguments.of(Order.OrderStatus.ALLOCATED, Order.OrderStatus.CANCELLED),
                Arguments.of(Order.OrderStatus.PICKING, Order.OrderStatus.PENDING),
                Arguments.of(Order.OrderStatus.PICKING, Order.OrderStatus.CONFIRMED),
                Arguments.of(Order.OrderStatus.PICKING, Order.OrderStatus.ALLOCATED),
                Arguments.of(Order.OrderStatus.PICKING, Order.OrderStatus.PACKED),
                Arguments.of(Order.OrderStatus.PICKING, Order.OrderStatus.SHIPPED),
                Arguments.of(Order.OrderStatus.PICKING, Order.OrderStatus.CANCELLED),
                Arguments.of(Order.OrderStatus.PICKED, Order.OrderStatus.PENDING),
                Arguments.of(Order.OrderStatus.PICKED, Order.OrderStatus.CONFIRMED),
                Arguments.of(Order.OrderStatus.PICKED, Order.OrderStatus.ALLOCATED),
                Arguments.of(Order.OrderStatus.PICKED, Order.OrderStatus.PICKING),
                Arguments.of(Order.OrderStatus.PICKED, Order.OrderStatus.SHIPPED),
                Arguments.of(Order.OrderStatus.PICKED, Order.OrderStatus.CANCELLED),
                Arguments.of(Order.OrderStatus.PACKED, Order.OrderStatus.PENDING),
                Arguments.of(Order.OrderStatus.PACKED, Order.OrderStatus.CONFIRMED),
                Arguments.of(Order.OrderStatus.PACKED, Order.OrderStatus.ALLOCATED),
                Arguments.of(Order.OrderStatus.PACKED, Order.OrderStatus.PICKING),
                Arguments.of(Order.OrderStatus.PACKED, Order.OrderStatus.PICKED),
                Arguments.of(Order.OrderStatus.PACKED, Order.OrderStatus.CANCELLED),
                Arguments.of(Order.OrderStatus.SHIPPED, Order.OrderStatus.PENDING),
                Arguments.of(Order.OrderStatus.SHIPPED, Order.OrderStatus.CONFIRMED),
                Arguments.of(Order.OrderStatus.SHIPPED, Order.OrderStatus.ALLOCATED),
                Arguments.of(Order.OrderStatus.SHIPPED, Order.OrderStatus.PICKING),
                Arguments.of(Order.OrderStatus.SHIPPED, Order.OrderStatus.PICKED),
                Arguments.of(Order.OrderStatus.SHIPPED, Order.OrderStatus.PACKED),
                Arguments.of(Order.OrderStatus.SHIPPED, Order.OrderStatus.CANCELLED),
                Arguments.of(Order.OrderStatus.SHIPPED, Order.OrderStatus.SHIPPED),
                Arguments.of(Order.OrderStatus.CANCELLED, Order.OrderStatus.PENDING),
                Arguments.of(Order.OrderStatus.CANCELLED, Order.OrderStatus.CONFIRMED),
                Arguments.of(Order.OrderStatus.CANCELLED, Order.OrderStatus.ALLOCATED),
                Arguments.of(Order.OrderStatus.CANCELLED, Order.OrderStatus.PICKING),
                Arguments.of(Order.OrderStatus.CANCELLED, Order.OrderStatus.PICKED),
                Arguments.of(Order.OrderStatus.CANCELLED, Order.OrderStatus.PACKED),
                Arguments.of(Order.OrderStatus.CANCELLED, Order.OrderStatus.SHIPPED),
                Arguments.of(Order.OrderStatus.CANCELLED, Order.OrderStatus.CANCELLED)
        );
    }
}
