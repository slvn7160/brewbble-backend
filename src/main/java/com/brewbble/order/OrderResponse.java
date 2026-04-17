package com.brewbble.order;

import com.brewbble.customization.CustomizationOptionResponse;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class OrderResponse {

    private Long id;
    private String status;
    private String paymentStatus;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal deliveryFee;
    private BigDecimal promoDiscount;
    private String promoCode;
    private BigDecimal rewardDiscount;
    private BigDecimal total;
    private int pointsEarned;
    private String notes;
    private Instant createdAt;
    private List<ItemLine> items;

    @Data
    @Builder
    public static class ItemLine {
        private Long menuItemId;
        private String name;
        private BigDecimal unitPrice;
        private int quantity;
        private BigDecimal subtotal;
        private List<CustomizationOptionResponse> customizations;
    }

    public static OrderResponse from(Order order) {
        List<ItemLine> lines = order.getItems().stream().map(i -> ItemLine.builder()
                .menuItemId(i.getMenuItem() != null ? i.getMenuItem().getId() : null)
                .name(i.getName())
                .unitPrice(i.getUnitPrice())
                .quantity(i.getQuantity())
                .subtotal(i.getSubtotal())
                .customizations(i.getCustomizations().stream()
                        .map(c -> new CustomizationOptionResponse(
                                c.getOptionId(), c.getName(), c.getType().name(), c.getPriceDelta()))
                        .toList())
                .build()).toList();

        return OrderResponse.builder()
                .id(order.getId())
                .status(order.getStatus().name())
                .paymentStatus(order.getPaymentStatus().name())
                .subtotal(order.getSubtotal())
                .tax(order.getTax())
                .deliveryFee(order.getDeliveryFee())
                .promoDiscount(order.getPromoDiscount())
                .promoCode(order.getPromotion() != null ? order.getPromotion().getCode() : null)
                .rewardDiscount(order.getRewardDiscount())
                .total(order.getTotal())
                .pointsEarned(order.getPointsEarned())
                .notes(order.getNotes())
                .createdAt(order.getCreatedAt())
                .items(lines)
                .build();
    }
}
