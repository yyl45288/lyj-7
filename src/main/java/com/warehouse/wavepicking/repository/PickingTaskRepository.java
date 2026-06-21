package com.warehouse.wavepicking.repository;

import com.warehouse.wavepicking.entity.PickingTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PickingTaskRepository extends JpaRepository<PickingTask, Long> {

    Optional<PickingTask> findByTaskNo(String taskNo);

    List<PickingTask> findByWaveId(Long waveId);

    List<PickingTask> findByStatus(PickingTask.TaskStatus status);

    List<PickingTask> findByPickerAndStatus(String picker, PickingTask.TaskStatus status);

    @Query("SELECT pt FROM PickingTask pt WHERE pt.picker = :picker AND pt.status IN :statuses ORDER BY pt.priority DESC, pt.createdAt ASC")
    List<PickingTask> findTasksByPickerAndStatusIn(String picker, List<PickingTask.TaskStatus> statuses);

    @Query("SELECT COUNT(pt) FROM PickingTask pt WHERE pt.status = :status")
    long countByStatus(PickingTask.TaskStatus status);

    @Query("SELECT COUNT(pt) FROM PickingTask pt WHERE pt.wave.id = :waveId AND pt.status = :status")
    long countByWaveIdAndStatus(Long waveId, PickingTask.TaskStatus status);

    boolean existsByTaskNo(String taskNo);
}
