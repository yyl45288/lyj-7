package com.warehouse.wavepicking.repository;

import com.warehouse.wavepicking.entity.InventoryBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;

@Repository
public interface InventoryBatchRepository extends JpaRepository<InventoryBatch, Long> {

    List<InventoryBatch> findBySkuId(Long skuId);

    @Query("SELECT b FROM InventoryBatch b WHERE b.sku.id = :skuId AND b.expiryDate > CURRENT_TIMESTAMP AND b.availableQuantity > 0 ORDER BY b.expiryDate ASC")
    List<InventoryBatch> findAvailableBatchesBySkuIdOrderByExpiryDate(Long skuId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM InventoryBatch b WHERE b.id = :id")
    InventoryBatch findByIdWithLock(Long id);

    @Query("SELECT b FROM InventoryBatch b WHERE b.sku.id = :skuId ORDER BY b.expiryDate ASC")
    List<InventoryBatch> findBySkuIdOrderByExpiryDate(Long skuId);
}
