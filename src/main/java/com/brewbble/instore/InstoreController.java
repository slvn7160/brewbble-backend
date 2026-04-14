package com.brewbble.instore;

import com.brewbble.auth.AuthService;
import com.brewbble.auth.RegisterRequest;
import com.brewbble.order.OrderResponse;
import com.brewbble.order.OrderService;
import com.brewbble.order.PlaceOrderRequest;
import com.brewbble.user.AppUser;
import com.brewbble.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/instore")
@PreAuthorize("hasRole('EMPLOYEE') or hasRole('ADMIN')")
@RequiredArgsConstructor
public class InstoreController {

    private final OrderService    orderService;
    private final UserRepository  userRepository;
    private final AuthService     authService;

    /**
     * Place an in-store order.
     * - If customerEmail is provided and account exists → order linked to customer, points earned/redeemed.
     * - If customerEmail is omitted → guest order, no rewards.
     */
    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody InstoreOrderRequest req) {
        AppUser customer = null;
        if (req.getCustomerEmail() != null && !req.getCustomerEmail().isBlank()) {
            customer = userRepository.findByEmail(req.getCustomerEmail())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No account found for " + req.getCustomerEmail() + ". Register the customer first."));
        }
        OrderResponse response = orderService.placeInstoreOrder(
                req.getItems(), req.getNotes(), req.isRedeemPoints(), customer);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Look up a customer by email — use this before placing an in-store order
     * to check if they have an account and see their reward balance.
     */
    @GetMapping("/customers/lookup")
    public ResponseEntity<CustomerSummary> lookup(@RequestParam String email) {
        return userRepository.findByEmail(email)
                .map(u -> ResponseEntity.ok(CustomerSummary.from(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Register a new customer on behalf of the employee.
     * Returns the customer profile (no JWT — customer logs in themselves later).
     */
    @PostMapping("/customers")
    public ResponseEntity<CustomerSummary> registerCustomer(@Valid @RequestBody RegisterRequest req) {
        AppUser created = authService.createCustomer(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(CustomerSummary.from(created));
    }

    // ── DTOs ────────────────────────────────────────────────────────────────

    @Data
    public static class InstoreOrderRequest {
        private String customerEmail;   // optional — omit for guest
        private boolean redeemPoints;

        @NotEmpty
        private List<PlaceOrderRequest.OrderLineRequest> items;
        private String notes;
    }

    @Data
    public static class CustomerSummary {
        private Long id;
        private String name;
        private String email;
        private int rewardPoints;
        private Instant createdAt;

        public static CustomerSummary from(AppUser u) {
            CustomerSummary s = new CustomerSummary();
            s.id           = u.getId();
            s.name         = u.getName();
            s.email        = u.getEmail();
            s.rewardPoints = u.getRewardPoints();
            s.createdAt    = u.getCreatedAt();
            return s;
        }
    }
}
