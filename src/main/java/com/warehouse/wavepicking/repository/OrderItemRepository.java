package com.warehouse.wavepicking.repository;

import com.warehouse.wavepicking.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.wave.id = :waveId")
    List<OrderItem> findByWaveId(Long waveId);

    @Query("SELECT oi FROM OrderItem oi WHERE oi.sku.id = :skuId AND oi.order.status = :status")
    List<OrderItem> findBySkuIdAndOrderStatus(Long skuId, com.warehouse.wavepicking.entity.Order.OrderStatus status);
}
