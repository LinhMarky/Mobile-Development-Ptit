package com.homely.rental.payment.controller;

import com.homely.rental.payment.dto.WebhookPayload;
import com.homely.rental.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Webhook receiver for mock payment gateway (PAY02).
 */
@RestController
@RequestMapping(path = "/webhooks/payments")
@RequiredArgsConstructor
@Slf4j
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="homely.payment.mock-enabled", havingValue="true")
public class WebhookController {

    private final PaymentService paymentService;
    private final com.homely.rental.payment.service.MockWebhookVerifier verifier;
    private final com.fasterxml.jackson.databind.ObjectMapper mapper;

    // PAY02: Receive webhook
    // This endpoint should typically be public, but signature verification is done inside the service
    @PostMapping("/mock")
    public ResponseEntity<Void> handleMockWebhook(@RequestBody byte[] body,
            @RequestHeader(value="X-Mock-Signature", required=false) String signature) throws java.io.IOException {
        verifier.verify(body, signature);
        WebhookPayload payload;
        try { payload = mapper.readValue(body, WebhookPayload.class); }
        catch (java.io.IOException malformed) { throw new IllegalArgumentException("Invalid webhook JSON"); }
        if (payload == null) throw new IllegalArgumentException("Webhook payload is required");
        paymentService.processWebhook(payload);
        return ResponseEntity.ok().build();
    }
}
