package com.warehouse.wavepicking.statemachine;

import com.warehouse.wavepicking.entity.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("订单状态机测试")
class OrderStateMachineTest {

    @ParameterizedTest
    @MethodSource("validTransitions")
    @DisplayName("合法状态转换 - 应该允许")
    void testCanTransition_Valid(Order.OrderStatus from, Order.OrderStatus to) {
        StateTransitionResult result = OrderStateMachine.canTransition(from, to);
        assertTrue(result.isAllowed(),
                String.format("状态 %s 应该可以转换为 %s", from, to));
    }

    @ParameterizedTest
    @MethodSource("invalidTransitions")
    @DisplayName("非法状态转换 - 应该拒绝")
    void testCanTransition_Invalid(Order.OrderStatus from, Order.OrderStatus to) {
        StateTransitionResult result = OrderStateMachine.canTransition(from, to);
        assertFalse(result.isAllowed(),
                String.format("状态 %s 不应该可以转换为 %s", from, to));
        assertEquals("INVALID_STATUS_TRANSITION", result.getErrorCode());
    }

    @Test
    @DisplayName("取消订单 - 状态合法且无波次 - 允许")
    void testCanCancel_Allowed() {
        StateTransitionResult result = OrderStateMachine.canCancel(
                Order.OrderStatus.CONFIRMED, false);
        assertTrue(result.isAllowed());
    }

    @Test
    @DisplayName("取消订单 - 状态合法但有波次 - 拒绝")
    void testCanCancel_HasWave_Denied() {
        StateTransitionResult result = OrderStateMachine.canCancel(
                Order.OrderStatus.CONFIRMED, true);
        assertFalse(result.isAllowed());
        assertEquals("CANNOT_CANCEL", result.getErrorCode());
        assertEquals("订单已在波次中，无法取消，请先回滚波次", result.getErrorMessage());
    }

    @Test
    @DisplayName("取消订单 - 状态不合法 - 拒绝（与原代码行为保持一致，错误码为CANNOT_CANCEL）")
    void testCanCancel_InvalidStatus_Denied() {
        StateTransitionResult result = OrderStateMachine.canCancel(
                Order.OrderStatus.SHIPPED, false);
        assertFalse(result.isAllowed());
        assertEquals("CANNOT_CANCEL", result.getErrorCode());
    }

    @Test
    @DisplayName("确认订单 - PENDING状态 - 允许")
    void testCanConfirm_Pending_Allowed() {
        StateTransitionResult result = OrderStateMachine.canConfirm(Order.OrderStatus.PENDING);
        assertTrue(result.isAllowed());
    }

    @Test
    @DisplayName("确认订单 - CONFIRMED状态 - 允许（幂等）")
    void testCanConfirm_Confirmed_Allowed() {
        StateTransitionResult result = OrderStateMachine.canConfirm(Order.OrderStatus.CONFIRMED);
        assertTrue(result.isAllowed());
    }

    @Test
    @DisplayName("确认订单 - ALLOCATED状态 - 拒绝")
    void testCanConfirm_Allocated_Denied() {
        StateTransitionResult result = OrderStateMachine.canConfirm(Order.OrderStatus.ALLOCATED);
        assertFalse(result.isAllowed());
        assertEquals("INVALID_STATUS", result.getErrorCode());
        assertEquals("只有待确认订单才能确认", result.getErrorMessage());
    }

    @Test
    @DisplayName("分配到波次 - CONFIRMED状态 - 允许")
    void testCanAllocateToWave_Confirmed_Allowed() {
        StateTransitionResult result = OrderStateMachine.canAllocateToWave(Order.OrderStatus.CONFIRMED);
        assertTrue(result.isAllowed());
    }

    @Test
    @DisplayName("分配到波次 - PENDING状态 - 拒绝")
    void testCanAllocateToWave_Pending_Denied() {
        StateTransitionResult result = OrderStateMachine.canAllocateToWave(Order.OrderStatus.PENDING);
        assertFalse(result.isAllowed());
        assertEquals("INVALID_STATUS_TRANSITION", result.getErrorCode());
    }

    @Test
    @DisplayName("从波次回滚 - ALLOCATED状态 - 允许")
    void testCanRollbackFromWave_Allocated_Allowed() {
        StateTransitionResult result = OrderStateMachine.canRollbackFromWave(Order.OrderStatus.ALLOCATED);
        assertTrue(result.isAllowed());
    }

    @Test
    @DisplayName("从波次回滚 - PICKING状态 - 拒绝")
    void testCanRollbackFromWave_Picking_Denied() {
        StateTransitionResult result = OrderStateMachine.canRollbackFromWave(Order.OrderStatus.PICKING);
        assertFalse(result.isAllowed());
        assertEquals("INVALID_STATUS_TRANSITION", result.getErrorCode());
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
                Arguments.of(Order.OrderStatus.PENDING, Order.OrderStatus.ALLOCATED),
                Arguments.of(Order.OrderStatus.PENDING, Order.OrderStatus.PICKING),
                Arguments.of(Order.OrderStatus.CONFIRMED, Order.OrderStatus.PICKING),
                Arguments.of(Order.OrderStatus.PICKING, Order.OrderStatus.PACKED),
                Arguments.of(Order.OrderStatus.SHIPPED, Order.OrderStatus.PENDING),
                Arguments.of(Order.OrderStatus.CANCELLED, Order.OrderStatus.CONFIRMED)
        );
    }
}
