package com.brewbble.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    java.util.Optional<Order> findBySquarePaymentId(String squarePaymentId);
    java.util.Optional<Order> findByTerminalCheckoutId(String terminalCheckoutId);

    Page<Order> findByStatusIn(Collection<OrderStatus> statuses, Pageable pageable);

    @Query("SELECT o.status, COUNT(o) FROM Order o WHERE o.createdAt >= :from AND o.createdAt < :to GROUP BY o.status")
    List<Object[]> countByStatusBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT COALESCE(o.paymentMethod, 'UNKNOWN'), COUNT(o), COALESCE(SUM(o.total), 0) FROM Order o WHERE o.createdAt >= :from AND o.createdAt < :to AND o.status != com.brewbble.order.OrderStatus.CANCELLED GROUP BY o.paymentMethod")
    List<Object[]> summaryByPaymentMethodBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.createdAt >= :startOfDay AND o.status != com.brewbble.order.OrderStatus.CANCELLED")
    BigDecimal sumRevenueAfter(@Param("startOfDay") Instant startOfDay);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :startOfDay AND o.status != com.brewbble.order.OrderStatus.CANCELLED")
    long countOrdersAfter(@Param("startOfDay") Instant startOfDay);

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.createdAt >= :from AND o.createdAt < :to AND o.status != com.brewbble.order.OrderStatus.CANCELLED")
    BigDecimal sumRevenueBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :from AND o.createdAt < :to AND o.status != com.brewbble.order.OrderStatus.CANCELLED")
    long countOrdersBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
            SELECT DATE(created_at AT TIME ZONE 'UTC') AS date,
                   COALESCE(SUM(total), 0)             AS revenue,
                   COUNT(*)                             AS order_count
            FROM   orders
            WHERE  created_at >= :from
              AND  created_at <  :to
              AND  status     != 'CANCELLED'
            GROUP BY DATE(created_at AT TIME ZONE 'UTC')
            ORDER BY date
            """, nativeQuery = true)
    List<Object[]> dailyRevenueBetween(@Param("from") Instant from, @Param("to") Instant to);
}
