package com.brewbble.promotion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;

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

    /** Accept date-only strings e.g. "2026-04-15" — treated as start of day UTC. */
    private LocalDate validFrom;

    /** Accept date-only strings e.g. "2026-08-31" — treated as end of day UTC. */
    private LocalDate validUntil;

    public Promotion toEntity() {
        return Promotion.builder()
                .code(code.toUpperCase())
                .description(description)
                .type(type)
                .value(value)
                .active(active)
                .minOrderAmount(minOrderAmount)
                .validFrom(validFrom  != null ? validFrom.atStartOfDay(ZoneOffset.UTC).toInstant()  : null)
                .validUntil(validUntil != null ? validUntil.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : null)
                .build();
    }
}
