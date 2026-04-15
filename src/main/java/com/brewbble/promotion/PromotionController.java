package com.brewbble.promotion;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    /** Employees and admins see currently valid active promotions to apply at checkout. */
    @GetMapping("/active")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('ADMIN')")
    public List<Promotion> getActive() {
        return promotionService.findActive();
    }

    /** Admin: all promotions regardless of status. */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Promotion> getAll() {
        return promotionService.findAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Promotion> create(@Valid @RequestBody PromotionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(promotionService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Promotion> update(@PathVariable Long id,
                                            @Valid @RequestBody PromotionRequest req) {
        return ResponseEntity.ok(promotionService.update(id, req));
    }

    /** Admin: quickly activate or deactivate a promotion. */
    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Promotion> setActive(@PathVariable Long id,
                                               @RequestBody Map<String, Boolean> body) {
        Boolean active = body.get("active");
        if (active == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(promotionService.setActive(id, active));
    }
}
