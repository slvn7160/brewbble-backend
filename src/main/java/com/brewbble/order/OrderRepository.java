package com.brewbble.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.createdAt >= :startOfDay AND o.status != com.brewbble.order.OrderStatus.CANCELLED")
    BigDecimal sumRevenueAfter(@Param("startOfDay") Instant startOfDay);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :startOfDay AND o.status != com.brewbble.order.OrderStatus.CANCELLED")
    long countOrdersAfter(@Param("startOfDay") Instant startOfDay);
}
