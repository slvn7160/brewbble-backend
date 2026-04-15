package com.brewbble.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {
    boolean existsBySquareEventId(String squareEventId);
    Optional<PaymentEvent> findBySquarePaymentId(String squarePaymentId);
}
