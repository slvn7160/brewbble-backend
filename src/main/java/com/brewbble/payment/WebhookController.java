package com.brewbble.payment;

import com.brewbble.config.SquareConfig;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Receives Square webhook events.
 * No JWT auth — authenticated via Square's HMAC-SHA256 signature instead.
 * Endpoint must be registered in the Square Developer Dashboard.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class WebhookController {

    private final PaymentService paymentService;
    private final SquareConfig   squareConfig;

    @PostMapping(value = "/webhook", consumes = "application/json")
    public ResponseEntity<Void> webhook(
            @RequestHeader(value = "x-square-hmacsha256-signature", required = false) String signature,
            HttpServletRequest request) throws IOException {

        // Read raw bytes directly — avoids Spring charset conversion altering the body
        byte[] bodyBytes = request.getInputStream().readAllBytes();
        String rawBody = new String(bodyBytes, StandardCharsets.UTF_8);

        log.info("Square webhook received — size={}b from={}", rawBody.length(), request.getRemoteAddr());

        if (squareConfig.isWebhookVerifySignature()) {
            if (signature == null) {
                log.warn("Rejected Square webhook — missing signature header from {}", request.getRemoteAddr());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            String webhookUrl = squareConfig.getWebhookUrl();
            boolean valid = paymentService.verifySignature(rawBody, signature, webhookUrl);
            log.debug("Webhook signature check: url={} valid={}", webhookUrl, valid);
            if (!valid) {
                log.warn("Rejected Square webhook — signature mismatch (url={}) from={}", webhookUrl, request.getRemoteAddr());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        } else {
            log.warn("Webhook signature verification DISABLED — sandbox mode only");
        }

        paymentService.handleWebhookEvent(rawBody);
        // Always return 200 quickly — Square retries on non-2xx
        return ResponseEntity.ok().build();
    }
}
