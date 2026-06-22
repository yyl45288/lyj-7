package com.warehouse.wavepicking.statemachine;

import com.warehouse.wavepicking.entity.Wave;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("波次状态机测试")
class WaveStateMachineTest {

    @Test
    @DisplayName("释放波次 - NEW状态 - 允许")
    void testCanRelease_New_Allowed() {
        StateTransitionResult result = WaveStateMachine.canRelease(Wave.WaveStatus.NEW);
        assertTrue(result.isAllowed());
    }

    @Test
    @DisplayName("释放波次 - RELEASED状态 - 拒绝")
    void testCanRelease_Released_Denied() {
        StateTransitionResult result = WaveStateMachine.canRelease(Wave.WaveStatus.RELEASED);
        assertFalse(result.isAllowed());
        assertEquals("INVALID_STATUS", result.getErrorCode());
        assertEquals("只有新建状态的波次才能释放", result.getErrorMessage());
    }

    @Test
    @DisplayName("释放波次 - COMPLETED状态 - 拒绝")
    void testCanRelease_Completed_Denied() {
        StateTransitionResult result = WaveStateMachine.canRelease(Wave.WaveStatus.COMPLETED);
        assertFalse(result.isAllowed());
        assertEquals("INVALID_STATUS", result.getErrorCode());
    }

    @Test
    @DisplayName("回滚波次 - RELEASED状态 - 允许")
    void testCanRollback_Released_Allowed() {
        StateTransitionResult result = WaveStateMachine.canRollback(Wave.WaveStatus.RELEASED);
        assertTrue(result.isAllowed());
    }

    @Test
    @DisplayName("回滚波次 - PICKING状态 - 允许")
    void testCanRollback_Picking_Allowed() {
        StateTransitionResult result = WaveStateMachine.canRollback(Wave.WaveStatus.PICKING);
        assertTrue(result.isAllowed());
    }

    @Test
    @DisplayName("回滚波次 - NEW状态 - 拒绝")
    void testCanRollback_New_Denied() {
        StateTransitionResult result = WaveStateMachine.canRollback(Wave.WaveStatus.NEW);
        assertFalse(result.isAllowed());
        assertEquals("INVALID_STATUS", result.getErrorCode());
        assertEquals("只有已释放或拣货中的波次才能回滚", result.getErrorMessage());
    }

    @Test
    @DisplayName("回滚波次 - COMPLETED状态 - 拒绝")
    void testCanRollback_Completed_Denied() {
        StateTransitionResult result = WaveStateMachine.canRollback(Wave.WaveStatus.COMPLETED);
        assertFalse(result.isAllowed());
        assertEquals("INVALID_STATUS", result.getErrorCode());
    }

    @Test
    @DisplayName("完成波次 - PICKING状态且任务全部完成 - 允许")
    void testCanComplete_AllTasksDone_Allowed() {
        StateTransitionResult result = WaveStateMachine.canComplete(
                Wave.WaveStatus.PICKING, 5, 5);
        assertTrue(result.isAllowed());
    }

    @Test
    @DisplayName("完成波次 - PICKING状态但任务未完成 - 拒绝")
    void testCanComplete_TasksNotDone_Denied() {
        StateTransitionResult result = WaveStateMachine.canComplete(
                Wave.WaveStatus.PICKING, 3, 5);
        assertFalse(result.isAllowed());
        assertEquals("TASKS_NOT_COMPLETED", result.getErrorCode());
        assertEquals("拣货任务未全部完成，已完成: 3，总计: 5", result.getErrorMessage());
    }

    @Test
    @DisplayName("完成波次 - RELEASED状态 - 拒绝")
    void testCanComplete_Released_Denied() {
        StateTransitionResult result = WaveStateMachine.canComplete(
                Wave.WaveStatus.RELEASED, 5, 5);
        assertFalse(result.isAllowed());
        assertEquals("INVALID_STATUS", result.getErrorCode());
        assertEquals("只有拣货中的波次才能完成", result.getErrorMessage());
    }

    @Test
    @DisplayName("紧急插单 - 波次PICKING，订单已确认无波次且紧急 - 允许")
    void testCanAddUrgentOrder_Allowed() {
        WaveStateMachine.OrderStatusCheck orderCheck = createOrderCheck(true, false, true);
        StateTransitionResult result = WaveStateMachine.canAddUrgentOrder(
                Wave.WaveStatus.PICKING, orderCheck);
        assertTrue(result.isAllowed());
    }

    @Test
    @DisplayName("紧急插单 - 波次已完成 - 拒绝")
    void testCanAddUrgentOrder_WaveCompleted_Denied() {
        WaveStateMachine.OrderStatusCheck orderCheck = createOrderCheck(true, false, true);
        StateTransitionResult result = WaveStateMachine.canAddUrgentOrder(
                Wave.WaveStatus.COMPLETED, orderCheck);
        assertFalse(result.isAllowed());
        assertEquals("INVALID_STATUS", result.getErrorCode());
        assertEquals("波次已完成或已取消，无法插入订单", result.getErrorMessage());
    }

    @Test
    @DisplayName("紧急插单 - 波次已取消 - 拒绝")
    void testCanAddUrgentOrder_WaveCancelled_Denied() {
        WaveStateMachine.OrderStatusCheck orderCheck = createOrderCheck(true, false, true);
        StateTransitionResult result = WaveStateMachine.canAddUrgentOrder(
                Wave.WaveStatus.CANCELLED, orderCheck);
        assertFalse(result.isAllowed());
        assertEquals("INVALID_STATUS", result.getErrorCode());
    }

    @Test
    @DisplayName("紧急插单 - 订单未确认 - 拒绝")
    void testCanAddUrgentOrder_OrderNotConfirmed_Denied() {
        WaveStateMachine.OrderStatusCheck orderCheck = createOrderCheck(false, false, true);
        StateTransitionResult result = WaveStateMachine.canAddUrgentOrder(
                Wave.WaveStatus.PICKING, orderCheck);
        assertFalse(result.isAllowed());
        assertEquals("INVALID_ORDER", result.getErrorCode());
        assertEquals("订单状态不满足紧急插单条件", result.getErrorMessage());
    }

    @Test
    @DisplayName("紧急插单 - 订单已有波次 - 拒绝")
    void testCanAddUrgentOrder_OrderHasWave_Denied() {
        WaveStateMachine.OrderStatusCheck orderCheck = createOrderCheck(true, true, true);
        StateTransitionResult result = WaveStateMachine.canAddUrgentOrder(
                Wave.WaveStatus.PICKING, orderCheck);
        assertFalse(result.isAllowed());
        assertEquals("INVALID_ORDER", result.getErrorCode());
    }

    @Test
    @DisplayName("紧急插单 - 订单非紧急 - 拒绝")
    void testCanAddUrgentOrder_OrderNotUrgent_Denied() {
        WaveStateMachine.OrderStatusCheck orderCheck = createOrderCheck(true, false, false);
        StateTransitionResult result = WaveStateMachine.canAddUrgentOrder(
                Wave.WaveStatus.PICKING, orderCheck);
        assertFalse(result.isAllowed());
        assertEquals("NOT_URGENT", result.getErrorCode());
        assertEquals("只有紧急订单才能插入正在进行的波次", result.getErrorMessage());
    }

    private WaveStateMachine.OrderStatusCheck createOrderCheck(
            boolean confirmed, boolean hasWave, boolean urgent) {
        return new WaveStateMachine.OrderStatusCheck() {
            @Override
            public boolean isConfirmed() {
                return confirmed;
            }

            @Override
            public boolean hasWave() {
                return hasWave;
            }

            @Override
            public boolean isUrgent() {
                return urgent;
            }
        };
    }
}
