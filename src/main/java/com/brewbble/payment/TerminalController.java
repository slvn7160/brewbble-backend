package com.brewbble.payment;

import com.brewbble.user.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/instore/orders")
@RequiredArgsConstructor
public class TerminalController {

    private final TerminalService terminalService;

    /**
     * Initiate a terminal charge — employee taps "Charge via Card Reader".
     * ?simulate=true routes to the cancel-test device to simulate buyer cancellation in sandbox.
     */
    @PostMapping("/{orderId}/terminal-charge")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN')")
    public ResponseEntity<TerminalService.TerminalChargeResult> initiateCharge(
            @PathVariable Long orderId,
            @RequestParam(defaultValue = "false") boolean simulate,
            @AuthenticationPrincipal AppUser user) {
        return ResponseEntity.ok(terminalService.initiateCharge(orderId, user, simulate));
    }

    /**
     * Poll terminal checkout status — frontend calls this every 3 seconds.
     * Check the {@code status} field: PENDING | COMPLETED | CANCELED.
     */
    @GetMapping("/{orderId}/terminal-status")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN')")
    public ResponseEntity<TerminalService.TerminalChargeResult> pollStatus(
            @PathVariable Long orderId,
            @AuthenticationPrincipal AppUser user) {
        return ResponseEntity.ok(terminalService.pollStatus(orderId, user));
    }

    /**
     * Cancel an in-flight terminal checkout — employee taps "Cancel".
     */
    @DeleteMapping("/{orderId}/terminal-charge")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN')")
    public ResponseEntity<Void> cancelCharge(
            @PathVariable Long orderId,
            @AuthenticationPrincipal AppUser user) {
        terminalService.cancelCharge(orderId, user);
        return ResponseEntity.noContent().build();
    }

    /**
     * Record a cash payment — marks the order PAID immediately.
     */
    @PostMapping("/{orderId}/cash-payment")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN')")
    public ResponseEntity<PaymentService.PaymentResult> collectCash(
            @PathVariable Long orderId,
            @AuthenticationPrincipal AppUser user) {
        return ResponseEntity.ok(terminalService.collectCash(orderId, user));
    }
}
