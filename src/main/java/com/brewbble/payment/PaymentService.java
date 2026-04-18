package com.brewbble.payment;

import com.brewbble.config.SquareConfig;
import com.brewbble.order.Order;
import com.brewbble.order.OrderRepository;
import com.brewbble.order.OrderStatus;
import com.brewbble.user.AppUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.square.SquareClient;
import com.squareup.square.core.SquareApiException;
import com.squareup.square.types.*;
import com.squareup.square.utilities.WebhooksHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final SquareClient           squareClient;
    private final SquareConfig           squareConfig;
    private final OrderRepository        orderRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final ObjectMapper           objectMapper;

    // ── Charge ──────────────────────────────────────────────────────────────

    @Transactional
    public PaymentResult charge(Long orderId, String sourceId, AppUser user) {
        Order order = findOrder(orderId);

        if (!order.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Order does not belong to this user");
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new IllegalArgumentException("Order is already paid");
        }

        order.setPaymentStatus(PaymentStatus.PENDING);
        orderRepository.save(order);

        Money money = buildMoney(order.getTotal());

        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .sourceId(sourceId)
                .idempotencyKey(UUID.randomUUID().toString())
                .amountMoney(money)
                .locationId(squareConfig.getLocationId())
                .referenceId("brewbble-" + orderId)
                .note("Brewbble Order #" + orderId)
                .build();

        try {
            CreatePaymentResponse response = squareClient.payments().create(request);
            Payment payment = response.getPayment().orElseThrow();
            String squarePaymentId = payment.getId().orElse(null);

            order.setPaymentStatus(PaymentStatus.PAID);
            order.setSquarePaymentId(squarePaymentId);
            order.setStatus(OrderStatus.PREPARING);
            orderRepository.save(order);

            saveEvent(UUID.randomUUID().toString(), orderId, squarePaymentId,
                    "payment.completed", order.getTotal(), "PAID", null);

            log.info("Payment PAID for order {} — Square ID: {}", orderId, squarePaymentId);
            return new PaymentResult(squarePaymentId, PaymentStatus.PAID, order.getTotal());

        } catch (SquareApiException e) {
            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);
            String detail = extractError(e);
            saveEvent(UUID.randomUUID().toString(), orderId, null,
                    "payment.failed", order.getTotal(), "FAILED", null);
            log.error("Payment FAILED for order {}: {}", orderId, detail);
            throw new IllegalArgumentException("Payment failed: " + detail);
        } catch (Exception e) {
            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);
            saveEvent(UUID.randomUUID().toString(), orderId, null,
                    "payment.failed", order.getTotal(), "FAILED", null);
            log.error("Payment error for order {}: {}", orderId, e.getMessage(), e);
            throw new IllegalArgumentException("Payment processing error. Please try again.");
        }
    }

    // ── Refund ──────────────────────────────────────────────────────────────

    @Transactional
    public PaymentResult refund(Long orderId) {
        Order order = findOrder(orderId);

        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new IllegalArgumentException("Order is not in a paid state — cannot refund");
        }
        if (order.getSquarePaymentId() == null) {
            throw new IllegalArgumentException("No Square payment ID found for this order");
        }

        Money money = buildMoney(order.getTotal());

        RefundPaymentRequest refundRequest = RefundPaymentRequest.builder()
                .idempotencyKey(UUID.randomUUID().toString())
                .amountMoney(money)
                .paymentId(order.getSquarePaymentId())
                .reason("Brewbble Order #" + orderId + " refund")
                .build();

        try {
            squareClient.refunds().refundPayment(refundRequest);

            order.setPaymentStatus(PaymentStatus.REFUNDED);
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);

            log.info("Refund processed for order {}", orderId);
            return new PaymentResult(order.getSquarePaymentId(), PaymentStatus.REFUNDED, order.getTotal());

        } catch (SquareApiException e) {
            String detail = extractError(e);
            log.error("Refund FAILED for order {}: {}", orderId, detail);
            throw new IllegalArgumentException("Refund failed: " + detail);
        } catch (Exception e) {
            log.error("Refund error for order {}: {}", orderId, e.getMessage(), e);
            throw new IllegalArgumentException("Refund processing error. Please try again.");
        }
    }

    // ── Payment status ───────────────────────────────────────────────────────

    public PaymentResult getStatus(Long orderId, AppUser user) {
        Order order = findOrder(orderId);

        boolean isOwner = order.getUser() != null && order.getUser().getId().equals(user.getId());
        boolean isStaff = user.getRole().name().equals("ADMIN") || user.getRole().name().equals("EMPLOYEE");
        if (!isOwner && !isStaff) {
            throw new IllegalArgumentException("Access denied");
        }
        return new PaymentResult(order.getSquarePaymentId(), order.getPaymentStatus(), order.getTotal());
    }

    // ── Webhook ─────────────────────────────────────────────────────────────

    public boolean verifySignature(String rawBody, String signature, String webhookUrl) {
        try {
            String key = squareConfig.getWebhookSignatureKey();
            // Parameter order: (requestBody, signatureHeader, signatureKey, notificationUrl)
            return WebhooksHelper.verifySignature(rawBody, signature, key, webhookUrl);
        } catch (Exception e) {
            log.error("Webhook signature verification error: {}", e.getMessage());
            return false;
        }
    }

    @Transactional
    public void handleWebhookEvent(String rawBody) {
        try {
            JsonNode root      = objectMapper.readTree(rawBody);
            String rawEventId = root.path("event_id").asText(null);
            String   eventId   = (rawEventId != null && !rawEventId.isBlank())
                    ? rawEventId
                    : "webhook-" + UUID.randomUUID();   // fallback so NOT NULL constraint is never hit
            String   eventType = root.path("type").asText();

            if (rawEventId == null || rawEventId.isBlank()) {
                log.warn("Square webhook missing event_id — assigned fallback id {}", eventId);
            }

            if (paymentEventRepository.existsBySquareEventId(eventId)) {
                log.info("Webhook event {} already processed — skipping", eventId);
                return;
            }

            log.info("Processing Square webhook: {} ({})", eventType, eventId);

            switch (eventType) {
                case "payment.completed"             -> handlePaymentCompleted(root, eventId, rawBody);
                case "payment.failed"                -> handlePaymentFailed(root, eventId, rawBody);
                case "refund.updated"                -> handleRefundCreated(root, eventId, rawBody);
                case "terminal.checkout.completed"   -> handleTerminalCompleted(root, eventId, rawBody);
                case "terminal.checkout.updated"     -> handleTerminalUpdated(root, eventId, rawBody);
                default -> {
                    log.debug("Unhandled Square event type: {}", eventType);
                    saveEvent(eventId, null, null, eventType, null, "IGNORED", rawBody);
                }
            }
        } catch (Exception e) {
            log.error("Failed to process Square webhook: {}", e.getMessage(), e);
        }
    }

    private void handlePaymentCompleted(JsonNode root, String eventId, String rawBody) {
        JsonNode   paymentNode     = root.path("data").path("object").path("payment");
        String     squarePaymentId = paymentNode.path("id").asText();
        String     referenceId     = paymentNode.path("reference_id").asText();
        long       amountCents     = paymentNode.path("total_money").path("amount").asLong();
        BigDecimal amount          = BigDecimal.valueOf(amountCents).movePointLeft(2);

        Long orderId = parseOrderId(referenceId);
        if (orderId != null) {
            orderRepository.findById(orderId).ifPresent(order -> {
                if (order.getPaymentStatus() != PaymentStatus.PAID) {
                    order.setPaymentStatus(PaymentStatus.PAID);
                    order.setSquarePaymentId(squarePaymentId);
                    order.setStatus(OrderStatus.PREPARING);
                    orderRepository.save(order);
                    log.info("Webhook: order {} marked PAID", orderId);
                }
            });
        }
        saveEvent(eventId, orderId, squarePaymentId, "payment.completed", amount, "PAID", rawBody);
    }

    private void handlePaymentFailed(JsonNode root, String eventId, String rawBody) {
        JsonNode paymentNode     = root.path("data").path("object").path("payment");
        String   squarePaymentId = paymentNode.path("id").asText();
        String   referenceId     = paymentNode.path("reference_id").asText();

        Long orderId = parseOrderId(referenceId);
        if (orderId != null) {
            orderRepository.findById(orderId).ifPresent(order -> {
                order.setPaymentStatus(PaymentStatus.FAILED);
                orderRepository.save(order);
                log.warn("Webhook: order {} payment FAILED", orderId);
            });
        }
        saveEvent(eventId, orderId, squarePaymentId, "payment.failed", null, "FAILED", rawBody);
    }

    private void handleRefundCreated(JsonNode root, String eventId, String rawBody) {
        JsonNode   refundNode      = root.path("data").path("object").path("refund");
        String     squarePaymentId = refundNode.path("payment_id").asText();
        long       amountCents     = refundNode.path("amount_money").path("amount").asLong();
        BigDecimal amount          = BigDecimal.valueOf(amountCents).movePointLeft(2);

        orderRepository.findBySquarePaymentId(squarePaymentId).ifPresent(order -> {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            log.info("Webhook: order {} marked REFUNDED", order.getId());
        });
        saveEvent(eventId, null, squarePaymentId, "refund.updated", amount, "REFUNDED", rawBody);
    }

    private void handleTerminalCompleted(JsonNode root, String eventId, String rawBody) {
        JsonNode   checkoutNode      = root.path("data").path("object").path("checkout");
        String     terminalCheckoutId = checkoutNode.path("id").asText();
        String     referenceId        = checkoutNode.path("reference_id").asText();
        long       amountCents        = checkoutNode.path("amount_money").path("amount").asLong();
        BigDecimal amount             = BigDecimal.valueOf(amountCents).movePointLeft(2);

        Long orderId = parseOrderId(referenceId);

        // Look up by terminalCheckoutId for reliability; fall back to referenceId parse
        orderRepository.findByTerminalCheckoutId(terminalCheckoutId).ifPresent(order -> {
            if (order.getPaymentStatus() != PaymentStatus.PAID) {
                order.setPaymentStatus(PaymentStatus.PAID);
                order.setStatus(OrderStatus.PREPARING);
                orderRepository.save(order);
                log.info("Webhook: terminal checkout COMPLETED for order {}", order.getId());
            }
        });

        saveEvent(eventId, orderId, terminalCheckoutId, "terminal.checkout.completed", amount, "PAID", rawBody);
    }

    private void handleTerminalUpdated(JsonNode root, String eventId, String rawBody) {
        JsonNode checkoutNode       = root.path("data").path("object").path("checkout");
        String   terminalCheckoutId = checkoutNode.path("id").asText();
        String   referenceId        = checkoutNode.path("reference_id").asText();
        String   checkoutStatus     = checkoutNode.path("status").asText();

        Long orderId = parseOrderId(referenceId);

        if ("CANCELED".equals(checkoutStatus)) {
            orderRepository.findByTerminalCheckoutId(terminalCheckoutId).ifPresent(order -> {
                order.setPaymentStatus(PaymentStatus.FAILED);
                orderRepository.save(order);
                log.warn("Webhook: terminal checkout CANCELED for order {}", order.getId());
            });
        }

        saveEvent(eventId, orderId, terminalCheckoutId, "terminal.checkout.updated", null,
                checkoutStatus, rawBody);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private Money buildMoney(BigDecimal amount) {
        long cents = amount.multiply(new BigDecimal("100")).longValue();
        return Money.builder()
                .amount(cents)
                .currency(Currency.USD)
                .build();
    }

    private String extractError(SquareApiException e) {
        if (e.errors() != null && !e.errors().isEmpty()) {
            return e.errors().get(0).getDetail().orElse(e.getMessage());
        }
        return e.getMessage();
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }

    private Long parseOrderId(String referenceId) {
        try {
            return Long.parseLong(referenceId.replace("brewbble-", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private void saveEvent(String eventId, Long orderId, String squarePaymentId,
                           String eventType, BigDecimal amount, String status, String rawBody) {
        paymentEventRepository.save(PaymentEvent.builder()
                .squareEventId(eventId)
                .orderId(orderId)
                .squarePaymentId(squarePaymentId)
                .eventType(eventType)
                .amount(amount)
                .status(status)
                .rawPayload(rawBody)
                .build());
    }

    public record PaymentResult(String squarePaymentId, PaymentStatus paymentStatus, BigDecimal amount) {}
}
