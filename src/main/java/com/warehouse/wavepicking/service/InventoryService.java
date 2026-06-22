package com.warehouse.wavepicking.service;

import com.warehouse.wavepicking.dto.response.InventoryResponse;
import com.warehouse.wavepicking.entity.*;
import com.warehouse.wavepicking.entity.InventoryLock.LockStatus;
import com.warehouse.wavepicking.exception.BusinessException;
import com.warehouse.wavepicking.repository.InventoryBatchRepository;
import com.warehouse.wavepicking.repository.InventoryLockRepository;
import com.warehouse.wavepicking.repository.InventoryRepository;
import com.warehouse.wavepicking.repository.SkuRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InventoryService implements IInventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryBatchRepository inventoryBatchRepository;
    private final InventoryLockRepository inventoryLockRepository;
    private final SkuRepository skuRepository;

    public InventoryService(InventoryRepository inventoryRepository,
                            InventoryBatchRepository inventoryBatchRepository,
                            InventoryLockRepository inventoryLockRepository,
                            SkuRepository skuRepository) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryBatchRepository = inventoryBatchRepository;
        this.inventoryLockRepository = inventoryLockRepository;
        this.skuRepository = skuRepository;
    }

    public List<InventoryResponse> getAllInventories() {
        return inventoryRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public InventoryResponse getInventoryBySkuId(Long skuId) {
        Inventory inventory = inventoryRepository.findBySkuId(skuId)
                .orElseThrow(() -> new BusinessException("INVENTORY_NOT_FOUND", "库存不存在，SKU ID: " + skuId));
        return convertToResponse(inventory);
    }

    public List<InventoryResponse> getOutOfStockItems() {
        return inventoryRepository.findOutOfStockItems().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public InventoryResponse createInventory(Long skuId, Integer quantity) {
        Sku sku = skuRepository.findById(skuId)
                .orElseThrow(() -> new BusinessException("SKU_NOT_FOUND", "SKU不存在: " + skuId));

        if (inventoryRepository.findBySkuId(skuId).isPresent()) {
            throw new BusinessException("INVENTORY_EXISTS", "该SKU库存已存在");
        }

        Inventory inventory = new Inventory();
        inventory.setSku(sku);
        inventory.setTotalQuantity(quantity);
        inventory.setAvailableQuantity(quantity);
        inventory.setLockedQuantity(0);
        inventory.setSafetyStock(10);

        Inventory saved = inventoryRepository.save(inventory);
        return convertToResponse(saved);
    }

    @Transactional
    public InventoryResponse addStock(Long skuId, Integer quantity) {
        Inventory inventory = inventoryRepository.findBySkuIdWithLock(skuId)
                .orElseThrow(() -> new BusinessException("INVENTORY_NOT_FOUND", "库存不存在，SKU ID: " + skuId));

        inventory.setTotalQuantity(inventory.getTotalQuantity() + quantity);
        inventory.setAvailableQuantity(inventory.getAvailableQuantity() + quantity);

        Inventory saved = inventoryRepository.save(inventory);
        return convertToResponse(saved);
    }

    @Transactional
    public InventoryResponse reduceStock(Long skuId, Integer quantity) {
        Inventory inventory = inventoryRepository.findBySkuIdWithLock(skuId)
                .orElseThrow(() -> new BusinessException("INVENTORY_NOT_FOUND", "库存不存在，SKU ID: " + skuId));

        if (inventory.getAvailableQuantity() < quantity) {
            throw new BusinessException("INSUFFICIENT_STOCK", "可用库存不足，SKU: " + inventory.getSku().getSkuCode());
        }

        inventory.setTotalQuantity(inventory.getTotalQuantity() - quantity);
        inventory.setAvailableQuantity(inventory.getAvailableQuantity() - quantity);

        Inventory saved = inventoryRepository.save(inventory);
        return convertToResponse(saved);
    }

    @Transactional
    public void lockStock(Long skuId, Integer quantity) {
        Inventory inventory = inventoryRepository.findBySkuIdWithLock(skuId)
                .orElseThrow(() -> new BusinessException("INVENTORY_NOT_FOUND", "库存不存在，SKU ID: " + skuId));

        if (inventory.getAvailableQuantity() < quantity) {
            throw new BusinessException("INSUFFICIENT_STOCK",
                    "可用库存不足，无法锁定。SKU: " + inventory.getSku().getSkuCode() +
                            "，可用: " + inventory.getAvailableQuantity() +
                            "，需要: " + quantity);
        }

        inventory.setAvailableQuantity(inventory.getAvailableQuantity() - quantity);
        inventory.setLockedQuantity(inventory.getLockedQuantity() + quantity);

        inventoryRepository.save(inventory);
    }

    @Transactional
    public void unlockStock(Long skuId, Integer quantity) {
        Inventory inventory = inventoryRepository.findBySkuIdWithLock(skuId)
                .orElseThrow(() -> new BusinessException("INVENTORY_NOT_FOUND", "库存不存在，SKU ID: " + skuId));

        if (inventory.getLockedQuantity() < quantity) {
            throw new BusinessException("UNLOCK_ERROR",
                    "锁定库存不足，无法解锁。锁定: " + inventory.getLockedQuantity() + "，需要解锁: " + quantity);
        }

        inventory.setAvailableQuantity(inventory.getAvailableQuantity() + quantity);
        inventory.setLockedQuantity(inventory.getLockedQuantity() - quantity);

        inventoryRepository.save(inventory);
    }

    @Transactional
    public void deductLockedStock(Long skuId, Integer quantity) {
        Inventory inventory = inventoryRepository.findBySkuIdWithLock(skuId)
                .orElseThrow(() -> new BusinessException("INVENTORY_NOT_FOUND", "库存不存在，SKU ID: " + skuId));

        if (inventory.getLockedQuantity() < quantity) {
            throw new BusinessException("DEDUCT_ERROR",
                    "锁定库存不足，无法扣减。锁定: " + inventory.getLockedQuantity() + "，需要扣减: " + quantity);
        }

        inventory.setLockedQuantity(inventory.getLockedQuantity() - quantity);
        inventory.setTotalQuantity(inventory.getTotalQuantity() - quantity);

        inventoryRepository.save(inventory);
    }

    public boolean hasEnoughStock(Long skuId, Integer quantity) {
        List<InventoryBatch> batches = inventoryBatchRepository
                .findAvailableBatchesBySkuIdOrderByExpiryDate(skuId);
        int totalAvailable = batches.stream()
                .mapToInt(InventoryBatch::getAvailableQuantity)
                .sum();
        return totalAvailable >= quantity;
    }

    @Transactional
    public List<InventoryLock> lockStockForOrder(Order order) {
        List<InventoryLock> createdLocks = new ArrayList<>();

        try {
            for (OrderItem item : order.getItems()) {
                Long skuId = item.getSku().getId();
                int requiredQty = item.getQuantity();

                List<InventoryBatch> availableBatches = inventoryBatchRepository
                        .findAvailableBatchesBySkuIdOrderByExpiryDate(skuId);

                int totalAvailable = availableBatches.stream()
                        .mapToInt(InventoryBatch::getAvailableQuantity)
                        .sum();

                if (totalAvailable < requiredQty) {
                    throw new BusinessException("INSUFFICIENT_STOCK",
                            "库存不足，无法锁定。SKU: " + item.getSku().getSkuCode() +
                                    "，需要: " + requiredQty +
                                    "，可用: " + totalAvailable);
                }

                int remaining = requiredQty;
                for (InventoryBatch batch : availableBatches) {
                    if (remaining <= 0) break;

                    InventoryBatch lockedBatch = inventoryBatchRepository.findByIdWithLock(batch.getId());
                    if (lockedBatch.isExpired() || lockedBatch.getAvailableQuantity() <= 0) {
                        continue;
                    }

                    int lockQty = Math.min(remaining, lockedBatch.getAvailableQuantity());

                    lockedBatch.setAvailableQuantity(lockedBatch.getAvailableQuantity() - lockQty);
                    lockedBatch.setLockedQuantity(lockedBatch.getLockedQuantity() + lockQty);
                    inventoryBatchRepository.save(lockedBatch);

                    InventoryLock lock = new InventoryLock();
                    lock.setOrder(order);
                    lock.setBatch(lockedBatch);
                    lock.setLockedQuantity(lockQty);
                    lock.setStatus(LockStatus.LOCKED);
                    inventoryLockRepository.save(lock);

                    createdLocks.add(lock);
                    remaining -= lockQty;
                }

                if (remaining > 0) {
                    throw new BusinessException("INSUFFICIENT_STOCK",
                            "库存不足，无法锁定。SKU: " + item.getSku().getSkuCode() +
                                    "，仍需: " + remaining);
                }

                Inventory inventory = inventoryRepository.findBySkuIdWithLock(skuId)
                        .orElseThrow(() -> new BusinessException("INVENTORY_NOT_FOUND", "库存不存在，SKU ID: " + skuId));
                inventory.setAvailableQuantity(inventory.getAvailableQuantity() - requiredQty);
                inventory.setLockedQuantity(inventory.getLockedQuantity() + requiredQty);
                inventoryRepository.save(inventory);
            }
            return createdLocks;
        } catch (BusinessException e) {
            rollbackLocks(createdLocks);
            throw e;
        } catch (Exception e) {
            rollbackLocks(createdLocks);
            throw new BusinessException("LOCK_FAILED", "库存锁定失败: " + e.getMessage());
        }
    }

    private void rollbackLocks(List<InventoryLock> locks) {
        for (InventoryLock lock : locks) {
            try {
                InventoryBatch batch = inventoryBatchRepository.findByIdWithLock(lock.getBatch().getId());
                if (batch != null) {
                    batch.setAvailableQuantity(batch.getAvailableQuantity() + lock.getLockedQuantity());
                    batch.setLockedQuantity(batch.getLockedQuantity() - lock.getLockedQuantity());
                    inventoryBatchRepository.save(batch);
                }
                inventoryLockRepository.delete(lock);
            } catch (Exception ignored) {
            }
        }
    }

    @Transactional
    public void releaseStockForOrder(Order order) {
        List<InventoryLock> locks = inventoryLockRepository.findByOrderIdAndStatus(
                order.getId(), LockStatus.LOCKED);

        if (locks.isEmpty()) {
            return;
        }

        Order.OrderStatus orderStatus = order.getStatus();
        boolean isShipped = orderStatus == Order.OrderStatus.SHIPPED;
        boolean isPostAllocation = orderStatus == Order.OrderStatus.PICKING
                || orderStatus == Order.OrderStatus.PICKED
                || orderStatus == Order.OrderStatus.PACKED
                || orderStatus == Order.OrderStatus.SHIPPED;

        Map<Long, Integer> skuQtyMap = new LinkedHashMap<>();

        for (InventoryLock lock : locks) {
            InventoryBatch batch = inventoryBatchRepository.findByIdWithLock(lock.getBatch().getId());

            if (isShipped) {
                batch.setLockedQuantity(batch.getLockedQuantity() - lock.getLockedQuantity());
                batch.setTotalQuantity(batch.getTotalQuantity() - lock.getLockedQuantity());
            } else {
                batch.setAvailableQuantity(batch.getAvailableQuantity() + lock.getLockedQuantity());
                batch.setLockedQuantity(batch.getLockedQuantity() - lock.getLockedQuantity());
            }

            inventoryBatchRepository.save(batch);

            Long skuId = batch.getSku().getId();
            skuQtyMap.merge(skuId, lock.getLockedQuantity(), Integer::sum);

            lock.setStatus(isShipped ? LockStatus.DEDUCTED : LockStatus.RELEASED);
            inventoryLockRepository.save(lock);
        }

        for (Map.Entry<Long, Integer> entry : skuQtyMap.entrySet()) {
            Long skuId = entry.getKey();
            Integer qty = entry.getValue();
            Inventory inventory = inventoryRepository.findBySkuIdWithLock(skuId)
                    .orElseThrow(() -> new BusinessException("INVENTORY_NOT_FOUND", "库存不存在，SKU ID: " + skuId));

            if (isShipped) {
                inventory.setLockedQuantity(inventory.getLockedQuantity() - qty);
                inventory.setTotalQuantity(inventory.getTotalQuantity() - qty);
            } else {
                inventory.setAvailableQuantity(inventory.getAvailableQuantity() + qty);
                inventory.setLockedQuantity(inventory.getLockedQuantity() - qty);
            }

            inventoryRepository.save(inventory);
        }
    }

    @Transactional
    public void deductStockForShippedOrder(Order order) {
        List<InventoryLock> locks = inventoryLockRepository.findByOrderIdAndStatus(
                order.getId(), LockStatus.LOCKED);

        if (locks.isEmpty()) {
            return;
        }

        Map<Long, Integer> skuQtyMap = new LinkedHashMap<>();

        for (InventoryLock lock : locks) {
            InventoryBatch batch = inventoryBatchRepository.findByIdWithLock(lock.getBatch().getId());
            batch.setLockedQuantity(batch.getLockedQuantity() - lock.getLockedQuantity());
            batch.setTotalQuantity(batch.getTotalQuantity() - lock.getLockedQuantity());
            inventoryBatchRepository.save(batch);

            Long skuId = batch.getSku().getId();
            skuQtyMap.merge(skuId, lock.getLockedQuantity(), Integer::sum);

            lock.setStatus(LockStatus.DEDUCTED);
            inventoryLockRepository.save(lock);
        }

        for (Map.Entry<Long, Integer> entry : skuQtyMap.entrySet()) {
            Long skuId = entry.getKey();
            Integer qty = entry.getValue();
            Inventory inventory = inventoryRepository.findBySkuIdWithLock(skuId)
                    .orElseThrow(() -> new BusinessException("INVENTORY_NOT_FOUND", "库存不存在，SKU ID: " + skuId));

            inventory.setLockedQuantity(inventory.getLockedQuantity() - qty);
            inventory.setTotalQuantity(inventory.getTotalQuantity() - qty);
            inventoryRepository.save(inventory);
        }
    }

    @Transactional
    public InventoryBatch createBatch(Long skuId, String batchNo, Integer quantity, LocalDateTime expiryDate) {
        Sku sku = skuRepository.findById(skuId)
                .orElseThrow(() -> new BusinessException("SKU_NOT_FOUND", "SKU不存在: " + skuId));

        InventoryBatch batch = new InventoryBatch();
        batch.setSku(sku);
        batch.setBatchNo(batchNo);
        batch.setTotalQuantity(quantity);
        batch.setAvailableQuantity(quantity);
        batch.setLockedQuantity(0);
        batch.setExpiryDate(expiryDate);

        return inventoryBatchRepository.save(batch);
    }

    public List<InventoryBatch> getBatchesBySkuId(Long skuId) {
        return inventoryBatchRepository.findBySkuIdOrderByExpiryDate(skuId);
    }

    private InventoryResponse convertToResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .id(inventory.getId())
                .skuId(inventory.getSku().getId())
                .skuCode(inventory.getSku().getSkuCode())
                .skuName(inventory.getSku().getSkuName())
                .availableQuantity(inventory.getAvailableQuantity())
                .lockedQuantity(inventory.getLockedQuantity())
                .totalQuantity(inventory.getTotalQuantity())
                .safetyStock(inventory.getSafetyStock())
                .outOfStock(inventory.isOutOfStock())
                .location(inventory.getSku().getLocation())
                .createdAt(inventory.getCreatedAt())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }
}
