package com.brewbble.customization;

import com.brewbble.order.OrderItem;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_item_customizations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemCustomization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    // Nullable — option may be deleted after the order was placed
    @Column(name = "option_id")
    private Long optionId;

    @Column(nullable = false)
    private String name;        // snapshot at time of order

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CustomizationType type;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal priceDelta = BigDecimal.ZERO;
}
