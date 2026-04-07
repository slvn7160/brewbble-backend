package com.brewbble.order;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

@Data
public class PlaceOrderRequest {

    @NotEmpty
    private List<OrderLineRequest> items;

    private String notes;

    @Data
    public static class OrderLineRequest {
        @NotNull
        private Long menuItemId;

        @Positive
        private int quantity;
    }
}
