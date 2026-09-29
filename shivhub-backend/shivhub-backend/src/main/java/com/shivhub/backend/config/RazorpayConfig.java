package com.shivhub.backend.config;

import org.springframework.context.annotation.Configuration;
import com.razorpay.RazorpayClient;
import com.shivhub.backend.service.AdminIntegrationCredentialService;
import org.springframework.beans.factory.annotation.Value;

/**
 * Resolves Razorpay settings from encrypted admin storage or environment fallback. The client is constructed lazily
 * so the application can still run in environments where gateway keys are not
 * configured; payment creation then returns a safe configuration error.
 */
@Configuration
public class RazorpayConfig {
    @Value("${razorpay.webhook-secret:}") private String webhookSecret;
    private final AdminIntegrationCredentialService credentials;

    public RazorpayConfig(AdminIntegrationCredentialService credentials) {
        this.credentials = credentials;
    }

    public String getKeyId() { return credentials.razorpay().publicValue(); }
    public String getWebhookSecret() { return webhookSecret; }
    public boolean isConfigured() { return credentials.razorpay().complete(); }
    public RazorpayClient client() {
        if (!isConfigured()) throw new IllegalStateException("Razorpay is not configured for this environment");
        try { return new RazorpayClient(getKeyId(), getKeySecret()); }
        catch (Exception exception) { throw new IllegalStateException("Unable to initialise Razorpay", exception); }
    }
    public String getKeySecret() { return credentials.razorpay().secret(); }
}
