package com.warehouse.wavepicking.service;

import com.warehouse.wavepicking.dto.response.InventoryResponse;
import com.warehouse.wavepicking.entity.Inventory;
import com.warehouse.wavepicking.entity.InventoryBatch;
import com.warehouse.wavepicking.entity.Sku;
import com.warehouse.wavepicking.exception.BusinessException;
import com.warehouse.wavepicking.repository.InventoryBatchRepository;
import com.warehouse.wavepicking.repository.InventoryLockRepository;
import com.warehouse.wavepicking.repository.InventoryRepository;
import com.warehouse.wavepicking.repository.SkuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryBatchRepository inventoryBatchRepository;

    @Mock
    private InventoryLockRepository inventoryLockRepository;

    @Mock
    private SkuRepository skuRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private Sku testSku;
    private Inventory testInventory;

    @BeforeEach
    void setUp() {
        testSku = new Sku();
        testSku.setId(1L);
        testSku.setSkuCode("SKU001");
        testSku.setSkuName("测试商品");
        testSku.setLocation("A01");

        testInventory = new Inventory();
        testInventory.setId(1L);
        testInventory.setSku(testSku);
        testInventory.setAvailableQuantity(100);
        testInventory.setLockedQuantity(0);
        testInventory.setTotalQuantity(100);
        testInventory.setSafetyStock(10);
        testInventory.setCreatedAt(LocalDateTime.now());
        testInventory.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("获取所有库存")
    void testGetAllInventories() {
        Sku sku2 = new Sku();
        sku2.setId(2L);
        sku2.setSkuCode("SKU002");
        sku2.setSkuName("商品2");

        Inventory inv2 = new Inventory();
        inv2.setId(2L);
        inv2.setSku(sku2);
        inv2.setAvailableQuantity(50);
        inv2.setTotalQuantity(50);

        when(inventoryRepository.findAll()).thenReturn(Arrays.asList(testInventory, inv2));

        List<InventoryResponse> result = inventoryService.getAllInventories();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("SKU001", result.get(0).getSkuCode());
    }

    @Test
    @DisplayName("根据SKU ID获取库存 - 成功")
    void testGetInventoryBySkuId_Success() {
        when(inventoryRepository.findBySkuId(1L)).thenReturn(Optional.of(testInventory));

        InventoryResponse result = inventoryService.getInventoryBySkuId(1L);

        assertNotNull(result);
        assertEquals(100, result.getAvailableQuantity());
        assertEquals("SKU001", result.getSkuCode());
    }

    @Test
    @DisplayName("根据SKU ID获取库存 - 不存在")
    void testGetInventoryBySkuId_NotFound() {
        when(inventoryRepository.findBySkuId(999L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> inventoryService.getInventoryBySkuId(999L));

        assertEquals("INVENTORY_NOT_FOUND", exception.getCode());
    }

    @Test
    @DisplayName("锁定库存 - 成功")
    void testLockStock_Success() {
        when(inventoryRepository.findBySkuIdWithLock(1L)).thenReturn(Optional.of(testInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

        assertDoesNotThrow(() -> inventoryService.lockStock(1L, 30));

        assertEquals(70, testInventory.getAvailableQuantity());
        assertEquals(30, testInventory.getLockedQuantity());
        verify(inventoryRepository).save(testInventory);
    }

    @Test
    @DisplayName("锁定库存 - 库存不足")
    void testLockStock_Insufficient() {
        when(inventoryRepository.findBySkuIdWithLock(1L)).thenReturn(Optional.of(testInventory));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> inventoryService.lockStock(1L, 150));

        assertEquals("INSUFFICIENT_STOCK", exception.getCode());
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("解锁库存 - 成功")
    void testUnlockStock_Success() {
        testInventory.setAvailableQuantity(70);
        testInventory.setLockedQuantity(30);

        when(inventoryRepository.findBySkuIdWithLock(1L)).thenReturn(Optional.of(testInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

        assertDoesNotThrow(() -> inventoryService.unlockStock(1L, 20));

        assertEquals(90, testInventory.getAvailableQuantity());
        assertEquals(10, testInventory.getLockedQuantity());
    }

    @Test
    @DisplayName("检查库存是否充足 - 充足（基于批次）")
    void testHasEnoughStock_True() {
        InventoryBatch batch = new InventoryBatch();
        batch.setId(1L);
        batch.setAvailableQuantity(100);
        batch.setExpiryDate(LocalDateTime.now().plusDays(30));

        when(inventoryBatchRepository.findAvailableBatchesBySkuIdOrderByExpiryDate(1L))
                .thenReturn(Collections.singletonList(batch));

        boolean result = inventoryService.hasEnoughStock(1L, 50);

        assertTrue(result);
    }

    @Test
    @DisplayName("检查库存是否充足 - 不足（基于批次）")
    void testHasEnoughStock_False() {
        InventoryBatch batch = new InventoryBatch();
        batch.setId(1L);
        batch.setAvailableQuantity(5);
        batch.setExpiryDate(LocalDateTime.now().plusDays(30));

        when(inventoryBatchRepository.findAvailableBatchesBySkuIdOrderByExpiryDate(1L))
                .thenReturn(Collections.singletonList(batch));

        boolean result = inventoryService.hasEnoughStock(1L, 200);

        assertFalse(result);
    }

    @Test
    @DisplayName("获取缺货商品列表")
    void testGetOutOfStockItems() {
        Inventory lowStockInv = new Inventory();
        lowStockInv.setId(2L);
        lowStockInv.setSku(testSku);
        lowStockInv.setAvailableQuantity(5);
        lowStockInv.setSafetyStock(10);

        when(inventoryRepository.findOutOfStockItems()).thenReturn(List.of(lowStockInv));

        List<InventoryResponse> result = inventoryService.getOutOfStockItems();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("增加库存")
    void testAddStock() {
        when(inventoryRepository.findBySkuIdWithLock(1L)).thenReturn(Optional.of(testInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

        InventoryResponse result = inventoryService.addStock(1L, 50);

        assertNotNull(result);
        assertEquals(150, testInventory.getTotalQuantity());
        assertEquals(150, testInventory.getAvailableQuantity());
    }

    @Test
    @DisplayName("扣减锁定库存 - 成功")
    void testDeductLockedStock_Success() {
        testInventory.setLockedQuantity(30);

        when(inventoryRepository.findBySkuIdWithLock(1L)).thenReturn(Optional.of(testInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

        assertDoesNotThrow(() -> inventoryService.deductLockedStock(1L, 20));

        assertEquals(10, testInventory.getLockedQuantity());
        assertEquals(80, testInventory.getTotalQuantity());
    }

    @Test
    @DisplayName("扣减锁定库存 - 锁定量不足")
    void testDeductLockedStock_Insufficient() {
        testInventory.setLockedQuantity(10);

        when(inventoryRepository.findBySkuIdWithLock(1L)).thenReturn(Optional.of(testInventory));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> inventoryService.deductLockedStock(1L, 20));

        assertEquals("DEDUCT_ERROR", exception.getCode());
    }
}
