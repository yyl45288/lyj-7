package com.warehouse.wavepicking.statemachine;

import com.warehouse.wavepicking.entity.Order;

public class OrderStateMachine {

    private OrderStateMachine() {
    }

    public static StateTransitionResult canTransition(Order.OrderStatus currentStatus,
                                                      Order.OrderStatus targetStatus) {
        if (!currentStatus.canTransitionTo(targetStatus)) {
            return StateTransitionResult.denied(
                    "INVALID_STATUS_TRANSITION",
                    currentStatus.getTransitionErrorMessage(targetStatus)
            );
        }
        return StateTransitionResult.allowed();
    }

    public static StateTransitionResult canCancel(Order.OrderStatus currentStatus, boolean hasWave) {
        if (!currentStatus.canTransitionTo(Order.OrderStatus.CANCELLED)) {
            return StateTransitionResult.denied(
                    "CANNOT_CANCEL",
                    currentStatus.getTransitionErrorMessage(Order.OrderStatus.CANCELLED)
            );
        }
        if (hasWave) {
            return StateTransitionResult.denied(
                    "CANNOT_CANCEL",
                    "订单已在波次中，无法取消，请先回滚波次"
            );
        }
        return StateTransitionResult.allowed();
    }

    public static StateTransitionResult canConfirm(Order.OrderStatus currentStatus) {
        if (currentStatus == Order.OrderStatus.CONFIRMED) {
            return StateTransitionResult.allowed();
        }
        if (currentStatus != Order.OrderStatus.PENDING) {
            return StateTransitionResult.denied(
                    "INVALID_STATUS",
                    "只有待确认订单才能确认"
            );
        }
        return StateTransitionResult.allowed();
    }

    public static StateTransitionResult canAllocateToWave(Order.OrderStatus currentStatus) {
        return canTransition(currentStatus, Order.OrderStatus.ALLOCATED);
    }

    public static StateTransitionResult canRollbackFromWave(Order.OrderStatus currentStatus) {
        return canTransition(currentStatus, Order.OrderStatus.CONFIRMED);
    }
}
