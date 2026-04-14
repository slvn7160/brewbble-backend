package com.brewbble.reward;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RewardTransactionRepository extends JpaRepository<RewardTransaction, Long> {
    List<RewardTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Modifying
    @Query("UPDATE RewardTransaction t SET t.orderId = :orderId WHERE t.user.id = :userId AND t.orderId IS NULL AND t.type = 'REDEEMED'")
    void linkLatestRedemption(@Param("userId") Long userId, @Param("orderId") Long orderId);
}
