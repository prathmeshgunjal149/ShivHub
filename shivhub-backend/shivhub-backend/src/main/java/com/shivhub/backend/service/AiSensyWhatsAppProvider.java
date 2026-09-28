package com.shivhub.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * AiSensy API-campaign adapter. It deliberately accepts an AiSensy campaign
 * name, rather than a Meta template name: AiSensy sends an approved template
 * through an API campaign that has first been made LIVE in its dashboard.
 */
@Service
public class AiSensyWhatsAppProvider {
    private final ObjectMapper mapper;
    private final boolean enabled;
    private final String apiKey;
    private final String apiUrl;
    private final String source;
    private final HttpClient client = HttpClient.newBuilder().build();

    public AiSensyWhatsAppProvider(
            ObjectMapper mapper,
            @Value("${shivhub.whatsapp.aisensy.enabled:false}") boolean enabled,
            @Value("${shivhub.whatsapp.aisensy.api-key:}") String apiKey,
            @Value("${shivhub.whatsapp.aisensy.api-url:https://backend.aisensy.com/campaign/t1/api/v2}") String apiUrl,
            @Value("${shivhub.whatsapp.aisensy.source:ShivHub Backend}") String source) {
        this.mapper = mapper;
        this.enabled = enabled;
        this.apiKey = clean(apiKey);
        this.apiUrl = clean(apiUrl);
        this.source = clean(source).isBlank() ? "ShivHub Backend" : clean(source);
    }

    public boolean isConfigured() {
        return enabled && !apiKey.isBlank() && !apiUrl.isBlank();
    }

    public ProviderResult send(String campaignName, String mobile, String userName,
            List<String> templateParams) {
        return send(campaignName, mobile, userName, templateParams, List.of());
    }

    /**
     * Authentication templates with a Copy Code button require the OTP in the
     * button parameters as well as in the message parameters. Other templates
     * deliberately send an empty button list.
     */
    public ProviderResult send(String campaignName, String mobile, String userName,
            List<String> templateParams, List<String> buttons) {
        return send(campaignName, mobile, userName, templateParams, buttons, null);
    }

    public ProviderResult send(String campaignName, String mobile, String userName,
            List<String> templateParams, List<String> buttons, String imageUrl) {
        if (!isConfigured()) return ProviderResult.failed("AiSensy is not configured");
        if (clean(campaignName).isBlank()) return ProviderResult.failed("AiSensy API campaign is not configured for this event");
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("apiKey", apiKey);
            payload.put("campaignName", campaignName.trim());
            payload.put("destination", internationalMobile(mobile));
            payload.put("userName", clean(userName).isBlank() ? "ShivHub customer" : clean(userName));
            payload.put("source", source);
            if (templateParams != null && !templateParams.isEmpty()) payload.put("templateParams", templateParams);
            List<Map<String, Object>> apiButtons = authenticationButtons(buttons);
            if (!apiButtons.isEmpty()) payload.put("buttons", apiButtons);
            if (imageUrl != null && !imageUrl.isBlank()) {
                payload.put("media", Map.of("url", imageUrl, "filename", "offer-image"));
            }

            HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return ProviderResult.failed("AiSensy returned HTTP " + response.statusCode() + ": " + safe(response.body()));
            }
            return ProviderResult.accepted("AISENSY_ACCEPTED_" + response.statusCode());
        } catch (Exception error) {
            return ProviderResult.failed(safe(error.getMessage()));
        }
    }

    /**
     * AiSensy authentication campaigns require the OTP in the URL button's
     * nested text parameter. Sending a raw string array is rejected by the
     * API even when the message parameter is valid.
     */
    private List<Map<String, Object>> authenticationButtons(List<String> buttons) {
        if (buttons == null || buttons.isEmpty()) return List.of();

        String code = clean(buttons.get(0));
        if (code.isBlank()) return List.of();

        Map<String, Object> parameter = Map.of("type", "text", "text", code);
        Map<String, Object> button = new LinkedHashMap<>();
        button.put("type", "button");
        button.put("sub_type", "url");
        button.put("index", 0);
        button.put("parameters", List.of(parameter));
        return List.of(button);
    }

    private String internationalMobile(String value) {
        String digits = clean(value).replaceAll("\\D", "");
        if (digits.length() == 10) return "+91" + digits;
        if (digits.startsWith("00")) return "+" + digits.substring(2);
        return "+" + digits;
    }

    private String clean(String value) { return value == null ? "" : value.trim(); }
    private String safe(String value) {
        String text = value == null || value.isBlank() ? "Provider request failed" : value;
        text = text.replaceAll("(?i)apiKey[\\\"=: ]+[A-Za-z0-9._-]+", "apiKey=[redacted]");
        return text.substring(0, Math.min(text.length(), 500));
    }

    public record ProviderResult(boolean accepted, String detail) {
        static ProviderResult accepted(String detail) { return new ProviderResult(true, detail); }
        static ProviderResult failed(String detail) { return new ProviderResult(false, detail); }
    }
}
