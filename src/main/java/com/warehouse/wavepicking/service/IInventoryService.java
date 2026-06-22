package com.warehouse.wavepicking.service;

import com.warehouse.wavepicking.dto.response.InventoryResponse;
import com.warehouse.wavepicking.entity.InventoryBatch;
import com.warehouse.wavepicking.entity.InventoryLock;
import com.warehouse.wavepicking.entity.Order;

import java.time.LocalDateTime;
import java.util.List;

public interface IInventoryService {

    List<InventoryResponse> getAllInventories();

    InventoryResponse getInventoryBySkuId(Long skuId);

    List<InventoryResponse> getOutOfStockItems();

    InventoryResponse createInventory(Long skuId, Integer quantity);

    InventoryResponse addStock(Long skuId, Integer quantity);

    InventoryResponse reduceStock(Long skuId, Integer quantity);

    void lockStock(Long skuId, Integer quantity);

    void unlockStock(Long skuId, Integer quantity);

    void deductLockedStock(Long skuId, Integer quantity);

    boolean hasEnoughStock(Long skuId, Integer quantity);

    List<InventoryLock> lockStockForOrder(Order order);

    void releaseStockForOrder(Order order);

    void deductStockForShippedOrder(Order order);

    InventoryBatch createBatch(Long skuId, String batchNo, Integer quantity, LocalDateTime expiryDate);

    List<InventoryBatch> getBatchesBySkuId(Long skuId);
}
