package com.warehouse.wavepicking.repository;

import com.warehouse.wavepicking.entity.InventoryLock;
import com.warehouse.wavepicking.entity.InventoryLock.LockStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryLockRepository extends JpaRepository<InventoryLock, Long> {

    List<InventoryLock> findByOrderIdAndStatus(Long orderId, LockStatus status);

    List<InventoryLock> findByBatchIdAndStatus(Long batchId, LockStatus status);

    List<InventoryLock> findByOrderId(Long orderId);

    List<InventoryLock> findByStatus(LockStatus status);
}
