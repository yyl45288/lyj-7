package com.warehouse.wavepicking.service;

import com.warehouse.wavepicking.dto.response.InventoryResponse;
import com.warehouse.wavepicking.entity.Inventory;
import com.warehouse.wavepicking.entity.Sku;
import com.warehouse.wavepicking.exception.BusinessException;
import com.warehouse.wavepicking.repository.InventoryRepository;
import com.warehouse.wavepicking.repository.SkuRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final SkuRepository skuRepository;

    public InventoryService(InventoryRepository inventoryRepository, SkuRepository skuRepository) {
        this.inventoryRepository = inventoryRepository;
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
        Inventory inventory = inventoryRepository.findBySkuId(skuId)
                .orElseThrow(() -> new BusinessException("INVENTORY_NOT_FOUND", "库存不存在，SKU ID: " + skuId));
        return inventory.hasEnoughStock(quantity);
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
