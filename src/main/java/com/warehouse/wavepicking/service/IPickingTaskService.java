package com.warehouse.wavepicking.service;

import com.warehouse.wavepicking.dto.response.PickingTaskResponse;
import com.warehouse.wavepicking.entity.Order;
import com.warehouse.wavepicking.entity.PickingTask;
import com.warehouse.wavepicking.entity.Wave;

import java.util.List;

public interface IPickingTaskService {

    List<PickingTaskResponse> getAllTasks();

    List<PickingTaskResponse> getTasksByStatus(PickingTask.TaskStatus status);

    List<PickingTaskResponse> getTasksByWave(Long waveId);

    PickingTaskResponse getTaskById(Long id);

    List<PickingTaskResponse> getTasksByPicker(String picker);

    void generatePickingTasks(Wave wave, List<Order> orders);

    void generatePickingTasksForOrder(Wave wave, Order order);

    PickingTaskResponse assignTask(Long taskId, String picker);

    PickingTaskResponse startTask(Long taskId);

    PickingTaskResponse completeTask(Long taskId, Integer pickedQuantity);

    void cancelTasksByWave(Long waveId);

    long countCompletedTasksByWave(Long waveId);

    long countTotalTasksByWave(Long waveId);

    long countByStatus(PickingTask.TaskStatus status);
}
