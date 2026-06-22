package com.warehouse.wavepicking.statemachine;

import com.warehouse.wavepicking.entity.Wave;

public class WaveStateMachine {

    private WaveStateMachine() {
    }

    public static StateTransitionResult canRelease(Wave.WaveStatus currentStatus) {
        if (currentStatus != Wave.WaveStatus.NEW) {
            return StateTransitionResult.denied(
                    "INVALID_STATUS",
                    "只有新建状态的波次才能释放"
            );
        }
        return StateTransitionResult.allowed();
    }

    public static StateTransitionResult canRollback(Wave.WaveStatus currentStatus) {
        if (currentStatus != Wave.WaveStatus.RELEASED && currentStatus != Wave.WaveStatus.PICKING) {
            return StateTransitionResult.denied(
                    "INVALID_STATUS",
                    "只有已释放或拣货中的波次才能回滚"
            );
        }
        return StateTransitionResult.allowed();
    }

    public static StateTransitionResult canComplete(Wave.WaveStatus currentStatus,
                                                    long completedTasks, long totalTasks) {
        if (currentStatus != Wave.WaveStatus.PICKING) {
            return StateTransitionResult.denied(
                    "INVALID_STATUS",
                    "只有拣货中的波次才能完成"
            );
        }
        if (completedTasks < totalTasks) {
            return StateTransitionResult.denied(
                    "TASKS_NOT_COMPLETED",
                    "拣货任务未全部完成，已完成: " + completedTasks + "，总计: " + totalTasks
            );
        }
        return StateTransitionResult.allowed();
    }

    public static StateTransitionResult canAddUrgentOrder(Wave.WaveStatus currentStatus,
                                                          OrderStatusCheck orderStatusCheck) {
        if (currentStatus == Wave.WaveStatus.COMPLETED || currentStatus == Wave.WaveStatus.CANCELLED) {
            return StateTransitionResult.denied(
                    "INVALID_STATUS",
                    "波次已完成或已取消，无法插入订单"
            );
        }
        if (!orderStatusCheck.isConfirmed() || orderStatusCheck.hasWave()) {
            return StateTransitionResult.denied(
                    "INVALID_ORDER",
                    "订单状态不满足紧急插单条件"
            );
        }
        if (!orderStatusCheck.isUrgent()) {
            return StateTransitionResult.denied(
                    "NOT_URGENT",
                    "只有紧急订单才能插入正在进行的波次"
            );
        }
        return StateTransitionResult.allowed();
    }

    public interface OrderStatusCheck {
        boolean isConfirmed();
        boolean hasWave();
        boolean isUrgent();
    }
}
