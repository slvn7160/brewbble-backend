package com.brewbble.customer;

import com.brewbble.menu.MenuItem;
import com.brewbble.menu.MenuItemRepository;
import com.brewbble.user.AppUser;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/customers/me/favourites")
@RequiredArgsConstructor
public class FavouriteController {

    private final UserFavouriteRepository favouriteRepository;
    private final MenuItemRepository      menuItemRepository;

    @GetMapping
    public ResponseEntity<List<FavouriteResponse>> list(@AuthenticationPrincipal AppUser user) {
        List<FavouriteResponse> result = favouriteRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(FavouriteResponse::from)
                .toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<FavouriteResponse> add(
            @RequestBody AddFavouriteRequest req,
            @AuthenticationPrincipal AppUser user) {

        if (favouriteRepository.existsByUserIdAndMenuItemId(user.getId(), req.getMenuItemId())) {
            throw new IllegalArgumentException("Item is already in favourites");
        }

        MenuItem menuItem = menuItemRepository.findById(req.getMenuItemId())
                .orElseThrow(() -> new IllegalArgumentException("Menu item not found: " + req.getMenuItemId()));

        UserFavourite saved = favouriteRepository.save(
                UserFavourite.builder().user(user).menuItem(menuItem).build());

        return ResponseEntity.status(HttpStatus.CREATED).body(FavouriteResponse.from(saved));
    }

    @DeleteMapping("/{menuItemId}")
    public ResponseEntity<Void> remove(
            @PathVariable Long menuItemId,
            @AuthenticationPrincipal AppUser user) {

        UserFavourite fav = favouriteRepository.findByUserIdAndMenuItemId(user.getId(), menuItemId)
                .orElseThrow(() -> new IllegalArgumentException("Favourite not found"));

        favouriteRepository.delete(fav);
        return ResponseEntity.noContent().build();
    }

    // ── DTOs ─────────────────────────────────────────────────────────────────

    @Data
    public static class AddFavouriteRequest {
        @NotNull
        private Long menuItemId;
    }

    @Data
    public static class FavouriteResponse {
        private Long id;
        private Long menuItemId;
        private String name;
        private String description;
        private BigDecimal price;
        private String imageUrl;
        private Instant savedAt;

        public static FavouriteResponse from(UserFavourite fav) {
            FavouriteResponse r = new FavouriteResponse();
            r.id          = fav.getId();
            r.menuItemId  = fav.getMenuItem().getId();
            r.name        = fav.getMenuItem().getName();
            r.description = fav.getMenuItem().getDescription();
            r.price       = fav.getMenuItem().getPrice();
            r.imageUrl    = fav.getMenuItem().getImageUrl();
            r.savedAt     = fav.getCreatedAt();
            return r;
        }
    }
}
