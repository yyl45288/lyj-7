package com.warehouse.wavepicking.service;

import com.warehouse.wavepicking.entity.*;
import com.warehouse.wavepicking.entity.InventoryLock.LockStatus;
import com.warehouse.wavepicking.exception.BusinessException;
import com.warehouse.wavepicking.repository.InventoryBatchRepository;
import com.warehouse.wavepicking.repository.InventoryLockRepository;
import com.warehouse.wavepicking.repository.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class InventoryLockService implements IInventoryLockService {

    private static final Logger log = LoggerFactory.getLogger(InventoryLockService.class);

    private final InventoryRepository inventoryRepository;
    private final InventoryBatchRepository inventoryBatchRepository;
    private final InventoryLockRepository inventoryLockRepository;

    public InventoryLockService(InventoryRepository inventoryRepository,
                                InventoryBatchRepository inventoryBatchRepository,
                                InventoryLockRepository inventoryLockRepository) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryBatchRepository = inventoryBatchRepository;
        this.inventoryLockRepository = inventoryLockRepository;
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
        Map<Long, Integer> skuLockedQtyMap = new LinkedHashMap<>();
        Set<Long> skuSummaryUpdated = new HashSet<>();

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
                int skuLockedQty = 0;

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
                    skuLockedQty += lockQty;
                }

                if (remaining > 0) {
                    throw new BusinessException("INSUFFICIENT_STOCK",
                            "库存不足，无法锁定。SKU: " + item.getSku().getSkuCode() +
                                    "，仍需: " + remaining);
                }

                Inventory inventory = inventoryRepository.findBySkuIdWithLock(skuId)
                        .orElseThrow(() -> new BusinessException("INVENTORY_NOT_FOUND",
                                "库存不存在，SKU ID: " + skuId));
                inventory.setAvailableQuantity(inventory.getAvailableQuantity() - skuLockedQty);
                inventory.setLockedQuantity(inventory.getLockedQuantity() + skuLockedQty);
                inventoryRepository.save(inventory);
                skuSummaryUpdated.add(skuId);

                skuLockedQtyMap.put(skuId, skuLockedQty);
            }

            return createdLocks;
        } catch (BusinessException e) {
            rollbackLocks(createdLocks, skuLockedQtyMap, skuSummaryUpdated);
            throw e;
        } catch (Exception e) {
            rollbackLocks(createdLocks, skuLockedQtyMap, skuSummaryUpdated);
            log.error("库存锁定失败，orderId={}", order.getId(), e);
            throw new BusinessException("LOCK_FAILED", "库存锁定失败: " + e.getMessage());
        }
    }

    private void rollbackLocks(List<InventoryLock> locks, Map<Long, Integer> skuLockedQtyMap,
                               Set<Long> skuSummaryUpdated) {
        log.info("开始回滚库存锁定，locks={}, skuLockedQtyMap={}, skuSummaryUpdated={}",
                locks.size(), skuLockedQtyMap, skuSummaryUpdated);

        for (InventoryLock lock : locks) {
            try {
                InventoryBatch batch = inventoryBatchRepository.findByIdWithLock(lock.getBatch().getId());
                if (batch != null) {
                    batch.setAvailableQuantity(batch.getAvailableQuantity() + lock.getLockedQuantity());
                    batch.setLockedQuantity(batch.getLockedQuantity() - lock.getLockedQuantity());
                    if (batch.getLockedQuantity() < 0) {
                        batch.setLockedQuantity(0);
                    }
                    inventoryBatchRepository.save(batch);
                }
                inventoryLockRepository.delete(lock);
            } catch (Exception ex) {
                log.error("回滚批次库存失败，lockId={}", lock.getId(), ex);
            }
        }

        for (Map.Entry<Long, Integer> entry : skuLockedQtyMap.entrySet()) {
            Long skuId = entry.getKey();
            if (!skuSummaryUpdated.contains(skuId)) {
                continue;
            }
            try {
                Integer qty = entry.getValue();
                Inventory inventory = inventoryRepository.findBySkuIdWithLock(skuId).orElse(null);
                if (inventory != null) {
                    inventory.setAvailableQuantity(inventory.getAvailableQuantity() + qty);
                    inventory.setLockedQuantity(inventory.getLockedQuantity() - qty);
                    if (inventory.getLockedQuantity() < 0) {
                        inventory.setLockedQuantity(0);
                    }
                    inventoryRepository.save(inventory);
                }
            } catch (Exception ex) {
                log.error("回滚汇总库存失败，skuId={}", entry.getKey(), ex);
            }
        }
    }

    @Transactional
    public void releaseStockForCancelledOrder(Order order) {
        Order.OrderStatus status = order.getStatus();

        switch (status) {
            case PENDING:
                log.info("订单{}处于PENDING状态，无库存锁定，无需释放", order.getId());
                return;
            case CONFIRMED:
            case ALLOCATED:
                log.info("订单{}处于{}状态，释放锁定库存回可售", order.getId(), status);
                releaseLockedStockToAvailable(order);
                break;
            case PICKING:
            case PICKED:
            case PACKED:
                throw new BusinessException("CANNOT_RELEASE_STOCK",
                        String.format("订单处于%s状态，已进入拣货流程，不能直接释放库存，请先完成业务回滚", status));
            case SHIPPED:
                throw new BusinessException("CANNOT_RELEASE_STOCK",
                        "订单已装车出库，库存已扣减，不能回滚到可售库存");
            case CANCELLED:
                log.info("订单{}已取消，无需重复释放库存", order.getId());
                return;
            default:
                throw new BusinessException("UNKNOWN_STATUS", "未知订单状态: " + status);
        }
    }

    private void releaseLockedStockToAvailable(Order order) {
        List<InventoryLock> locks = inventoryLockRepository.findByOrderIdAndStatus(
                order.getId(), LockStatus.LOCKED);

        if (locks.isEmpty()) {
            log.warn("订单{}无有效锁定记录", order.getId());
            return;
        }

        Map<Long, Integer> skuQtyMap = new LinkedHashMap<>();

        for (InventoryLock lock : locks) {
            InventoryBatch batch = inventoryBatchRepository.findByIdWithLock(lock.getBatch().getId());

            batch.setAvailableQuantity(batch.getAvailableQuantity() + lock.getLockedQuantity());
            batch.setLockedQuantity(batch.getLockedQuantity() - lock.getLockedQuantity());
            if (batch.getLockedQuantity() < 0) {
                batch.setLockedQuantity(0);
            }
            inventoryBatchRepository.save(batch);

            Long skuId = batch.getSku().getId();
            skuQtyMap.merge(skuId, lock.getLockedQuantity(), Integer::sum);

            lock.setStatus(LockStatus.RELEASED);
            inventoryLockRepository.save(lock);
        }

        for (Map.Entry<Long, Integer> entry : skuQtyMap.entrySet()) {
            Long skuId = entry.getKey();
            Integer qty = entry.getValue();
            Inventory inventory = inventoryRepository.findBySkuIdWithLock(skuId)
                    .orElseThrow(() -> new BusinessException("INVENTORY_NOT_FOUND",
                            "库存不存在，SKU ID: " + skuId));

            inventory.setAvailableQuantity(inventory.getAvailableQuantity() + qty);
            inventory.setLockedQuantity(inventory.getLockedQuantity() - qty);
            if (inventory.getLockedQuantity() < 0) {
                inventory.setLockedQuantity(0);
            }
            inventoryRepository.save(inventory);
        }

        log.info("订单{}库存释放完成，共释放{}个锁定记录", order.getId(), locks.size());
    }

    @Transactional
    public void deductStockForShippedOrder(Order order) {
        List<InventoryLock> locks = inventoryLockRepository.findByOrderIdAndStatus(
                order.getId(), LockStatus.LOCKED);

        if (locks.isEmpty()) {
            log.warn("订单{}无有效锁定记录，无法扣减已发货库存", order.getId());
            return;
        }

        Map<Long, Integer> skuQtyMap = new LinkedHashMap<>();

        for (InventoryLock lock : locks) {
            InventoryBatch batch = inventoryBatchRepository.findByIdWithLock(lock.getBatch().getId());

            batch.setLockedQuantity(batch.getLockedQuantity() - lock.getLockedQuantity());
            batch.setTotalQuantity(batch.getTotalQuantity() - lock.getLockedQuantity());
            if (batch.getLockedQuantity() < 0) {
                batch.setLockedQuantity(0);
            }
            if (batch.getTotalQuantity() < 0) {
                batch.setTotalQuantity(0);
            }
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
                    .orElseThrow(() -> new BusinessException("INVENTORY_NOT_FOUND",
                            "库存不存在，SKU ID: " + skuId));

            inventory.setLockedQuantity(inventory.getLockedQuantity() - qty);
            inventory.setTotalQuantity(inventory.getTotalQuantity() - qty);
            if (inventory.getLockedQuantity() < 0) {
                inventory.setLockedQuantity(0);
            }
            if (inventory.getTotalQuantity() < 0) {
                inventory.setTotalQuantity(0);
            }
            inventoryRepository.save(inventory);
        }

        log.info("订单{}发货库存扣减完成，共扣减{}个锁定记录，已出库货物不会回到可售库存",
                order.getId(), locks.size());
    }

    @Transactional
    public void rollbackStockFromPickingStage(Order order) {
        Order.OrderStatus status = order.getStatus();
        if (status != Order.OrderStatus.PICKING
                && status != Order.OrderStatus.PICKED
                && status != Order.OrderStatus.PACKED) {
            throw new BusinessException("INVALID_ROLLBACK",
                    "只有 PICKING/PICKED/PACKED 状态才能执行拣货回滚");
        }
        log.info("订单{}从{}状态回滚库存到已锁定", order.getId(), status);
        releaseLockedStockToAvailable(order);
    }

    public List<InventoryLock> getLocksByOrderId(Long orderId) {
        return inventoryLockRepository.findByOrderId(orderId);
    }

    public List<InventoryLock> getActiveLocksByOrderId(Long orderId) {
        return inventoryLockRepository.findByOrderIdAndStatus(orderId, LockStatus.LOCKED);
    }
}
