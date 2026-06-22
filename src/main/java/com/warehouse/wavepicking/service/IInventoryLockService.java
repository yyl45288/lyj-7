package com.warehouse.wavepicking.service;

import com.warehouse.wavepicking.entity.InventoryLock;
import com.warehouse.wavepicking.entity.Order;

import java.util.List;

public interface IInventoryLockService {

    boolean hasEnoughStock(Long skuId, Integer quantity);

    List<InventoryLock> lockStockForOrder(Order order);

    void releaseStockForCancelledOrder(Order order);

    void deductStockForShippedOrder(Order order);

    void rollbackStockFromPickingStage(Order order);

    List<InventoryLock> getLocksByOrderId(Long orderId);

    List<InventoryLock> getActiveLocksByOrderId(Long orderId);
}
