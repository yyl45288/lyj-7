package com.warehouse.wavepicking.service;

import com.warehouse.wavepicking.entity.*;
import com.warehouse.wavepicking.entity.InventoryLock.LockStatus;
import com.warehouse.wavepicking.exception.BusinessException;
import com.warehouse.wavepicking.repository.InventoryBatchRepository;
import com.warehouse.wavepicking.repository.InventoryLockRepository;
import com.warehouse.wavepicking.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryLockServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryBatchRepository inventoryBatchRepository;

    @Mock
    private InventoryLockRepository inventoryLockRepository;

    @InjectMocks
    private InventoryLockService inventoryLockService;

    private Sku testSku;
    private Inventory testInventory;
    private InventoryBatch nearExpiryBatch;
    private InventoryBatch farExpiryBatch;
    private InventoryBatch expiredBatch;
    private Order testOrder;
    private OrderItem testOrderItem;

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

        nearExpiryBatch = new InventoryBatch();
        nearExpiryBatch.setId(1L);
        nearExpiryBatch.setSku(testSku);
        nearExpiryBatch.setBatchNo("B001");
        nearExpiryBatch.setTotalQuantity(40);
        nearExpiryBatch.setAvailableQuantity(40);
        nearExpiryBatch.setLockedQuantity(0);
        nearExpiryBatch.setExpiryDate(LocalDateTime.now().plusDays(7));

        farExpiryBatch = new InventoryBatch();
        farExpiryBatch.setId(2L);
        farExpiryBatch.setSku(testSku);
        farExpiryBatch.setBatchNo("B002");
        farExpiryBatch.setTotalQuantity(60);
        farExpiryBatch.setAvailableQuantity(60);
        farExpiryBatch.setLockedQuantity(0);
        farExpiryBatch.setExpiryDate(LocalDateTime.now().plusDays(90));

        expiredBatch = new InventoryBatch();
        expiredBatch.setId(3L);
        expiredBatch.setSku(testSku);
        expiredBatch.setBatchNo("B003");
        expiredBatch.setTotalQuantity(100);
        expiredBatch.setAvailableQuantity(100);
        expiredBatch.setLockedQuantity(0);
        expiredBatch.setExpiryDate(LocalDateTime.now().minusDays(1));

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setOrderNo("ORD20240101000001");
        testOrder.setCustomerName("测试客户");
        testOrder.setStatus(Order.OrderStatus.PENDING);

        testOrderItem = new OrderItem();
        testOrderItem.setId(1L);
        testOrderItem.setOrder(testOrder);
        testOrderItem.setSku(testSku);
        testOrderItem.setQuantity(30);
        testOrder.setItems(Collections.singletonList(testOrderItem));
    }

    @Nested
    @DisplayName("库存充足性检查")
    class HasEnoughStockTests {
        @Test
        @DisplayName("基于批次可用量检查库存是否充足")
        void testHasEnoughStock_BasedOnBatches() {
            when(inventoryBatchRepository.findAvailableBatchesBySkuIdOrderByExpiryDate(1L))
                    .thenReturn(Arrays.asList(nearExpiryBatch, farExpiryBatch));

            assertTrue(inventoryLockService.hasEnoughStock(1L, 50));
            assertTrue(inventoryLockService.hasEnoughStock(1L, 100));
            assertFalse(inventoryLockService.hasEnoughStock(1L, 101));
        }

        @Test
        @DisplayName("无库存批次时返回false")
        void testHasEnoughStock_NoBatches() {
            when(inventoryBatchRepository.findAvailableBatchesBySkuIdOrderByExpiryDate(1L))
                    .thenReturn(Collections.emptyList());

            assertFalse(inventoryLockService.hasEnoughStock(1L, 1));
        }
    }

    @Nested
    @DisplayName("锁库存 - 临期优先分配策略")
    class LockStockExpiryFirstTests {
        @Test
        @DisplayName("临期优先分配 - 单批次足够时只锁定临期批次")
        void testLockStock_NearExpiryFirst_SingleBatchSufficient() {
            when(inventoryBatchRepository.findAvailableBatchesBySkuIdOrderByExpiryDate(1L))
                    .thenReturn(Arrays.asList(nearExpiryBatch, farExpiryBatch));
            when(inventoryBatchRepository.findByIdWithLock(1L)).thenReturn(nearExpiryBatch);
            when(inventoryBatchRepository.save(any(InventoryBatch.class))).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryLockRepository.save(any(InventoryLock.class))).thenAnswer(inv -> {
                InventoryLock lock = inv.getArgument(0);
                lock.setId(System.nanoTime());
                return lock;
            });
            when(inventoryRepository.findBySkuIdWithLock(1L)).thenReturn(Optional.of(testInventory));
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

            List<InventoryLock> locks = inventoryLockService.lockStockForOrder(testOrder);

            assertNotNull(locks);
            assertEquals(1, locks.size());
            assertEquals(1L, locks.get(0).getBatch().getId());

            int totalLockedNear = locks.stream()
                    .filter(l -> l.getBatch().getId().equals(1L))
                    .mapToInt(InventoryLock::getLockedQuantity)
                    .sum();
            assertEquals(30, totalLockedNear);
            assertEquals(10, nearExpiryBatch.getAvailableQuantity());
            assertEquals(30, nearExpiryBatch.getLockedQuantity());
            assertEquals(60, farExpiryBatch.getAvailableQuantity());
        }

        @Test
        @DisplayName("多批次分配 - 临期不够时用远期批次补足")
        void testLockStock_MultiBatchAllocation() {
            nearExpiryBatch.setAvailableQuantity(10);
            nearExpiryBatch.setTotalQuantity(10);
            farExpiryBatch.setAvailableQuantity(20);
            farExpiryBatch.setTotalQuantity(20);
            testOrderItem.setQuantity(30);

            when(inventoryBatchRepository.findAvailableBatchesBySkuIdOrderByExpiryDate(1L))
                    .thenReturn(Arrays.asList(nearExpiryBatch, farExpiryBatch));
            when(inventoryBatchRepository.findByIdWithLock(1L)).thenReturn(nearExpiryBatch);
            when(inventoryBatchRepository.findByIdWithLock(2L)).thenReturn(farExpiryBatch);
            when(inventoryBatchRepository.save(any(InventoryBatch.class))).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryLockRepository.save(any(InventoryLock.class))).thenAnswer(inv -> {
                InventoryLock lock = inv.getArgument(0);
                lock.setId(System.nanoTime());
                return lock;
            });
            when(inventoryRepository.findBySkuIdWithLock(1L)).thenReturn(Optional.of(testInventory));
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

            List<InventoryLock> locks = inventoryLockService.lockStockForOrder(testOrder);

            assertEquals(2, locks.size());

            InventoryLock lock1 = locks.stream().filter(l -> l.getBatch().getId().equals(1L)).findFirst().orElse(null);
            InventoryLock lock2 = locks.stream().filter(l -> l.getBatch().getId().equals(2L)).findFirst().orElse(null);

            assertNotNull(lock1);
            assertNotNull(lock2);
            assertEquals(10, lock1.getLockedQuantity());
            assertEquals(20, lock2.getLockedQuantity());
            assertEquals(LockStatus.LOCKED, lock1.getStatus());
            assertEquals(LockStatus.LOCKED, lock2.getStatus());

            assertEquals(0, nearExpiryBatch.getAvailableQuantity());
            assertEquals(10, nearExpiryBatch.getLockedQuantity());
            assertEquals(0, farExpiryBatch.getAvailableQuantity());
            assertEquals(20, farExpiryBatch.getLockedQuantity());
        }

        @Test
        @DisplayName("不分配过期库存 - 查询过滤掉过期批次")
        void testLockStock_ExpiredBatch_NotAllocated() {
            testOrderItem.setQuantity(50);
            when(inventoryBatchRepository.findAvailableBatchesBySkuIdOrderByExpiryDate(1L))
                    .thenReturn(Arrays.asList(nearExpiryBatch, farExpiryBatch));
            when(inventoryBatchRepository.findByIdWithLock(1L)).thenReturn(nearExpiryBatch);
            when(inventoryBatchRepository.findByIdWithLock(2L)).thenReturn(farExpiryBatch);
            when(inventoryBatchRepository.save(any(InventoryBatch.class))).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryLockRepository.save(any(InventoryLock.class))).thenAnswer(inv -> {
                InventoryLock lock = inv.getArgument(0);
                lock.setId(System.nanoTime());
                return lock;
            });
            when(inventoryRepository.findBySkuIdWithLock(1L)).thenReturn(Optional.of(testInventory));
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

            List<InventoryLock> locks = inventoryLockService.lockStockForOrder(testOrder);

            for (InventoryLock lock : locks) {
                assertNotEquals(3L, lock.getBatch().getId(), "不应锁定过期批次");
            }
            verify(inventoryBatchRepository, never()).findByIdWithLock(3L);
        }

        @Test
        @DisplayName("锁库存时二次检查 - 即使批次进入循环也会跳过过期批次")
        void testLockStock_SecondaryExpiryCheck_SkipsExpired() {
            nearExpiryBatch.setExpiryDate(LocalDateTime.now().minusDays(1));
            nearExpiryBatch.setAvailableQuantity(40);
            testOrderItem.setQuantity(30);

            when(inventoryBatchRepository.findAvailableBatchesBySkuIdOrderByExpiryDate(1L))
                    .thenReturn(Arrays.asList(nearExpiryBatch, farExpiryBatch));
            when(inventoryBatchRepository.findByIdWithLock(1L)).thenReturn(nearExpiryBatch);
            when(inventoryBatchRepository.findByIdWithLock(2L)).thenReturn(farExpiryBatch);
            when(inventoryBatchRepository.save(any(InventoryBatch.class))).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryLockRepository.save(any(InventoryLock.class))).thenAnswer(inv -> {
                InventoryLock lock = inv.getArgument(0);
                lock.setId(System.nanoTime());
                return lock;
            });
            when(inventoryRepository.findBySkuIdWithLock(1L)).thenReturn(Optional.of(testInventory));
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

            List<InventoryLock> locks = inventoryLockService.lockStockForOrder(testOrder);

            assertEquals(1, locks.size());
            assertEquals(2L, locks.get(0).getBatch().getId(), "应跳过过期批次，使用远期批次");
            assertEquals(30, locks.get(0).getLockedQuantity());
        }

        @Test
        @DisplayName("完全无库存 - 抛出异常且不产生任何锁")
        void testLockStock_NoStockAtAll_ThrowsException() {
            testOrderItem.setQuantity(10);

            when(inventoryBatchRepository.findAvailableBatchesBySkuIdOrderByExpiryDate(1L))
                    .thenReturn(Collections.emptyList());

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> inventoryLockService.lockStockForOrder(testOrder));

            assertEquals("INSUFFICIENT_STOCK", exception.getCode());
            verify(inventoryLockRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("锁库存 - 事务一致性与回滚")
    class LockStockRollbackTests {
        @Test
        @DisplayName("库存不足时 - 初步校验失败直接抛错不产生脏数据")
        void testLockStock_InsufficientStock_PreliminaryCheck_FailFast() {
            nearExpiryBatch.setAvailableQuantity(10);
            nearExpiryBatch.setTotalQuantity(10);
            farExpiryBatch.setAvailableQuantity(5);
            farExpiryBatch.setTotalQuantity(5);
            testOrderItem.setQuantity(50);

            when(inventoryBatchRepository.findAvailableBatchesBySkuIdOrderByExpiryDate(1L))
                    .thenReturn(Arrays.asList(nearExpiryBatch, farExpiryBatch));

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> inventoryLockService.lockStockForOrder(testOrder));

            assertEquals("INSUFFICIENT_STOCK", exception.getCode());
            verify(inventoryLockRepository, never()).save(any());
            verify(inventoryBatchRepository, never()).save(any());
        }

        @Test
        @DisplayName("部分锁定后回滚 - 批次数量恢复且删除锁记录")
        void testLockStock_PartialLock_Rollback_BatchAndLockRestored() {
            nearExpiryBatch.setAvailableQuantity(10);
            nearExpiryBatch.setTotalQuantity(10);
            InventoryBatch midExpiryBatch = new InventoryBatch();
            midExpiryBatch.setId(3L);
            midExpiryBatch.setSku(testSku);
            midExpiryBatch.setBatchNo("B003");
            midExpiryBatch.setAvailableQuantity(20);
            midExpiryBatch.setTotalQuantity(20);
            midExpiryBatch.setExpiryDate(LocalDateTime.now().plusDays(30));
            InventoryBatch midExpiryBatchButLocked = new InventoryBatch();
            midExpiryBatchButLocked.setId(3L);
            midExpiryBatchButLocked.setSku(testSku);
            midExpiryBatchButLocked.setBatchNo("B003");
            midExpiryBatchButLocked.setAvailableQuantity(20);
            midExpiryBatchButLocked.setTotalQuantity(20);
            midExpiryBatchButLocked.setExpiryDate(LocalDateTime.now().minusDays(1));
            farExpiryBatch.setAvailableQuantity(10);
            farExpiryBatch.setTotalQuantity(10);
            testOrderItem.setQuantity(35);

            when(inventoryBatchRepository.findAvailableBatchesBySkuIdOrderByExpiryDate(1L))
                    .thenReturn(Arrays.asList(nearExpiryBatch, midExpiryBatch, farExpiryBatch));
            when(inventoryBatchRepository.findByIdWithLock(1L)).thenReturn(nearExpiryBatch);
            when(inventoryBatchRepository.findByIdWithLock(3L)).thenReturn(midExpiryBatchButLocked);
            when(inventoryBatchRepository.findByIdWithLock(2L)).thenReturn(farExpiryBatch);
            when(inventoryBatchRepository.save(any(InventoryBatch.class))).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryLockRepository.save(any(InventoryLock.class))).thenAnswer(inv -> {
                InventoryLock lock = inv.getArgument(0);
                lock.setId(System.nanoTime());
                return lock;
            });

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> inventoryLockService.lockStockForOrder(testOrder));

            assertEquals("INSUFFICIENT_STOCK", exception.getCode());

            verify(inventoryBatchRepository, atLeastOnce()).save(any(InventoryBatch.class));
            verify(inventoryLockRepository, atLeastOnce()).delete(any(InventoryLock.class));
        }

        @Test
        @DisplayName("回滚时同步恢复汇总库存表")
        void testLockStock_Rollback_RestoresInventorySummary() {
            Sku sku2 = new Sku();
            sku2.setId(2L);
            sku2.setSkuCode("SKU002");

            OrderItem item2 = new OrderItem();
            item2.setId(2L);
            item2.setOrder(testOrder);
            item2.setSku(sku2);
            item2.setQuantity(9999);
            testOrder.setItems(Arrays.asList(testOrderItem, item2));

            InventoryBatch batchForSku2 = new InventoryBatch();
            batchForSku2.setId(10L);
            batchForSku2.setSku(sku2);
            batchForSku2.setBatchNo("B010");
            batchForSku2.setTotalQuantity(10);
            batchForSku2.setAvailableQuantity(10);
            batchForSku2.setExpiryDate(LocalDateTime.now().plusDays(60));

            Inventory inventory2 = new Inventory();
            inventory2.setId(2L);
            inventory2.setSku(sku2);
            inventory2.setAvailableQuantity(10);
            inventory2.setLockedQuantity(0);
            inventory2.setTotalQuantity(10);

            when(inventoryBatchRepository.findAvailableBatchesBySkuIdOrderByExpiryDate(1L))
                    .thenReturn(Arrays.asList(nearExpiryBatch, farExpiryBatch));
            when(inventoryBatchRepository.findAvailableBatchesBySkuIdOrderByExpiryDate(2L))
                    .thenReturn(Collections.singletonList(batchForSku2));
            when(inventoryBatchRepository.findByIdWithLock(1L)).thenReturn(nearExpiryBatch);
            lenient().when(inventoryBatchRepository.findByIdWithLock(2L)).thenReturn(farExpiryBatch);
            lenient().when(inventoryBatchRepository.findByIdWithLock(10L)).thenReturn(batchForSku2);
            when(inventoryBatchRepository.save(any(InventoryBatch.class))).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryLockRepository.save(any(InventoryLock.class))).thenAnswer(inv -> {
                InventoryLock lock = inv.getArgument(0);
                lock.setId(System.nanoTime());
                return lock;
            });
            when(inventoryRepository.findBySkuIdWithLock(1L)).thenReturn(Optional.of(testInventory));
            lenient().when(inventoryRepository.findBySkuIdWithLock(2L)).thenReturn(Optional.of(inventory2));
            when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));
            lenient().doNothing().when(inventoryLockRepository).delete(any(InventoryLock.class));

            assertThrows(BusinessException.class,
                    () -> inventoryLockService.lockStockForOrder(testOrder));

            assertEquals(100, testInventory.getAvailableQuantity(),
                    "回滚后汇总表可用库存应恢复到原始值");
            assertEquals(0, testInventory.getLockedQuantity(),
                    "回滚后汇总表锁定库存应恢复到原始值");
        }

        @Test
        @DisplayName("汇总库存同步更新 - 锁定成功时正确更新")
        void testLockStock_Success_SyncsInventorySummary() {
            when(inventoryBatchRepository.findAvailableBatchesBySkuIdOrderByExpiryDate(1L))
                    .thenReturn(Arrays.asList(nearExpiryBatch, farExpiryBatch));
            when(inventoryBatchRepository.findByIdWithLock(1L)).thenReturn(nearExpiryBatch);
            when(inventoryBatchRepository.save(any(InventoryBatch.class))).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryLockRepository.save(any(InventoryLock.class))).thenAnswer(inv -> {
                InventoryLock lock = inv.getArgument(0);
                lock.setId(System.nanoTime());
                return lock;
            });
            when(inventoryRepository.findBySkuIdWithLock(1L)).thenReturn(Optional.of(testInventory));
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

            inventoryLockService.lockStockForOrder(testOrder);

            assertEquals(70, testInventory.getAvailableQuantity());
            assertEquals(30, testInventory.getLockedQuantity());
            assertEquals(100, testInventory.getTotalQuantity());
        }

        @Test
        @DisplayName("多个SKU订单项锁定 - 全部成功")
        void testLockStock_MultipleOrderItems_Success() {
            Sku sku2 = new Sku();
            sku2.setId(2L);
            sku2.setSkuCode("SKU002");
            sku2.setSkuName("商品2");

            OrderItem item2 = new OrderItem();
            item2.setId(2L);
            item2.setOrder(testOrder);
            item2.setSku(sku2);
            item2.setQuantity(20);
            testOrder.setItems(Arrays.asList(testOrderItem, item2));

            InventoryBatch batchForSku2 = new InventoryBatch();
            batchForSku2.setId(10L);
            batchForSku2.setSku(sku2);
            batchForSku2.setBatchNo("B010");
            batchForSku2.setTotalQuantity(50);
            batchForSku2.setAvailableQuantity(50);
            batchForSku2.setLockedQuantity(0);
            batchForSku2.setExpiryDate(LocalDateTime.now().plusDays(60));

            Inventory inventory2 = new Inventory();
            inventory2.setId(2L);
            inventory2.setSku(sku2);
            inventory2.setAvailableQuantity(50);
            inventory2.setLockedQuantity(0);
            inventory2.setTotalQuantity(50);

            when(inventoryBatchRepository.findAvailableBatchesBySkuIdOrderByExpiryDate(1L))
                    .thenReturn(Arrays.asList(nearExpiryBatch, farExpiryBatch));
            when(inventoryBatchRepository.findAvailableBatchesBySkuIdOrderByExpiryDate(2L))
                    .thenReturn(Collections.singletonList(batchForSku2));
            when(inventoryBatchRepository.findByIdWithLock(1L)).thenReturn(nearExpiryBatch);
            when(inventoryBatchRepository.findByIdWithLock(10L)).thenReturn(batchForSku2);
            when(inventoryBatchRepository.save(any(InventoryBatch.class))).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryLockRepository.save(any(InventoryLock.class))).thenAnswer(inv -> {
                InventoryLock lock = inv.getArgument(0);
                lock.setId(System.nanoTime());
                return lock;
            });
            when(inventoryRepository.findBySkuIdWithLock(1L)).thenReturn(Optional.of(testInventory));
            when(inventoryRepository.findBySkuIdWithLock(2L)).thenReturn(Optional.of(inventory2));
            when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));

            List<InventoryLock> locks = inventoryLockService.lockStockForOrder(testOrder);

            assertEquals(2, locks.size());

            long sku1Locks = locks.stream().filter(l -> l.getBatch().getSku().getId().equals(1L)).count();
            long sku2Locks = locks.stream().filter(l -> l.getBatch().getSku().getId().equals(2L)).count();
            assertEquals(1, sku1Locks);
            assertEquals(1, sku2Locks);

            assertEquals(70, testInventory.getAvailableQuantity());
            assertEquals(30, testInventory.getLockedQuantity());
            assertEquals(30, inventory2.getAvailableQuantity());
            assertEquals(20, inventory2.getLockedQuantity());
        }
    }

    @Nested
    @DisplayName("取消订单 - 按阶段处理库存")
    class CancelOrderStageTests {
        @Test
        @DisplayName("PENDING - 无需释放库存")
        void testReleaseStock_Pending_NoAction() {
            testOrder.setStatus(Order.OrderStatus.PENDING);

            inventoryLockService.releaseStockForCancelledOrder(testOrder);

            verify(inventoryLockRepository, never()).findByOrderIdAndStatus(any(), any());
        }

        @Test
        @DisplayName("CONFIRMED - 释放锁定库存回可售")
        void testReleaseStock_Confirmed_ReleaseToAvailable() {
            testOrder.setStatus(Order.OrderStatus.CONFIRMED);
            nearExpiryBatch.setAvailableQuantity(10);
            nearExpiryBatch.setLockedQuantity(30);
            testInventory.setAvailableQuantity(70);
            testInventory.setLockedQuantity(30);

            InventoryLock lock = new InventoryLock();
            lock.setId(1L);
            lock.setOrder(testOrder);
            lock.setBatch(nearExpiryBatch);
            lock.setLockedQuantity(30);
            lock.setStatus(LockStatus.LOCKED);

            when(inventoryLockRepository.findByOrderIdAndStatus(1L, LockStatus.LOCKED))
                    .thenReturn(Collections.singletonList(lock));
            when(inventoryBatchRepository.findByIdWithLock(1L)).thenReturn(nearExpiryBatch);
            when(inventoryBatchRepository.save(any(InventoryBatch.class))).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryLockRepository.save(any(InventoryLock.class))).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryRepository.findBySkuIdWithLock(1L)).thenReturn(Optional.of(testInventory));
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

            inventoryLockService.releaseStockForCancelledOrder(testOrder);

            assertEquals(40, nearExpiryBatch.getAvailableQuantity());
            assertEquals(0, nearExpiryBatch.getLockedQuantity());
            assertEquals(100, testInventory.getAvailableQuantity());
            assertEquals(0, testInventory.getLockedQuantity());
            assertEquals(LockStatus.RELEASED, lock.getStatus());
        }

        @Test
        @DisplayName("ALLOCATED - 释放锁定库存回可售")
        void testReleaseStock_Allocated_ReleaseToAvailable() {
            testOrder.setStatus(Order.OrderStatus.ALLOCATED);
            nearExpiryBatch.setAvailableQuantity(10);
            nearExpiryBatch.setLockedQuantity(30);
            testInventory.setAvailableQuantity(70);
            testInventory.setLockedQuantity(30);

            InventoryLock lock = new InventoryLock();
            lock.setId(1L);
            lock.setOrder(testOrder);
            lock.setBatch(nearExpiryBatch);
            lock.setLockedQuantity(30);
            lock.setStatus(LockStatus.LOCKED);

            when(inventoryLockRepository.findByOrderIdAndStatus(1L, LockStatus.LOCKED))
                    .thenReturn(Collections.singletonList(lock));
            when(inventoryBatchRepository.findByIdWithLock(1L)).thenReturn(nearExpiryBatch);
            when(inventoryBatchRepository.save(any(InventoryBatch.class))).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryLockRepository.save(any(InventoryLock.class))).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryRepository.findBySkuIdWithLock(1L)).thenReturn(Optional.of(testInventory));
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

            inventoryLockService.releaseStockForCancelledOrder(testOrder);

            assertEquals(40, nearExpiryBatch.getAvailableQuantity());
            assertEquals(0, nearExpiryBatch.getLockedQuantity());
            assertEquals(100, testInventory.getAvailableQuantity());
            assertEquals(0, testInventory.getLockedQuantity());
            assertEquals(LockStatus.RELEASED, lock.getStatus());
        }

        @Test
        @DisplayName("PICKING - 已进入拣货流程不能直接释放，抛异常")
        void testReleaseStock_Picking_ThrowsException() {
            testOrder.setStatus(Order.OrderStatus.PICKING);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> inventoryLockService.releaseStockForCancelledOrder(testOrder));

            assertEquals("CANNOT_RELEASE_STOCK", exception.getCode());
            assertTrue(exception.getMessage().contains("已进入拣货流程"));
        }

        @Test
        @DisplayName("PICKED - 已拣货不能直接释放，抛异常")
        void testReleaseStock_Picked_ThrowsException() {
            testOrder.setStatus(Order.OrderStatus.PICKED);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> inventoryLockService.releaseStockForCancelledOrder(testOrder));

            assertEquals("CANNOT_RELEASE_STOCK", exception.getCode());
        }

        @Test
        @DisplayName("PACKED - 已打包不能直接释放，抛异常")
        void testReleaseStock_Packed_ThrowsException() {
            testOrder.setStatus(Order.OrderStatus.PACKED);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> inventoryLockService.releaseStockForCancelledOrder(testOrder));

            assertEquals("CANNOT_RELEASE_STOCK", exception.getCode());
        }

        @Test
        @DisplayName("SHIPPED - 已装车出库不能回滚，抛异常且明确说明不回可售")
        void testReleaseStock_Shipped_ThrowsException() {
            testOrder.setStatus(Order.OrderStatus.SHIPPED);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> inventoryLockService.releaseStockForCancelledOrder(testOrder));

            assertEquals("CANNOT_RELEASE_STOCK", exception.getCode());
            assertTrue(exception.getMessage().contains("已装车出库"));
            assertTrue(exception.getMessage().contains("不能回滚到可售库存"));
        }

        @Test
        @DisplayName("CANCELLED - 已取消无需重复操作")
        void testReleaseStock_Cancelled_NoAction() {
            testOrder.setStatus(Order.OrderStatus.CANCELLED);

            inventoryLockService.releaseStockForCancelledOrder(testOrder);

            verify(inventoryLockRepository, never()).findByOrderIdAndStatus(any(), any());
        }

        @Test
        @DisplayName("无有效锁定记录 - 不报错不操作")
        void testReleaseStock_NoLocks_NoAction() {
            testOrder.setStatus(Order.OrderStatus.CONFIRMED);

            when(inventoryLockRepository.findByOrderIdAndStatus(1L, LockStatus.LOCKED))
                    .thenReturn(Collections.emptyList());

            inventoryLockService.releaseStockForCancelledOrder(testOrder);

            verify(inventoryBatchRepository, never()).save(any());
            verify(inventoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("多批次同时释放 - 汇总数量正确")
        void testReleaseStock_MultiBatchRelease() {
            testOrder.setStatus(Order.OrderStatus.CONFIRMED);
            testInventory.setAvailableQuantity(60);
            testInventory.setLockedQuantity(40);
            nearExpiryBatch.setAvailableQuantity(15);
            nearExpiryBatch.setLockedQuantity(25);
            farExpiryBatch.setAvailableQuantity(45);
            farExpiryBatch.setLockedQuantity(15);

            InventoryLock lock1 = new InventoryLock();
            lock1.setId(1L);
            lock1.setOrder(testOrder);
            lock1.setBatch(nearExpiryBatch);
            lock1.setLockedQuantity(25);
            lock1.setStatus(LockStatus.LOCKED);

            InventoryLock lock2 = new InventoryLock();
            lock2.setId(2L);
            lock2.setOrder(testOrder);
            lock2.setBatch(farExpiryBatch);
            lock2.setLockedQuantity(15);
            lock2.setStatus(LockStatus.LOCKED);

            when(inventoryLockRepository.findByOrderIdAndStatus(1L, LockStatus.LOCKED))
                    .thenReturn(Arrays.asList(lock1, lock2));
            when(inventoryBatchRepository.findByIdWithLock(1L)).thenReturn(nearExpiryBatch);
            when(inventoryBatchRepository.findByIdWithLock(2L)).thenReturn(farExpiryBatch);
            when(inventoryBatchRepository.save(any(InventoryBatch.class))).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryLockRepository.save(any(InventoryLock.class))).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryRepository.findBySkuIdWithLock(1L)).thenReturn(Optional.of(testInventory));
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

            inventoryLockService.releaseStockForCancelledOrder(testOrder);

            assertEquals(40, nearExpiryBatch.getAvailableQuantity());
            assertEquals(0, nearExpiryBatch.getLockedQuantity());
            assertEquals(60, farExpiryBatch.getAvailableQuantity());
            assertEquals(0, farExpiryBatch.getLockedQuantity());
            assertEquals(LockStatus.RELEASED, lock1.getStatus());
            assertEquals(LockStatus.RELEASED, lock2.getStatus());
            assertEquals(100, testInventory.getAvailableQuantity());
            assertEquals(0, testInventory.getLockedQuantity());
        }
    }

    @Nested
    @DisplayName("已发货扣减库存 - 不回可售库存")
    class DeductShippedStockTests {
        @Test
        @DisplayName("SHIPPED - 扣减锁定库存，不增加可用，不回到可售")
        void testDeductStock_Shipped_DeductsNotReleases() {
            testOrder.setStatus(Order.OrderStatus.SHIPPED);
            testInventory.setAvailableQuantity(70);
            testInventory.setLockedQuantity(30);
            testInventory.setTotalQuantity(100);
            nearExpiryBatch.setAvailableQuantity(10);
            nearExpiryBatch.setLockedQuantity(30);
            nearExpiryBatch.setTotalQuantity(40);

            InventoryLock lock = new InventoryLock();
            lock.setId(1L);
            lock.setOrder(testOrder);
            lock.setBatch(nearExpiryBatch);
            lock.setLockedQuantity(30);
            lock.setStatus(LockStatus.LOCKED);

            when(inventoryLockRepository.findByOrderIdAndStatus(1L, LockStatus.LOCKED))
                    .thenReturn(Collections.singletonList(lock));
            when(inventoryBatchRepository.findByIdWithLock(1L)).thenReturn(nearExpiryBatch);
            when(inventoryBatchRepository.save(any(InventoryBatch.class))).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryLockRepository.save(any(InventoryLock.class))).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryRepository.findBySkuIdWithLock(1L)).thenReturn(Optional.of(testInventory));
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

            inventoryLockService.deductStockForShippedOrder(testOrder);

            assertEquals(10, nearExpiryBatch.getAvailableQuantity(), "可用库存不应增加");
            assertEquals(0, nearExpiryBatch.getLockedQuantity(), "锁定库存应扣减");
            assertEquals(10, nearExpiryBatch.getTotalQuantity(), "批次总库存应扣减");
            assertEquals(LockStatus.DEDUCTED, lock.getStatus());

            assertEquals(70, testInventory.getAvailableQuantity(), "汇总可用库存不应增加");
            assertEquals(0, testInventory.getLockedQuantity(), "汇总锁定库存应扣减");
            assertEquals(70, testInventory.getTotalQuantity(), "汇总总库存应扣减");
        }

        @Test
        @DisplayName("扣减后锁定状态为 DEDUCTED 而非 RELEASED")
        void testDeductStock_StatusIsDeductedNotReleased() {
            testInventory.setAvailableQuantity(70);
            testInventory.setLockedQuantity(30);
            nearExpiryBatch.setAvailableQuantity(10);
            nearExpiryBatch.setLockedQuantity(30);

            InventoryLock lock = new InventoryLock();
            lock.setId(1L);
            lock.setOrder(testOrder);
            lock.setBatch(nearExpiryBatch);
            lock.setLockedQuantity(30);
            lock.setStatus(LockStatus.LOCKED);

            when(inventoryLockRepository.findByOrderIdAndStatus(1L, LockStatus.LOCKED))
                    .thenReturn(Collections.singletonList(lock));
            when(inventoryBatchRepository.findByIdWithLock(1L)).thenReturn(nearExpiryBatch);
            when(inventoryBatchRepository.save(any(InventoryBatch.class))).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryLockRepository.save(any(InventoryLock.class))).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryRepository.findBySkuIdWithLock(1L)).thenReturn(Optional.of(testInventory));
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

            inventoryLockService.deductStockForShippedOrder(testOrder);

            assertEquals(LockStatus.DEDUCTED, lock.getStatus());
            assertNotEquals(LockStatus.RELEASED, lock.getStatus());
        }

        @Test
        @DisplayName("扣减时可用库存保持不变 - 体现不回可售")
        void testDeductStock_AvailableUnchanged() {
            testInventory.setAvailableQuantity(50);
            testInventory.setLockedQuantity(50);
            nearExpiryBatch.setAvailableQuantity(20);
            nearExpiryBatch.setLockedQuantity(30);

            InventoryLock lock = new InventoryLock();
            lock.setId(1L);
            lock.setOrder(testOrder);
            lock.setBatch(nearExpiryBatch);
            lock.setLockedQuantity(30);
            lock.setStatus(LockStatus.LOCKED);

            when(inventoryLockRepository.findByOrderIdAndStatus(1L, LockStatus.LOCKED))
                    .thenReturn(Collections.singletonList(lock));
            when(inventoryBatchRepository.findByIdWithLock(1L)).thenReturn(nearExpiryBatch);
            when(inventoryBatchRepository.save(any(InventoryBatch.class))).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryLockRepository.save(any(InventoryLock.class))).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryRepository.findBySkuIdWithLock(1L)).thenReturn(Optional.of(testInventory));
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

            inventoryLockService.deductStockForShippedOrder(testOrder);

            assertEquals(20, nearExpiryBatch.getAvailableQuantity(), "批次可用库存保持不变");
            assertEquals(50, testInventory.getAvailableQuantity(), "汇总可用库存保持不变");
        }
    }

    @Nested
    @DisplayName("拣货阶段回滚库存")
    class RollbackFromPickingStageTests {
        @Test
        @DisplayName("PICKING 状态可通过专用方法回滚")
        void testRollbackFromPicking_PickingStatus_Success() {
            testOrder.setStatus(Order.OrderStatus.PICKING);
            nearExpiryBatch.setAvailableQuantity(10);
            nearExpiryBatch.setLockedQuantity(30);
            testInventory.setAvailableQuantity(70);
            testInventory.setLockedQuantity(30);

            InventoryLock lock = new InventoryLock();
            lock.setId(1L);
            lock.setOrder(testOrder);
            lock.setBatch(nearExpiryBatch);
            lock.setLockedQuantity(30);
            lock.setStatus(LockStatus.LOCKED);

            when(inventoryLockRepository.findByOrderIdAndStatus(1L, LockStatus.LOCKED))
                    .thenReturn(Collections.singletonList(lock));
            when(inventoryBatchRepository.findByIdWithLock(1L)).thenReturn(nearExpiryBatch);
            when(inventoryBatchRepository.save(any(InventoryBatch.class))).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryLockRepository.save(any(InventoryLock.class))).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryRepository.findBySkuIdWithLock(1L)).thenReturn(Optional.of(testInventory));
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

            inventoryLockService.rollbackStockFromPickingStage(testOrder);

            assertEquals(40, nearExpiryBatch.getAvailableQuantity());
            assertEquals(0, nearExpiryBatch.getLockedQuantity());
            assertEquals(LockStatus.RELEASED, lock.getStatus());
        }

        @Test
        @DisplayName("非拣货阶段调用专用回滚方法抛异常")
        void testRollbackFromPicking_InvalidStatus_ThrowsException() {
            testOrder.setStatus(Order.OrderStatus.CONFIRMED);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> inventoryLockService.rollbackStockFromPickingStage(testOrder));

            assertEquals("INVALID_ROLLBACK", exception.getCode());
        }
    }

    @Nested
    @DisplayName("实体与枚举完整性验证")
    class EntityAndEnumTests {
        @Test
        @DisplayName("InventoryLock 状态枚举完整性")
        void testLockStatus_EnumValues() {
            assertEquals(3, LockStatus.values().length);
            assertNotNull(LockStatus.valueOf("LOCKED"));
            assertNotNull(LockStatus.valueOf("RELEASED"));
            assertNotNull(LockStatus.valueOf("DEDUCTED"));
        }

        @Test
        @DisplayName("InventoryBatch 过期检测方法")
        void testInventoryBatch_IsExpired() {
            InventoryBatch expired = new InventoryBatch();
            expired.setExpiryDate(LocalDateTime.now().minusDays(1));
            assertTrue(expired.isExpired());

            InventoryBatch notExpired = new InventoryBatch();
            notExpired.setExpiryDate(LocalDateTime.now().plusDays(30));
            assertFalse(notExpired.isExpired());
        }

        @Test
        @DisplayName("InventoryBatch 临期检测方法")
        void testInventoryBatch_IsNearExpiry() {
            InventoryBatch nearExpiry = new InventoryBatch();
            nearExpiry.setExpiryDate(LocalDateTime.now().plusDays(5));
            assertTrue(nearExpiry.isNearExpiry(7));
            assertFalse(nearExpiry.isNearExpiry(3));

            InventoryBatch farExpiry = new InventoryBatch();
            farExpiry.setExpiryDate(LocalDateTime.now().plusDays(30));
            assertFalse(farExpiry.isNearExpiry(7));
        }
    }
}
