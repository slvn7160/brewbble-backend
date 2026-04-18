package com.brewbble.payment;

import com.brewbble.user.AppUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Customer charges their order.
     * Frontend gets sourceId from Square Web Payments SDK and sends it here.
     */
    @PostMapping("/charge")
    public ResponseEntity<PaymentService.PaymentResult> charge(
            @Valid @RequestBody ChargeRequest req,
            @AuthenticationPrincipal AppUser user) {
        return ResponseEntity.ok(paymentService.charge(req.getOrderId(), req.getSourceId(), user));
    }

    /** Get payment status for an order. Accessible by the order owner, ADMIN, or EMPLOYEE. */
    @GetMapping("/{orderId}")
    public ResponseEntity<PaymentService.PaymentResult> status(
            @PathVariable Long orderId,
            @AuthenticationPrincipal AppUser user) {
        return ResponseEntity.ok(paymentService.getStatus(orderId, user));
    }

    /** Full refund — accessible by ADMIN or EMPLOYEE. */
    @PostMapping("/{orderId}/refund")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    public ResponseEntity<PaymentService.PaymentResult> refund(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.refund(orderId));
    }

    @Data
    public static class ChargeRequest {
        @NotNull
        private Long orderId;

        @NotBlank
        private String sourceId;  // nonce from Square Web Payments SDK
    }
}
