package com.warehouse.wavepicking.service;

import com.warehouse.wavepicking.dto.response.PickingTaskResponse;
import com.warehouse.wavepicking.entity.*;
import com.warehouse.wavepicking.exception.BusinessException;
import com.warehouse.wavepicking.repository.PickingTaskRepository;
import com.warehouse.wavepicking.statemachine.PickingTaskStateMachine;
import com.warehouse.wavepicking.statemachine.StateTransitionResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class PickingTaskService implements IPickingTaskService {

    private final PickingTaskRepository pickingTaskRepository;
    private final IInventoryService inventoryService;

    public PickingTaskService(PickingTaskRepository pickingTaskRepository, IInventoryService inventoryService) {
        this.pickingTaskRepository = pickingTaskRepository;
        this.inventoryService = inventoryService;
    }

    private static final AtomicInteger taskCounter = new AtomicInteger(0);

    public List<PickingTaskResponse> getAllTasks() {
        return pickingTaskRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<PickingTaskResponse> getTasksByStatus(PickingTask.TaskStatus status) {
        return pickingTaskRepository.findByStatus(status).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<PickingTaskResponse> getTasksByWave(Long waveId) {
        return pickingTaskRepository.findByWaveId(waveId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public PickingTaskResponse getTaskById(Long id) {
        PickingTask task = pickingTaskRepository.findById(id)
                .orElseThrow(() -> new BusinessException("TASK_NOT_FOUND", "拣货任务不存在: " + id));
        return convertToResponse(task);
    }

    public List<PickingTaskResponse> getTasksByPicker(String picker) {
        List<PickingTask.TaskStatus> statuses = Arrays.asList(
                PickingTask.TaskStatus.ASSIGNED,
                PickingTask.TaskStatus.PICKING
        );
        return pickingTaskRepository.findTasksByPickerAndStatusIn(picker, statuses).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void generatePickingTasks(Wave wave, List<Order> orders) {
        Map<Long, Integer> skuQuantityMap = new HashMap<>();
        Map<Long, Sku> skuMap = new HashMap<>();
        Map<Long, String> skuLocationMap = new HashMap<>();

        for (Order order : orders) {
            for (OrderItem item : order.getItems()) {
                Long skuId = item.getSku().getId();
                skuQuantityMap.merge(skuId, item.getQuantity(), Integer::sum);
                skuMap.put(skuId, item.getSku());
                skuLocationMap.put(skuId, item.getLocation());
            }
        }

        for (Map.Entry<Long, Integer> entry : skuQuantityMap.entrySet()) {
            Sku sku = skuMap.get(entry.getKey());
            PickingTask task = new PickingTask();
            task.setTaskNo(generateTaskNo());
            task.setWave(wave);
            task.setSku(sku);
            task.setQuantity(entry.getValue());
            task.setPickedQuantity(0);
            task.setLocation(skuLocationMap.get(entry.getKey()));
            task.setStatus(PickingTask.TaskStatus.PENDING);
            task.setPriority(wave.getWaveType() == Wave.WaveType.URGENT ? 10 : 5);
            task.setZone(sku.getLocation() != null ? extractZone(sku.getLocation()) : null);

            pickingTaskRepository.save(task);
        }
    }

    @Transactional
    public void generatePickingTasksForOrder(Wave wave, Order order) {
        Map<Long, Integer> existingQuantities = new HashMap<>();
        List<PickingTask> existingTasks = pickingTaskRepository.findByWaveId(wave.getId());
        for (PickingTask task : existingTasks) {
            existingQuantities.merge(task.getSku().getId(), task.getQuantity(), Integer::sum);
        }

        for (OrderItem item : order.getItems()) {
            Long skuId = item.getSku().getId();
            if (existingQuantities.containsKey(skuId)) {
                PickingTask existingTask = existingTasks.stream()
                        .filter(t -> t.getSku().getId().equals(skuId))
                        .findFirst()
                        .orElse(null);
                if (existingTask != null) {
                    existingTask.setQuantity(existingTask.getQuantity() + item.getQuantity());
                    pickingTaskRepository.save(existingTask);
                }
            } else {
                PickingTask task = new PickingTask();
                task.setTaskNo(generateTaskNo());
                task.setWave(wave);
                task.setSku(item.getSku());
                task.setQuantity(item.getQuantity());
                task.setPickedQuantity(0);
                task.setLocation(item.getLocation());
                task.setStatus(PickingTask.TaskStatus.PENDING);
                task.setPriority(10);
                task.setZone(item.getLocation() != null ? extractZone(item.getLocation()) : null);

                pickingTaskRepository.save(task);
            }
        }
    }

    @Transactional
    public PickingTaskResponse assignTask(Long taskId, String picker) {
        PickingTask task = pickingTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException("TASK_NOT_FOUND", "拣货任务不存在: " + taskId));

        StateTransitionResult result = PickingTaskStateMachine.canAssign(task.getStatus());
        throwIfNotAllowed(result);

        task.setPicker(picker);
        task.setStatus(PickingTask.TaskStatus.ASSIGNED);

        PickingTask saved = pickingTaskRepository.save(task);
        return convertToResponse(saved);
    }

    @Transactional
    public PickingTaskResponse startTask(Long taskId) {
        PickingTask task = pickingTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException("TASK_NOT_FOUND", "拣货任务不存在: " + taskId));

        StateTransitionResult result = PickingTaskStateMachine.canStart(task.getStatus());
        throwIfNotAllowed(result);

        task.setStatus(PickingTask.TaskStatus.PICKING);
        task.setStartedAt(LocalDateTime.now());

        PickingTask saved = pickingTaskRepository.save(task);
        return convertToResponse(saved);
    }

    @Transactional
    public PickingTaskResponse completeTask(Long taskId, Integer pickedQuantity) {
        PickingTask task = pickingTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException("TASK_NOT_FOUND", "拣货任务不存在: " + taskId));

        StateTransitionResult result = PickingTaskStateMachine.canComplete(
                task.getStatus(), pickedQuantity, task.getQuantity());
        throwIfNotAllowed(result);

        task.setPickedQuantity(pickedQuantity);
        task.setStatus(PickingTask.TaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());

        inventoryService.deductLockedStock(task.getSku().getId(), pickedQuantity);

        if (pickedQuantity < task.getQuantity()) {
            int shortQty = task.getQuantity() - pickedQuantity;
            inventoryService.unlockStock(task.getSku().getId(), shortQty);
        }

        PickingTask saved = pickingTaskRepository.save(task);
        return convertToResponse(saved);
    }

    @Transactional
    public void cancelTasksByWave(Long waveId) {
        List<PickingTask> tasks = pickingTaskRepository.findByWaveId(waveId);
        for (PickingTask task : tasks) {
            if (PickingTaskStateMachine.canCancel(task.getStatus())) {
                task.setStatus(PickingTask.TaskStatus.CANCELLED);
                pickingTaskRepository.save(task);
            }
        }
    }

    private void throwIfNotAllowed(StateTransitionResult result) {
        if (!result.isAllowed()) {
            throw new BusinessException(result.getErrorCode(), result.getErrorMessage());
        }
    }

    public long countCompletedTasksByWave(Long waveId) {
        return pickingTaskRepository.countByWaveIdAndStatus(waveId, PickingTask.TaskStatus.COMPLETED);
    }

    public long countTotalTasksByWave(Long waveId) {
        return pickingTaskRepository.findByWaveId(waveId).size();
    }

    public long countByStatus(PickingTask.TaskStatus status) {
        return pickingTaskRepository.countByStatus(status);
    }

    private String generateTaskNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int seq = taskCounter.incrementAndGet();
        return "TASK" + date + String.format("%06d", seq);
    }

    private String extractZone(String location) {
        if (location == null || location.isEmpty()) {
            return null;
        }
        return location.substring(0, Math.min(2, location.length()));
    }

    private PickingTaskResponse convertToResponse(PickingTask task) {
        return PickingTaskResponse.builder()
                .id(task.getId())
                .taskNo(task.getTaskNo())
                .waveId(task.getWave() != null ? task.getWave().getId() : null)
                .waveNo(task.getWave() != null ? task.getWave().getWaveNo() : null)
                .skuId(task.getSku() != null ? task.getSku().getId() : null)
                .skuCode(task.getSku() != null ? task.getSku().getSkuCode() : null)
                .skuName(task.getSku() != null ? task.getSku().getSkuName() : null)
                .quantity(task.getQuantity())
                .pickedQuantity(task.getPickedQuantity())
                .location(task.getLocation())
                .status(task.getStatus().name())
                .picker(task.getPicker())
                .zone(task.getZone())
                .priority(task.getPriority())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .startedAt(task.getStartedAt())
                .completedAt(task.getCompletedAt())
                .build();
    }
}
