package com.warehouse.wavepicking.config;

import com.warehouse.wavepicking.entity.*;
import com.warehouse.wavepicking.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final SkuRepository skuRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryBatchRepository inventoryBatchRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final WaveRepository waveRepository;
    private final PickingTaskRepository pickingTaskRepository;

    public DataInitializer(SkuRepository skuRepository,
                           InventoryRepository inventoryRepository,
                           InventoryBatchRepository inventoryBatchRepository,
                           OrderRepository orderRepository,
                           OrderItemRepository orderItemRepository,
                           WaveRepository waveRepository,
                           PickingTaskRepository pickingTaskRepository) {
        this.skuRepository = skuRepository;
        this.inventoryRepository = inventoryRepository;
        this.inventoryBatchRepository = inventoryBatchRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.waveRepository = waveRepository;
        this.pickingTaskRepository = pickingTaskRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (skuRepository.count() > 0) {
            log.info("数据已存在，跳过初始化");
            return;
        }

        log.info("开始初始化数据...");

        List<Sku> skus = createSkus();
        createInventories(skus);
        createInventoryBatches(skus);
        createSampleOrders(skus);

        log.info("数据初始化完成");
    }

    private List<Sku> createSkus() {
        List<Sku> skus = new ArrayList<>();

        String[] categories = {"电子产品", "食品饮料", "日用百货", "服装鞋帽", "美妆护肤"};
        String[] locations = {"A01", "A02", "A03", "B01", "B02", "B03", "C01", "C02", "C03", "D01"};

        for (int i = 1; i <= 20; i++) {
            Sku sku = new Sku();
            sku.setSkuCode("SKU" + String.format("%04d", i));
            sku.setSkuName("商品" + i);
            sku.setCategory(categories[i % categories.length]);
            sku.setUnit("件");
            sku.setWeight(0.5 + i * 0.1);
            sku.setLocation(locations[i % locations.length]);
            skus.add(skuRepository.save(sku));
        }

        log.info("创建了 {} 个SKU", skus.size());
        return skus;
    }

    private void createInventories(List<Sku> skus) {
        int[] quantities = {100, 50, 200, 80, 150, 30, 120, 60, 90, 5,
                8, 200, 75, 15, 180, 40, 110, 3, 65, 12};

        for (int i = 0; i < skus.size(); i++) {
            Inventory inventory = new Inventory();
            inventory.setSku(skus.get(i));
            int qty = quantities[i % quantities.length];
            inventory.setTotalQuantity(qty);
            inventory.setAvailableQuantity(qty);
            inventory.setLockedQuantity(0);
            inventory.setSafetyStock(10);
            inventoryRepository.save(inventory);
        }

        log.info("创建了 {} 个库存记录", skus.size());
    }

    private void createInventoryBatches(List<Sku> skus) {
        int[] quantities = {100, 50, 200, 80, 150, 30, 120, 60, 90, 5,
                8, 200, 75, 15, 180, 40, 110, 3, 65, 12};
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < skus.size(); i++) {
            Sku sku = skus.get(i);
            int qty = quantities[i % quantities.length];

            InventoryBatch batch1 = new InventoryBatch();
            batch1.setSku(sku);
            batch1.setBatchNo("B" + String.format("%04d", i * 2 + 1));
            batch1.setTotalQuantity(qty * 2 / 3);
            batch1.setAvailableQuantity(qty * 2 / 3);
            batch1.setLockedQuantity(0);
            batch1.setExpiryDate(now.plusDays(30 + i * 3));
            inventoryBatchRepository.save(batch1);

            InventoryBatch batch2 = new InventoryBatch();
            batch2.setSku(sku);
            batch2.setBatchNo("B" + String.format("%04d", i * 2 + 2));
            batch2.setTotalQuantity(qty - qty * 2 / 3);
            batch2.setAvailableQuantity(qty - qty * 2 / 3);
            batch2.setLockedQuantity(0);
            batch2.setExpiryDate(now.plusDays(90 + i * 5));
            inventoryBatchRepository.save(batch2);
        }

        log.info("创建了库存批次记录");
    }

    private void createSampleOrders(List<Sku> skus) {
        String[] customers = {"张三", "李四", "王五", "赵六", "钱七", "孙八", "周九", "吴十"};
        String[] addresses = {"北京市朝阳区", "上海市浦东新区", "广州市天河区", "深圳市南山区",
                "杭州市西湖区", "成都市锦江区", "武汉市洪山区", "南京市鼓楼区"};

        for (int i = 1; i <= 8; i++) {
            Order order = new Order();
            order.setOrderNo("ORD20240101" + String.format("%04d", i));
            order.setCustomerName(customers[i % customers.length]);
            order.setAddress(addresses[i % addresses.length]);
            order.setPhone("138" + String.format("%08d", 10000000 + i));
            order.setStatus(Order.OrderStatus.PENDING);
            order.setUrgent(i == 1 || i == 5);
            order.setRemark(i == 1 ? "加急配送" : "");

            int itemCount = 1 + (i % 3);
            for (int j = 0; j < itemCount; j++) {
                Sku sku = skus.get((i + j * 3) % skus.size());
                OrderItem item = new OrderItem();
                item.setOrder(order);
                item.setSku(sku);
                item.setQuantity(1 + (j + i) % 5);
                item.setPickedQuantity(0);
                item.setLocation(sku.getLocation());
                order.getItems().add(item);
            }

            orderRepository.save(order);
        }

        for (int i = 9; i <= 12; i++) {
            Order order = new Order();
            order.setOrderNo("ORD20240101" + String.format("%04d", i));
            order.setCustomerName(customers[i % customers.length]);
            order.setAddress(addresses[i % addresses.length]);
            order.setPhone("139" + String.format("%08d", 20000000 + i));
            order.setStatus(Order.OrderStatus.CONFIRMED);
            order.setUrgent(i == 10);
            order.setRemark("");

            int itemCount = 1 + (i % 4);
            for (int j = 0; j < itemCount; j++) {
                Sku sku = skus.get((i + j * 2) % skus.size());
                OrderItem item = new OrderItem();
                item.setOrder(order);
                item.setSku(sku);
                item.setQuantity(1 + (j + i) % 3);
                item.setPickedQuantity(0);
                item.setLocation(sku.getLocation());
                order.getItems().add(item);
            }

            orderRepository.save(order);
        }

        log.info("创建了示例订单");
    }
}
