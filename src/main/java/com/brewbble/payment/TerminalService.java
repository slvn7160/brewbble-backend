package com.brewbble.payment;

import com.brewbble.config.SquareConfig;
import com.brewbble.order.Order;
import com.brewbble.order.OrderRepository;
import com.brewbble.order.OrderStatus;
import com.brewbble.user.AppUser;
import com.squareup.square.SquareClient;
import com.squareup.square.core.SquareApiException;
import com.squareup.square.terminal.types.CancelCheckoutsRequest;
import com.squareup.square.terminal.types.CreateTerminalCheckoutRequest;
import com.squareup.square.terminal.types.GetCheckoutsRequest;
import com.squareup.square.types.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TerminalService {

    private final SquareClient           squareClient;
    private final SquareConfig           squareConfig;
    private final OrderRepository        orderRepository;
    private final PaymentEventRepository paymentEventRepository;

    // ── Initiate Terminal Charge ─────────────────────────────────────────────

    @Transactional
    public TerminalChargeResult initiateCharge(Long orderId, AppUser user, boolean useCancelDevice) {
        Order order = findAndValidateOrder(orderId, user);

        long cents = order.getTotal()
                .multiply(new BigDecimal("100"))
                .longValue();

        Money money = Money.builder()
                .amount(cents)
                .currency(Currency.USD)
                .build();

        // useCancelDevice=true lets employee test the cancel/decline scenario in sandbox
        String deviceId = useCancelDevice
                ? squareConfig.getTerminalDeviceIdCancelTest()
                : squareConfig.getTerminalDeviceId();

        TerminalCheckout checkout = TerminalCheckout.builder()
                .amountMoney(money)
                .deviceOptions(DeviceCheckoutOptions.builder()
                        .deviceId(deviceId)
                        .build())
                .referenceId("brewbble-" + orderId)
                .note("Brewbble Order #" + orderId)
                .build();

        CreateTerminalCheckoutRequest request = CreateTerminalCheckoutRequest.builder()
                .idempotencyKey(UUID.randomUUID().toString())
                .checkout(checkout)
                .build();

        try {
            CreateTerminalCheckoutResponse response =
                    squareClient.terminal().checkouts().create(request);
            TerminalCheckout created = response.getCheckout()
                    .orElseThrow(() -> new IllegalArgumentException("No checkout returned from Square"));
            String checkoutId = created.getId().orElseThrow();

            order.setTerminalCheckoutId(checkoutId);
            order.setPaymentMethod("SQUARE_TERMINAL");
            order.setPaymentStatus(PaymentStatus.PENDING);
            orderRepository.save(order);

            log.info("Terminal checkout initiated for order {} — checkoutId: {}", orderId, checkoutId);
            return new TerminalChargeResult(checkoutId, "PENDING", order.getTotal());

        } catch (SquareApiException e) {
            String detail = extractError(e);
            log.error("Terminal initiation FAILED for order {}: {}", orderId, detail);
            throw new IllegalArgumentException("Terminal charge failed: " + detail);
        }
    }

    // ── Poll Terminal Checkout Status ────────────────────────────────────────
    // Called by the employee POS every 3 seconds while waiting for the customer to tap/insert card

    @Transactional
    public TerminalChargeResult pollStatus(Long orderId, AppUser user) {
        Order order = findAndValidateOrder(orderId, user);

        // Already resolved — return current state without hitting Square
        if (order.getPaymentStatus() == PaymentStatus.PAID ||
                order.getPaymentStatus() == PaymentStatus.FAILED) {
            return new TerminalChargeResult(
                    order.getTerminalCheckoutId(),
                    order.getPaymentStatus().name(),
                    order.getTotal());
        }

        if (order.getTerminalCheckoutId() == null) {
            throw new IllegalArgumentException("No terminal checkout in progress for order " + orderId);
        }

        try {
            GetTerminalCheckoutResponse response = squareClient.terminal().checkouts()
                    .get(GetCheckoutsRequest.builder()
                            .checkoutId(order.getTerminalCheckoutId())
                            .build());

            TerminalCheckout fetched = response.getCheckout()
                    .orElseThrow(() -> new IllegalArgumentException("Checkout not found"));
            String squareStatus = fetched.getStatus().orElse("PENDING");

            if ("COMPLETED".equals(squareStatus)) {
                order.setPaymentStatus(PaymentStatus.PAID);
                order.setStatus(OrderStatus.PREPARING);
                orderRepository.save(order);
                log.info("Terminal payment COMPLETED for order {}", orderId);
            } else if ("CANCELED".equals(squareStatus)) {
                order.setPaymentStatus(PaymentStatus.FAILED);
                orderRepository.save(order);
                log.warn("Terminal checkout CANCELED for order {}", orderId);
            }

            return new TerminalChargeResult(
                    order.getTerminalCheckoutId(),
                    squareStatus,
                    order.getTotal());

        } catch (SquareApiException e) {
            String detail = extractError(e);
            log.error("Terminal poll FAILED for order {}: {}", orderId, detail);
            throw new IllegalArgumentException("Failed to poll terminal status: " + detail);
        }
    }

    // ── Cancel In-Flight Terminal Checkout ──────────────────────────────────

    @Transactional
    public void cancelCharge(Long orderId, AppUser user) {
        Order order = findAndValidateOrder(orderId, user);

        if (order.getTerminalCheckoutId() == null) {
            throw new IllegalArgumentException("No terminal checkout in progress");
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new IllegalArgumentException("Cannot cancel — payment already completed");
        }

        try {
            squareClient.terminal().checkouts()
                    .cancel(CancelCheckoutsRequest.builder()
                            .checkoutId(order.getTerminalCheckoutId())
                            .build());

            order.setPaymentStatus(PaymentStatus.FAILED);
            order.setTerminalCheckoutId(null);
            orderRepository.save(order);

            log.info("Terminal checkout cancelled for order {}", orderId);

        } catch (SquareApiException e) {
            String detail = extractError(e);
            log.error("Terminal cancel FAILED for order {}: {}", orderId, detail);
            throw new IllegalArgumentException("Failed to cancel terminal checkout: " + detail);
        }
    }

    // ── Cash Payment ─────────────────────────────────────────────────────────

    @Transactional
    public PaymentService.PaymentResult collectCash(Long orderId, AppUser user) {
        Order order = findAndValidateOrder(orderId, user);

        order.setPaymentStatus(PaymentStatus.PAID);
        order.setPaymentMethod("CASH");
        order.setStatus(OrderStatus.PREPARING);
        orderRepository.save(order);

        log.info("Cash payment recorded for order {}", orderId);
        return new PaymentService.PaymentResult(null, PaymentStatus.PAID, order.getTotal());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Order findAndValidateOrder(Long orderId, AppUser user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        boolean isStaff = user.getRole().name().equals("ADMIN") ||
                user.getRole().name().equals("EMPLOYEE");
        if (!isStaff) {
            throw new IllegalArgumentException("Only staff can initiate terminal payments");
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new IllegalArgumentException("Order is already paid");
        }
        return order;
    }

    private String extractError(SquareApiException e) {
        if (e.errors() != null && !e.errors().isEmpty()) {
            return e.errors().get(0).getDetail().orElse(e.getMessage());
        }
        return e.getMessage();
    }

    public record TerminalChargeResult(
            String terminalCheckoutId,
            String status,
            BigDecimal amount) {}
}
