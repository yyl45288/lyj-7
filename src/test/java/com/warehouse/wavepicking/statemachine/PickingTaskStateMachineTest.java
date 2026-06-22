package com.warehouse.wavepicking.statemachine;

import com.warehouse.wavepicking.entity.PickingTask;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("拣货任务状态机测试")
class PickingTaskStateMachineTest {

    @Test
    @DisplayName("分配任务 - PENDING状态 - 允许")
    void testCanAssign_Pending_Allowed() {
        StateTransitionResult result = PickingTaskStateMachine.canAssign(
                PickingTask.TaskStatus.PENDING);
        assertTrue(result.isAllowed());
    }

    @Test
    @DisplayName("分配任务 - ASSIGNED状态 - 拒绝")
    void testCanAssign_Assigned_Denied() {
        StateTransitionResult result = PickingTaskStateMachine.canAssign(
                PickingTask.TaskStatus.ASSIGNED);
        assertFalse(result.isAllowed());
        assertEquals("INVALID_STATUS", result.getErrorCode());
        assertEquals("只有待分配的任务才能分配拣货员", result.getErrorMessage());
    }

    @Test
    @DisplayName("分配任务 - COMPLETED状态 - 拒绝")
    void testCanAssign_Completed_Denied() {
        StateTransitionResult result = PickingTaskStateMachine.canAssign(
                PickingTask.TaskStatus.COMPLETED);
        assertFalse(result.isAllowed());
        assertEquals("INVALID_STATUS", result.getErrorCode());
    }

    @Test
    @DisplayName("开始任务 - ASSIGNED状态 - 允许")
    void testCanStart_Assigned_Allowed() {
        StateTransitionResult result = PickingTaskStateMachine.canStart(
                PickingTask.TaskStatus.ASSIGNED);
        assertTrue(result.isAllowed());
    }

    @Test
    @DisplayName("开始任务 - PENDING状态 - 拒绝")
    void testCanStart_Pending_Denied() {
        StateTransitionResult result = PickingTaskStateMachine.canStart(
                PickingTask.TaskStatus.PENDING);
        assertFalse(result.isAllowed());
        assertEquals("INVALID_STATUS", result.getErrorCode());
        assertEquals("只有已分配的任务才能开始拣货", result.getErrorMessage());
    }

    @Test
    @DisplayName("开始任务 - PICKING状态 - 拒绝")
    void testCanStart_Picking_Denied() {
        StateTransitionResult result = PickingTaskStateMachine.canStart(
                PickingTask.TaskStatus.PICKING);
        assertFalse(result.isAllowed());
        assertEquals("INVALID_STATUS", result.getErrorCode());
    }

    @Test
    @DisplayName("完成任务 - PICKING状态，数量正确 - 允许")
    void testCanComplete_Picking_ValidQuantity_Allowed() {
        StateTransitionResult result = PickingTaskStateMachine.canComplete(
                PickingTask.TaskStatus.PICKING, 10, 10);
        assertTrue(result.isAllowed());
    }

    @Test
    @DisplayName("完成任务 - ASSIGNED状态，数量正确 - 允许")
    void testCanComplete_Assigned_ValidQuantity_Allowed() {
        StateTransitionResult result = PickingTaskStateMachine.canComplete(
                PickingTask.TaskStatus.ASSIGNED, 10, 10);
        assertTrue(result.isAllowed());
    }

    @Test
    @DisplayName("完成任务 - PENDING状态 - 拒绝")
    void testCanComplete_Pending_Denied() {
        StateTransitionResult result = PickingTaskStateMachine.canComplete(
                PickingTask.TaskStatus.PENDING, 10, 10);
        assertFalse(result.isAllowed());
        assertEquals("INVALID_STATUS", result.getErrorCode());
        assertEquals("任务状态不正确，无法完成", result.getErrorMessage());
    }

    @Test
    @DisplayName("完成任务 - 拣货数量为null - 拒绝")
    void testCanComplete_NullQuantity_Denied() {
        StateTransitionResult result = PickingTaskStateMachine.canComplete(
                PickingTask.TaskStatus.PICKING, null, 10);
        assertFalse(result.isAllowed());
        assertEquals("INVALID_QUANTITY", result.getErrorCode());
        assertEquals("拣货数量无效", result.getErrorMessage());
    }

    @Test
    @DisplayName("完成任务 - 拣货数量为负 - 拒绝")
    void testCanComplete_NegativeQuantity_Denied() {
        StateTransitionResult result = PickingTaskStateMachine.canComplete(
                PickingTask.TaskStatus.PICKING, -1, 10);
        assertFalse(result.isAllowed());
        assertEquals("INVALID_QUANTITY", result.getErrorCode());
    }

    @Test
    @DisplayName("完成任务 - 拣货数量超过任务数量 - 拒绝")
    void testCanComplete_ExceedQuantity_Denied() {
        StateTransitionResult result = PickingTaskStateMachine.canComplete(
                PickingTask.TaskStatus.PICKING, 11, 10);
        assertFalse(result.isAllowed());
        assertEquals("EXCEED_QUANTITY", result.getErrorCode());
        assertEquals("拣货数量不能超过任务数量", result.getErrorMessage());
    }

    @Test
    @DisplayName("完成任务 - 部分拣货（数量小于任务量）- 允许")
    void testCanComplete_PartialQuantity_Allowed() {
        StateTransitionResult result = PickingTaskStateMachine.canComplete(
                PickingTask.TaskStatus.PICKING, 8, 10);
        assertTrue(result.isAllowed());
    }

    @Test
    @DisplayName("取消任务 - PENDING状态 - 允许")
    void testCanCancel_Pending_Allowed() {
        assertTrue(PickingTaskStateMachine.canCancel(PickingTask.TaskStatus.PENDING));
    }

    @Test
    @DisplayName("取消任务 - ASSIGNED状态 - 允许")
    void testCanCancel_Assigned_Allowed() {
        assertTrue(PickingTaskStateMachine.canCancel(PickingTask.TaskStatus.ASSIGNED));
    }

    @Test
    @DisplayName("取消任务 - PICKING状态 - 允许")
    void testCanCancel_Picking_Allowed() {
        assertTrue(PickingTaskStateMachine.canCancel(PickingTask.TaskStatus.PICKING));
    }

    @Test
    @DisplayName("取消任务 - COMPLETED状态 - 不允许")
    void testCanCancel_Completed_NotAllowed() {
        assertFalse(PickingTaskStateMachine.canCancel(PickingTask.TaskStatus.COMPLETED));
    }
}
