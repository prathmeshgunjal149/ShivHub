package com.shivhub.backend.controller;

import java.nio.charset.StandardCharsets;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shivhub.backend.service.RazorpayPaymentService;
import com.shivhub.backend.service.SellerEntitlementService;

@RestController
@RequestMapping("/api/webhooks/razorpay")
public class RazorpayWebhookController {
    private final RazorpayPaymentService payments;
    private final ObjectMapper objectMapper;
    private final SellerEntitlementService subscriptions;
    public RazorpayWebhookController(RazorpayPaymentService payments, ObjectMapper objectMapper, SellerEntitlementService subscriptions) { this.payments = payments; this.objectMapper = objectMapper; this.subscriptions = subscriptions; }
    @PostMapping
    public ResponseEntity<Void> receive(@RequestHeader(name = "x-razorpay-signature", required = false) String signature,
                                        @RequestHeader(name = "x-razorpay-event-id", required = false) String eventId,
                                        @RequestBody byte[] body) {
        String raw = new String(body, StandardCharsets.UTF_8);
        if (!payments.validWebhookSignature(raw, signature)) return ResponseEntity.status(401).build();
        try {
            JsonNode root = objectMapper.readTree(raw); String event = root.path("event").asText();
            JsonNode payment = root.path("payload").path("payment").path("entity");
            JsonNode order = root.path("payload").path("order").path("entity");
            String orderId = payment.path("order_id").asText(order.path("id").asText(null));
            String paymentId = payment.path("id").asText(null);
            if (!subscriptions.completeGatewayPayment(orderId, paymentId)) payments.processWebhook(event, raw, eventId);
        }
        catch (Exception ignored) { return ResponseEntity.status(500).build(); }
        return ResponseEntity.noContent().build();
    }
}
