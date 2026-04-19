package com.brewbble.order;

import com.brewbble.common.PagedResponse;
import com.brewbble.user.AppUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
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

    /**
     * List orders for kitchen display / admin.
     * Optional ?status=PENDING,PREPARING filter — comma-separated, case-insensitive.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    public PagedResponse<OrderResponse> allOrders(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)    List<String> status) {
        size = Math.min(size, 100);
        List<OrderStatus> statuses = (status == null) ? List.of() :
                status.stream().map(s -> OrderStatus.valueOf(s.toUpperCase())).toList();
        return orderService.getAllOrders(page, size, statuses);
    }

    /**
     * Shift summary — order count, revenue, breakdown by status and payment method.
     * ?date=2026-04-18  or  ?date=today  (default: today)
     */
    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    public ResponseEntity<OrderService.ShiftSummary> shiftSummary(
            @RequestParam(required = false) String date) {
        LocalDate day = (date == null || "today".equalsIgnoreCase(date))
                ? LocalDate.now(java.time.ZoneOffset.UTC)
                : LocalDate.parse(date);
        return ResponseEntity.ok(orderService.getShiftSummary(day));
    }

    @GetMapping("/revenue/today")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    public ResponseEntity<OrderService.TodayRevenue> todayRevenue() {
        return ResponseEntity.ok(orderService.getTodayRevenue());
    }

    /**
     * Flexible revenue report for admins.
     *
     * Usage (pick one):
     *   ?date=2026-04-15              → single day
     *   ?month=2026-04                → full month
     *   ?year=2026                    → full year
     *   ?year=current                 → current year
     *   ?year=previous                → previous year
     *   ?from=2026-04-01&to=2026-04-30 → inclusive date range
     *   (no params)                   → current month
     */
    @GetMapping("/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderService.RevenueReport> revenue(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        LocalDate fromDate;
        LocalDate toExclusive; // end is always exclusive internally

        if (date != null) {
            fromDate    = LocalDate.parse(date);
            toExclusive = fromDate.plusDays(1);

        } else if (month != null) {
            YearMonth ym = YearMonth.parse(month);
            fromDate    = ym.atDay(1);
            toExclusive = ym.plusMonths(1).atDay(1);

        } else if (year != null) {
            int y = switch (year) {
                case "current"  -> LocalDate.now().getYear();
                case "previous" -> LocalDate.now().getYear() - 1;
                default         -> Integer.parseInt(year);
            };
            fromDate    = LocalDate.of(y, 1, 1);
            toExclusive = LocalDate.of(y + 1, 1, 1);

        } else if (from != null && to != null) {
            fromDate    = LocalDate.parse(from);
            toExclusive = LocalDate.parse(to).plusDays(1); // to is inclusive
            if (fromDate.isAfter(toExclusive)) {
                throw new IllegalArgumentException("'from' date must be before 'to' date");
            }

        } else {
            // default: current month
            YearMonth current = YearMonth.now();
            fromDate    = current.atDay(1);
            toExclusive = current.plusMonths(1).atDay(1);
        }

        return ResponseEntity.ok(orderService.getRevenueSummary(fromDate, toExclusive));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
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
