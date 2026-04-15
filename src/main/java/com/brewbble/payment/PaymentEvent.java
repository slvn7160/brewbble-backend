package com.brewbble.payment;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;

    @Column(nullable = false, unique = true)
    private String squareEventId;

    private String squarePaymentId;

    @Column(nullable = false)
    private String eventType;

    private BigDecimal amount;

    @Builder.Default
    private String currency = "USD";

    @Column(nullable = false)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String rawPayload;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
