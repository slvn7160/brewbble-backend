package com.brewbble.customer;

import com.brewbble.customization.CustomizationOptionRepository;
import com.brewbble.menu.MenuItem;
import com.brewbble.menu.MenuItemRepository;
import com.brewbble.user.AppUser;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/customers/me/presets")
@RequiredArgsConstructor
public class PresetController {

    private final UserPresetRepository         presetRepository;
    private final MenuItemRepository           menuItemRepository;
    private final CustomizationOptionRepository customizationOptionRepository;

    @GetMapping
    public ResponseEntity<List<PresetResponse>> list(@AuthenticationPrincipal AppUser user) {
        List<PresetResponse> result = presetRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(PresetResponse::from)
                .toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<PresetResponse> create(
            @RequestBody CreatePresetRequest req,
            @AuthenticationPrincipal AppUser user) {

        MenuItem menuItem = menuItemRepository.findById(req.getMenuItemId())
                .orElseThrow(() -> new IllegalArgumentException("Menu item not found: " + req.getMenuItemId()));

        // Validate all option IDs exist
        List<Long> optionIds = req.getOptionIds() == null ? List.of() : req.getOptionIds();
        if (!optionIds.isEmpty()) {
            long found = customizationOptionRepository.countByIdIn(optionIds);
            if (found != optionIds.size()) {
                throw new IllegalArgumentException("One or more customization options not found");
            }
        }

        UserPreset saved = presetRepository.save(UserPreset.builder()
                .user(user)
                .name(req.getName())
                .menuItem(menuItem)
                .optionIds(new ArrayList<>(optionIds))
                .build());

        return ResponseEntity.status(HttpStatus.CREATED).body(PresetResponse.from(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUser user) {

        UserPreset preset = presetRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Preset not found"));

        presetRepository.delete(preset);
        return ResponseEntity.noContent().build();
    }

    // ── DTOs ─────────────────────────────────────────────────────────────────

    @Data
    public static class CreatePresetRequest {
        @NotBlank
        private String name;

        @NotNull
        private Long menuItemId;

        private List<Long> optionIds;   // optional — empty means no customizations
    }

    @Data
    public static class PresetResponse {
        private Long id;
        private String name;
        private Long menuItemId;
        private String menuItemName;
        private BigDecimal basePrice;
        private String imageUrl;
        private List<Long> optionIds;
        private Instant createdAt;

        public static PresetResponse from(UserPreset p) {
            PresetResponse r = new PresetResponse();
            r.id           = p.getId();
            r.name         = p.getName();
            r.menuItemId   = p.getMenuItem() != null ? p.getMenuItem().getId() : null;
            r.menuItemName = p.getMenuItem() != null ? p.getMenuItem().getName() : null;
            r.basePrice    = p.getMenuItem() != null ? p.getMenuItem().getPrice() : null;
            r.imageUrl     = p.getMenuItem() != null ? p.getMenuItem().getImageUrl() : null;
            r.optionIds    = p.getOptionIds();
            r.createdAt    = p.getCreatedAt();
            return r;
        }
    }
}
