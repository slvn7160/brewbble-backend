package com.brewbble.order;

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
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal deliveryFee;
    private BigDecimal total;
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
    }

    public static OrderResponse from(Order order) {
        List<ItemLine> lines = order.getItems().stream().map(i -> ItemLine.builder()
                .menuItemId(i.getMenuItem() != null ? i.getMenuItem().getId() : null)
                .name(i.getName())
                .unitPrice(i.getUnitPrice())
                .quantity(i.getQuantity())
                .subtotal(i.getSubtotal())
                .build()).toList();

        return OrderResponse.builder()
                .id(order.getId())
                .status(order.getStatus().name())
                .subtotal(order.getSubtotal())
                .tax(order.getTax())
                .deliveryFee(order.getDeliveryFee())
                .total(order.getTotal())
                .notes(order.getNotes())
                .createdAt(order.getCreatedAt())
                .items(lines)
                .build();
    }
}
