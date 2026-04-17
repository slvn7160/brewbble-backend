package com.brewbble.customization;

import java.math.BigDecimal;

public record CustomizationOptionResponse(
        Long id,
        String name,
        String type,
        BigDecimal priceDelta
) {}
