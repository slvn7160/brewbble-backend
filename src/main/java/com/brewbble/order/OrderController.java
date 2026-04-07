package com.brewbble.order;

import com.brewbble.user.AppUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @Valid @RequestBody PlaceOrderRequest request,
            @AuthenticationPrincipal AppUser user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.placeOrder(request, user));
    }

    @GetMapping("/my")
    public List<OrderResponse> myOrders(@AuthenticationPrincipal AppUser user) {
        return orderService.getMyOrders(user.getId());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<OrderResponse> allOrders() {
        return orderService.getAllOrders();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        OrderStatus status;
        try {
            status = OrderStatus.valueOf(body.get("status").toUpperCase());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(orderService.updateStatus(id, status));
    }
}
