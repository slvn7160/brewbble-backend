package com.brewbble.customization;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/customizations")
@RequiredArgsConstructor
public class CustomizationController {

    private final CustomizationOptionRepository customizationOptionRepository;

    /** Public — frontend picker loads all available options grouped by type. */
    @GetMapping
    public ResponseEntity<GroupedCustomizationsResponse> getAll() {
        List<CustomizationOption> options =
                customizationOptionRepository.findByAvailableTrueOrderBySortOrderAsc();
        return ResponseEntity.ok(toGrouped(options));
    }

    /** ADMIN — create a new customization option. */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomizationOptionResponse> create(
            @Valid @RequestBody CustomizationRequest req) {
        CustomizationOption saved = customizationOptionRepository.save(
                CustomizationOption.builder()
                        .name(req.getName())
                        .type(CustomizationType.valueOf(req.getType().toUpperCase()))
                        .priceDelta(req.getPriceDelta() != null ? req.getPriceDelta() : BigDecimal.ZERO)
                        .available(req.isAvailable())
                        .sortOrder(req.getSortOrder())
                        .build());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    /** ADMIN — update an existing option. */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomizationOptionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CustomizationRequest req) {
        CustomizationOption option = customizationOptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customization option not found: " + id));
        option.setName(req.getName());
        option.setType(CustomizationType.valueOf(req.getType().toUpperCase()));
        option.setPriceDelta(req.getPriceDelta() != null ? req.getPriceDelta() : BigDecimal.ZERO);
        option.setAvailable(req.isAvailable());
        option.setSortOrder(req.getSortOrder());
        return ResponseEntity.ok(toResponse(customizationOptionRepository.save(option)));
    }

    /** ADMIN — toggle availability without a full update. */
    @PatchMapping("/{id}/available")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomizationOptionResponse> toggleAvailable(
            @PathVariable Long id,
            @RequestBody AvailableRequest req) {
        CustomizationOption option = customizationOptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customization option not found: " + id));
        option.setAvailable(req.isAvailable());
        return ResponseEntity.ok(toResponse(customizationOptionRepository.save(option)));
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private GroupedCustomizationsResponse toGrouped(List<CustomizationOption> options) {
        return new GroupedCustomizationsResponse(
                filter(options, CustomizationType.SWEETNESS),
                filter(options, CustomizationType.ICE_LEVEL),
                filter(options, CustomizationType.MILK_TYPE),
                filter(options, CustomizationType.TOPPING),
                filter(options, CustomizationType.SIZE),
                filter(options, CustomizationType.TEMPERATURE)
        );
    }

    private List<CustomizationOptionResponse> filter(List<CustomizationOption> options, CustomizationType type) {
        return options.stream()
                .filter(o -> o.getType() == type)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private CustomizationOptionResponse toResponse(CustomizationOption o) {
        return new CustomizationOptionResponse(o.getId(), o.getName(), o.getType().name(), o.getPriceDelta());
    }

    // ── DTOs ─────────────────────────────────────────────────────────────────

    @Data
    public static class CustomizationRequest {
        @NotBlank
        private String name;

        @NotBlank
        private String type;    // SWEETNESS | ICE_LEVEL | MILK_TYPE | TOPPING | SIZE | TEMPERATURE

        private BigDecimal priceDelta;
        private boolean available = true;
        private int sortOrder = 0;
    }

    @Data
    public static class AvailableRequest {
        @NotNull
        private boolean available;
    }
}
