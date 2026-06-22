package com.warehouse.wavepicking.statemachine;

import com.warehouse.wavepicking.entity.PickingTask;

public class PickingTaskStateMachine {

    private PickingTaskStateMachine() {
    }

    public static StateTransitionResult canAssign(PickingTask.TaskStatus currentStatus) {
        if (currentStatus != PickingTask.TaskStatus.PENDING) {
            return StateTransitionResult.denied(
                    "INVALID_STATUS",
                    "只有待分配的任务才能分配拣货员"
            );
        }
        return StateTransitionResult.allowed();
    }

    public static StateTransitionResult canStart(PickingTask.TaskStatus currentStatus) {
        if (currentStatus != PickingTask.TaskStatus.ASSIGNED) {
            return StateTransitionResult.denied(
                    "INVALID_STATUS",
                    "只有已分配的任务才能开始拣货"
            );
        }
        return StateTransitionResult.allowed();
    }

    public static StateTransitionResult canComplete(PickingTask.TaskStatus currentStatus,
                                                    Integer pickedQuantity, Integer taskQuantity) {
        if (currentStatus != PickingTask.TaskStatus.PICKING
                && currentStatus != PickingTask.TaskStatus.ASSIGNED) {
            return StateTransitionResult.denied(
                    "INVALID_STATUS",
                    "任务状态不正确，无法完成"
            );
        }
        if (pickedQuantity == null || pickedQuantity < 0) {
            return StateTransitionResult.denied(
                    "INVALID_QUANTITY",
                    "拣货数量无效"
            );
        }
        if (pickedQuantity > taskQuantity) {
            return StateTransitionResult.denied(
                    "EXCEED_QUANTITY",
                    "拣货数量不能超过任务数量"
            );
        }
        return StateTransitionResult.allowed();
    }

    public static boolean canCancel(PickingTask.TaskStatus currentStatus) {
        return currentStatus != PickingTask.TaskStatus.COMPLETED;
    }
}
