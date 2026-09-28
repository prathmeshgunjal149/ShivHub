package com.shivhub.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import com.razorpay.RazorpayClient;

/**
 * Holds environment-only Razorpay settings.  The client is constructed lazily
 * so the application can still run in environments where gateway keys are not
 * configured; payment creation then returns a safe configuration error.
 */
@Configuration
public class RazorpayConfig {
    @Value("${razorpay.key-id:}") private String keyId;
    @Value("${razorpay.key-secret:}") private String keySecret;
    @Value("${razorpay.webhook-secret:}") private String webhookSecret;

    public String getKeyId() { return keyId; }
    public String getWebhookSecret() { return webhookSecret; }
    public boolean isConfigured() { return notBlank(keyId) && notBlank(keySecret); }
    public RazorpayClient client() {
        if (!isConfigured()) throw new IllegalStateException("Razorpay is not configured for this environment");
        try { return new RazorpayClient(keyId, keySecret); }
        catch (Exception exception) { throw new IllegalStateException("Unable to initialise Razorpay", exception); }
    }
    public String getKeySecret() { return keySecret; }
    private boolean notBlank(String value) { return value != null && !value.isBlank(); }
}
