package com.brewbble.promotion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class PromotionRequest {

    @NotBlank
    private String code;

    private String description;

    @NotNull
    private Promotion.DiscountType type;

    @NotNull
    @Positive
    private BigDecimal value;

    private boolean active = true;

    private BigDecimal minOrderAmount;
    private Instant validFrom;
    private Instant validUntil;

    public Promotion toEntity() {
        return Promotion.builder()
                .code(code.toUpperCase())
                .description(description)
                .type(type)
                .value(value)
                .active(active)
                .minOrderAmount(minOrderAmount)
                .validFrom(validFrom)
                .validUntil(validUntil)
                .build();
    }
}
