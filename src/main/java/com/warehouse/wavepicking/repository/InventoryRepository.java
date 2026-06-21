package com.warehouse.wavepicking.repository;

import com.warehouse.wavepicking.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findBySkuId(Long skuId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.sku.id = :skuId")
    Optional<Inventory> findBySkuIdWithLock(Long skuId);

    @Query("SELECT i FROM Inventory i WHERE i.availableQuantity <= i.safetyStock")
    List<Inventory> findOutOfStockItems();

    @Query("SELECT i FROM Inventory i WHERE i.availableQuantity < :quantity")
    List<Inventory> findItemsWithInsufficientStock(Integer quantity);
}
