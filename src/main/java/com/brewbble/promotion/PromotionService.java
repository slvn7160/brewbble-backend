package com.brewbble.promotion;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promoRepository;

    @Transactional
    public Promotion create(PromotionRequest req) {
        if (promoRepository.findByCodeIgnoreCase(req.getCode()).isPresent()) {
            throw new IllegalArgumentException("Promo code already exists: " + req.getCode());
        }
        return promoRepository.save(req.toEntity());
    }

    @Transactional
    public Promotion update(Long id, PromotionRequest req) {
        Promotion promo = findById(id);

        // If code is changing, ensure the new code is not taken by another promo
        if (!promo.getCode().equalsIgnoreCase(req.getCode())) {
            promoRepository.findByCodeIgnoreCase(req.getCode()).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new IllegalArgumentException("Promo code already in use: " + req.getCode());
                }
            });
        }

        promo.setCode(req.getCode().toUpperCase());
        promo.setDescription(req.getDescription());
        promo.setType(req.getType());
        promo.setValue(req.getValue());
        promo.setActive(req.isActive());
        promo.setMinOrderAmount(req.getMinOrderAmount());
        promo.setValidFrom(req.getValidFrom()  != null ? req.getValidFrom().atStartOfDay(ZoneOffset.UTC).toInstant()  : null);
        promo.setValidUntil(req.getValidUntil() != null ? req.getValidUntil().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : null);
        promo.setUpdatedAt(Instant.now());
        return promoRepository.save(promo);
    }

    @Transactional
    public Promotion setActive(Long id, boolean active) {
        Promotion promo = findById(id);
        promo.setActive(active);
        promo.setUpdatedAt(Instant.now());
        return promoRepository.save(promo);
    }

    public List<Promotion> findAll() {
        return promoRepository.findAll();
    }

    public List<Promotion> findActive() {
        Instant now = Instant.now();
        return promoRepository.findByActiveTrue().stream()
                .filter(p -> p.getValidFrom()  == null || !now.isBefore(p.getValidFrom()))
                .filter(p -> p.getValidUntil() == null || !now.isAfter(p.getValidUntil()))
                .toList();
    }

    /**
     * Validates a promo code against the given subtotal and returns the discount amount.
     * Throws IllegalArgumentException (→ 400) if the code is invalid or conditions aren't met.
     */
    public PromoResult applyPromo(String code, BigDecimal subtotal) {
        Promotion promo = promoRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new IllegalArgumentException("Invalid promo code: " + code));

        if (!promo.isActive()) {
            throw new IllegalArgumentException("Promo code is inactive: " + code);
        }

        Instant now = Instant.now();
        if (promo.getValidFrom() != null && now.isBefore(promo.getValidFrom())) {
            throw new IllegalArgumentException("Promo code is not yet valid: " + code);
        }
        if (promo.getValidUntil() != null && now.isAfter(promo.getValidUntil())) {
            throw new IllegalArgumentException("Promo code has expired: " + code);
        }
        if (promo.getMinOrderAmount() != null && subtotal.compareTo(promo.getMinOrderAmount()) < 0) {
            throw new IllegalArgumentException(
                    "Order minimum for promo " + code + " is $" + promo.getMinOrderAmount());
        }

        BigDecimal discount = switch (promo.getType()) {
            case PERCENTAGE -> subtotal
                    .multiply(promo.getValue().divide(new BigDecimal("100")))
                    .setScale(2, RoundingMode.HALF_UP);
            case FIXED_AMOUNT -> promo.getValue().min(subtotal); // never exceed subtotal
        };

        return new PromoResult(promo, discount);
    }

    private Promotion findById(Long id) {
        return promoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found: " + id));
    }

    public record PromoResult(Promotion promotion, BigDecimal discount) {}
}
