package com.brewbble.customization;

import java.util.List;

public record GroupedCustomizationsResponse(
        List<CustomizationOptionResponse> sweetness,
        List<CustomizationOptionResponse> iceLevel,
        List<CustomizationOptionResponse> milkType,
        List<CustomizationOptionResponse> toppings,
        List<CustomizationOptionResponse> size,
        List<CustomizationOptionResponse> temperature
) {}
