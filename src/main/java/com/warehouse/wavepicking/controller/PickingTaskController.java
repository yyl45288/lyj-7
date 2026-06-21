package com.warehouse.wavepicking.controller;

import com.warehouse.wavepicking.common.ApiResponse;
import com.warehouse.wavepicking.dto.request.AssignTaskRequest;
import com.warehouse.wavepicking.dto.request.CompleteTaskRequest;
import com.warehouse.wavepicking.dto.response.PickingTaskResponse;
import com.warehouse.wavepicking.entity.PickingTask;
import com.warehouse.wavepicking.service.PickingTaskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/picking-tasks")
public class PickingTaskController {

    private final PickingTaskService pickingTaskService;

    public PickingTaskController(PickingTaskService pickingTaskService) {
        this.pickingTaskService = pickingTaskService;
    }

    @GetMapping
    public ApiResponse<List<PickingTaskResponse>> getAllTasks() {
        return ApiResponse.success(pickingTaskService.getAllTasks());
    }

    @GetMapping("/status/{status}")
    public ApiResponse<List<PickingTaskResponse>> getTasksByStatus(@PathVariable String status) {
        PickingTask.TaskStatus taskStatus = PickingTask.TaskStatus.valueOf(status.toUpperCase());
        return ApiResponse.success(pickingTaskService.getTasksByStatus(taskStatus));
    }

    @GetMapping("/wave/{waveId}")
    public ApiResponse<List<PickingTaskResponse>> getTasksByWave(@PathVariable Long waveId) {
        return ApiResponse.success(pickingTaskService.getTasksByWave(waveId));
    }

    @GetMapping("/{id}")
    public ApiResponse<PickingTaskResponse> getTaskById(@PathVariable Long id) {
        return ApiResponse.success(pickingTaskService.getTaskById(id));
    }

    @GetMapping("/picker/{picker}")
    public ApiResponse<List<PickingTaskResponse>> getTasksByPicker(@PathVariable String picker) {
        return ApiResponse.success(pickingTaskService.getTasksByPicker(picker));
    }

    @PutMapping("/{id}/assign")
    public ApiResponse<PickingTaskResponse> assignTask(@PathVariable Long id,
                                                        @Valid @RequestBody AssignTaskRequest request) {
        return ApiResponse.success(pickingTaskService.assignTask(id, request.getPicker()));
    }

    @PutMapping("/{id}/start")
    public ApiResponse<PickingTaskResponse> startTask(@PathVariable Long id) {
        return ApiResponse.success(pickingTaskService.startTask(id));
    }

    @PutMapping("/{id}/complete")
    public ApiResponse<PickingTaskResponse> completeTask(@PathVariable Long id,
                                                          @Valid @RequestBody CompleteTaskRequest request) {
        return ApiResponse.success(pickingTaskService.completeTask(id, request.getPickedQuantity()));
    }
}
