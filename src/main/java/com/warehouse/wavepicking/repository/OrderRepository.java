package com.warehouse.wavepicking.repository;

import com.warehouse.wavepicking.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNo(String orderNo);

    List<Order> findByStatus(Order.OrderStatus status);

    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.wave IS NULL ORDER BY o.createdAt ASC")
    List<Order> findPendingOrdersWithoutWave(Order.OrderStatus status);

    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.urgent = true AND o.wave IS NULL ORDER BY o.createdAt ASC")
    List<Order> findUrgentPendingOrdersWithoutWave(Order.OrderStatus status);

    @Query("SELECT o FROM Order o WHERE o.wave.id = :waveId")
    List<Order> findByWaveId(Long waveId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    long countByStatus(Order.OrderStatus status);

    boolean existsByOrderNo(String orderNo);
}
