package com.shivhub.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.entity.WhatsAppCampaignMapping;
import com.shivhub.backend.entity.WhatsAppDeliveryLog;
import com.shivhub.backend.dto.WhatsAppCampaignMappingRequest;
import com.shivhub.backend.repository.UserRepository;
import com.shivhub.backend.repository.WhatsAppCampaignMappingRepository;
import com.shivhub.backend.repository.WhatsAppDeliveryLogRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * Single provider adapter for transactional WhatsApp notifications. It is a
 * no-op until a real provider URL, token and sender are configured; it never
 * claims that a message was delivered without a provider success response.
 */
@Service
public class WhatsAppNotificationService {
    private final UserRepository users;
    private final WhatsAppDeliveryLogRepository logs;
    private final WhatsAppCampaignMappingRepository campaignMappings;
    private final ObjectMapper mapper;
    private final boolean enabled;
    private final String providerUrl;
    private final String token;
    private final String sender;
    private final String metaTemplateName;
    private final String metaOtpTemplateName;
    private final String metaTemplateLanguage;
    private final String provider;
    private final AiSensyWhatsAppProvider aiSensy;
    private final Environment environment;
    private final HttpClient client = HttpClient.newBuilder().build();

    public WhatsAppNotificationService(UserRepository users, WhatsAppDeliveryLogRepository logs,
            WhatsAppCampaignMappingRepository campaignMappings, ObjectMapper mapper,
            @Value("${shivhub.whatsapp.enabled:false}") boolean enabled,
            @Value("${shivhub.whatsapp.provider-url:}") String providerUrl,
            @Value("${shivhub.whatsapp.token:}") String token,
            @Value("${shivhub.whatsapp.sender:}") String sender,
            @Value("${shivhub.whatsapp.meta-template-name:}") String metaTemplateName,
            @Value("${shivhub.whatsapp.meta-otp-template-name:}") String metaOtpTemplateName,
            @Value("${shivhub.whatsapp.meta-template-language:en_US}") String metaTemplateLanguage,
            @Value("${shivhub.whatsapp.provider:meta}") String provider,
            AiSensyWhatsAppProvider aiSensy,
            Environment environment) {
        this.users = users; this.logs = logs; this.campaignMappings = campaignMappings; this.mapper = mapper; this.enabled = enabled;
        this.providerUrl = providerUrl == null ? "" : providerUrl.trim();
        this.token = token == null ? "" : token.trim(); this.sender = sender == null ? "" : sender.trim();
        this.metaTemplateName = metaTemplateName == null ? "" : metaTemplateName.trim();
        this.metaOtpTemplateName = metaOtpTemplateName == null ? "" : metaOtpTemplateName.trim();
        this.metaTemplateLanguage = metaTemplateLanguage == null || metaTemplateLanguage.isBlank() ? "en_US" : metaTemplateLanguage.trim();
        this.provider = provider == null ? "meta" : provider.trim().toLowerCase();
        this.aiSensy = aiSensy;
        this.environment = environment;
    }

    public boolean isConfigured() {
        return isAiSensy() ? aiSensy.isConfigured() : enabled && !providerUrl.isBlank() && !token.isBlank() && !sender.isBlank();
    }

    public String configuredProvider() { return isAiSensy() ? "AISENSY" : "META"; }
    public boolean usesAiSensy() { return isAiSensy(); }

    /** Admin-safe status for every supported notification variant; credentials are never exposed. */
    public List<EventConfiguration> eventConfigurations() {
        Map<String, WhatsAppCampaignMapping> mappings = new HashMap<>();
        campaignMappings.findAll().forEach(mapping -> mappings.put(mapping.getEventKey(), mapping));
        return Arrays.stream(WhatsAppNotificationEvent.values())
                .map(event -> eventConfiguration(event, mappings.get(event.configKey())))
                .toList();
    }

    /** Stores only template/campaign metadata; provider credentials stay in the server environment. */
    public EventConfiguration updateCampaignMapping(String eventKey, WhatsAppCampaignMappingRequest request) {
        WhatsAppNotificationEvent event = Arrays.stream(WhatsAppNotificationEvent.values())
                .filter(candidate -> candidate.configKey().equals(eventKey))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown WhatsApp event: " + eventKey));
        String templateName = cleanMappingValue(request.templateName());
        String campaignName = cleanMappingValue(request.campaignName());
        if (request.enabled() && campaignName.isBlank()) {
            throw new IllegalArgumentException("Enter the live AiSensy campaign name before enabling this event");
        }
        WhatsAppCampaignMapping mapping = campaignMappings.findByEventKey(event.configKey())
                .orElseGet(WhatsAppCampaignMapping::new);
        mapping.setEventKey(event.configKey());
        mapping.setTemplateName(templateName);
        mapping.setCampaignName(campaignName);
        mapping.setEnabled(request.enabled());
        return eventConfiguration(event, campaignMappings.save(mapping));
    }

    /** Mirrors customer email after SMTP succeeds. Failures are captured and never bubble to the sale/order flow. */
    public void mirrorCustomerEmail(String recipientEmail, String subject, String body) {
        try {
            // AiSensy requires an event-specific LIVE campaign and exact template
            // parameters. Event methods below dispatch it explicitly instead of
            // guessing a campaign from arbitrary email text.
            if (isAiSensy()) return;
            if (!isConfigured() || recipientEmail == null || recipientEmail.isBlank()) return;
            users.findByEmailIgnoreCase(recipientEmail.trim()).filter(this::eligibleWhatsAppRecipient).ifPresent(user -> {
                send(user.getMobile(), subject, shortMessage(subject, body));
            });
        } catch (Exception ignored) {
            // WhatsApp is secondary: a database/provider failure can never undo a committed sale or email.
        }
    }

    /** OTP is intentionally not mirrored through email and is sent only when a provider accepts it. */
    public boolean sendRegistrationOtp(User user, String otp) {
        if (user == null || !validMobile(user.getMobile()) || !isConfigured()) return false;
        if (isAiSensy()) {
            return sendAiSensy(user, WhatsAppNotificationEvent.OTP_REGISTRATION, user.getName(), "Registration OTP",
                    List.of(otp == null ? "" : otp));
        }
        return send(user.getMobile(), "ShivHub verification code",
                "Your ShivHub WhatsApp verification code is " + otp + ". It expires in 5 minutes. Do not share this code.",
                metaOtpTemplateName);
    }

    private boolean eligibleWhatsAppRecipient(User user) {
        if (user == null || !user.isWhatsappOptIn() || !validMobile(user.getMobile())) return false;
        return switch (user.getRole()) {
            case CUSTOMER -> user.isWhatsappVerified();
            case SELLER, STAFF -> true;
            case ADMIN -> false;
        };
    }

    /**
     * Sends an approved AiSensy API campaign for a known ShivHub business event.
     * It is intentionally a no-op for Meta mode so the migration can be enabled
     * only after every AiSensy campaign is LIVE and configured.
     */
    public boolean sendCustomerEvent(String recipientEmail, WhatsAppNotificationEvent event,
            String customerName, String reference, List<String> templateParams) {
        return sendUserEvent(recipientEmail, event, customerName, reference, templateParams);
    }

    /**
     * Sends a configured AiSensy event to an opted-in ShivHub user. Customers
     * must have verified WhatsApp; seller and staff business notifications use
     * the opted-in mobile captured during onboarding.
     */
    public boolean sendUserEvent(String recipientEmail, WhatsAppNotificationEvent event,
            String userName, String reference, List<String> templateParams) {
        if (!isAiSensy() || !aiSensy.isConfigured() || recipientEmail == null || recipientEmail.isBlank()) return false;
        return users.findByEmailIgnoreCase(recipientEmail.trim()).filter(this::eligibleWhatsAppRecipient)
                .map(user -> sendAiSensy(user, event,
                        userName == null || userName.isBlank() ? user.getName() : userName,
                        reference, templateParams)).orElse(false);
    }

    /**
     * Used only after a seller has recorded explicit WhatsApp consent on a POS
     * bill. It supports walk-in buyers who do not yet have a ShivHub account.
     */
    public boolean sendOptedInMobileEvent(String mobile, WhatsAppNotificationEvent event,
            String userName, String reference, List<String> templateParams) {
        if (!isAiSensy() || !aiSensy.isConfigured() || !validMobile(mobile)) return false;
        return sendAiSensy(mobile, event, userName, reference, templateParams);
    }

    private boolean sendAiSensy(User user, WhatsAppNotificationEvent event, String userName,
            String reference, List<String> templateParams) {
        return sendAiSensy(user.getMobile(), event, userName, reference, templateParams);
    }

    private boolean sendAiSensy(String mobile, WhatsAppNotificationEvent event, String userName,
            String reference, List<String> templateParams) {
        return sendAiSensy(mobile, event, userName, reference, templateParams, null);
    }

    public boolean sendOptedInMobileImageOffer(String mobile, String userName,
            String reference, List<String> templateParams, String imageUrl) {
        if (!isAiSensy() || !aiSensy.isConfigured() || !validMobile(mobile)) return false;
        return sendAiSensy(mobile, WhatsAppNotificationEvent.OFFER_IMAGE_NOTIFICATION,
                userName, reference, templateParams, imageUrl);
    }

    public boolean imageOffersConfigured() {
        return isAiSensy() && aiSensy.isConfigured()
                && !campaignFor(WhatsAppNotificationEvent.OFFER_IMAGE_NOTIFICATION).isBlank();
    }

    private boolean sendAiSensy(String mobile, WhatsAppNotificationEvent event, String userName,
            String reference, List<String> templateParams, String imageUrl) {
        String campaign = campaignFor(event);
        String eventKey = sha256(mobile + "|AISENSY|" + event.name() + "|" + safe(reference) + "|" + String.valueOf(templateParams) + (imageUrl == null ? "" : "|" + imageUrl));
        WhatsAppDeliveryLog prior = logs.findTopByRecipientAndEventKeyOrderByCreatedAtDesc(mobile, eventKey).orElse(null);
        if (prior != null && "SENT".equals(prior.getStatus())) return true;
        WhatsAppDeliveryLog log = prior == null ? new WhatsAppDeliveryLog() : prior;
        log.setRecipient(mobile); log.setEventKey(eventKey); log.setEventType(event.configKey()); log.setStatus("PENDING"); log.setFailureReason(null); logs.save(log);
        List<String> parameters = templateParams == null ? List.of() : templateParams;
        List<String> buttons = event == WhatsAppNotificationEvent.OTP_REGISTRATION || event == WhatsAppNotificationEvent.OTP_LOGIN
                ? parameters : List.of();
        AiSensyWhatsAppProvider.ProviderResult result = aiSensy.send(campaign, mobile, userName, parameters, buttons, imageUrl);
        if (result.accepted()) {
            log.setStatus("SENT"); log.setProviderStatus(result.detail()); log.setSentAt(LocalDateTime.now()); logs.save(log); return true;
        }
        log.setStatus(campaign.isBlank() ? "SKIPPED" : "FAILED"); log.setFailureReason(result.detail()); logs.save(log); return false;
    }

    private boolean send(String mobile, String subject, String message) {
        return send(mobile, subject, message, "");
    }

    private boolean send(String mobile, String subject, String message, String templateOverride) {
        String eventKey = sha256(mobile + "|" + subject + "|" + message);
        WhatsAppDeliveryLog prior = logs.findTopByRecipientAndEventKeyOrderByCreatedAtDesc(mobile, eventKey).orElse(null);
        if (prior != null && "SENT".equals(prior.getStatus())) return true;
        WhatsAppDeliveryLog log = prior == null ? new WhatsAppDeliveryLog() : prior;
        log.setRecipient(mobile); log.setEventKey(eventKey); log.setEventType(templateOverride == null || templateOverride.isBlank() ? "general-update" : "otp-registration"); log.setStatus("PENDING"); log.setFailureReason(null);
        log = logs.save(log);
        try {
            String json = mapper.writeValueAsString(providerPayload(mobile, message, templateOverride));
            HttpRequest request = HttpRequest.newBuilder(URI.create(providerUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Provider returned HTTP " + response.statusCode() + ": " + safeProviderBody(response.body()));
            }
            log.setStatus("SENT"); log.setProviderStatus("ACCEPTED_" + response.statusCode()); log.setSentAt(LocalDateTime.now()); logs.save(log);
            return true;
        } catch (Exception error) {
            log.setStatus("FAILED"); log.setFailureReason(safeFailure(error)); logs.save(log);
            return false;
        }
    }

    private String shortMessage(String subject, String body) {
        String plain = (body == null ? "" : body.replaceAll("(?s)<[^>]*>", " ")).replaceAll("\\s+", " ").trim();
        String message = "ShivHub: " + (subject == null ? "Update" : subject.trim()) + "\n" + plain;
        return message.length() > 1500 ? message.substring(0, 1497) + "..." : message;
    }
    /** Meta Cloud API expects a WhatsApp message envelope, while other configured providers keep the legacy payload. */
    private Map<String, Object> providerPayload(String mobile, String message, String templateOverride) {
        if (providerUrl.contains("graph.facebook.com")) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("messaging_product", "whatsapp");
            payload.put("recipient_type", "individual");
            payload.put("to", whatsappRecipient(mobile));
            String selectedTemplate = templateOverride == null || templateOverride.isBlank() ? metaTemplateName : templateOverride;
            if (!selectedTemplate.isBlank()) {
                String templateMessage = message.length() > 900 ? message.substring(0, 897) + "..." : message;
                payload.put("type", "template");
                payload.put("template", Map.of(
                        "name", selectedTemplate,
                        "language", Map.of("code", metaTemplateLanguage),
                        "components", java.util.List.of(Map.of(
                                "type", "body",
                                "parameters", java.util.List.of(Map.of("type", "text", "text", templateMessage))))));
            } else {
                payload.put("type", "text");
                payload.put("text", Map.of("preview_url", false, "body", message));
            }
            return payload;
        }
        return Map.of("to", mobile, "from", sender, "message", message);
    }
    private boolean validMobile(String value) { return value != null && value.trim().matches("(?:\\+91)?[6-9]\\d{9}"); }
    private String whatsappRecipient(String value) {
        String digits = value == null ? "" : value.replaceAll("\\D", "");
        if (digits.length() == 10) return "91" + digits;
        if (digits.startsWith("00")) return digits.substring(2);
        return digits;
    }
    private String safeProviderBody(String body) {
        String text = body == null ? "" : body.replaceAll("(?i)Bearer\\s+[A-Za-z0-9._-]+", "Bearer [redacted]");
        return text.length() > 450 ? text.substring(0, 450) : text;
    }
    private String safeFailure(Exception error) { String text = error.getMessage() == null ? "Provider request failed" : error.getMessage(); return text.length() > 500 ? text.substring(0, 500) : text; }
    private String safe(String value) { return value == null ? "" : value.length() > 300 ? value.substring(0, 300) : value; }
    private boolean isAiSensy() { return "aisensy".equals(provider); }

    private EventConfiguration eventConfiguration(WhatsAppNotificationEvent event, WhatsAppCampaignMapping mapping) {
        String templateName = mapping == null ? "" : safeMappingValue(mapping.getTemplateName());
        String campaignName = mapping == null
                ? environmentCampaign(event)
                : safeMappingValue(mapping.getCampaignName());
        boolean enabled = mapping == null || mapping.isEnabled();
        boolean configured = isAiSensy() ? enabled && !campaignName.isBlank() : isConfigured();
        return new EventConfiguration(event.configKey(), configured, templateName, campaignName, enabled,
                mapping == null ? "ENVIRONMENT" : "ADMIN");
    }

    private String campaignFor(WhatsAppNotificationEvent event) {
        return campaignMappings.findByEventKey(event.configKey())
                .map(mapping -> mapping.isEnabled() ? safeMappingValue(mapping.getCampaignName()) : "")
                .orElseGet(() -> environmentCampaign(event));
    }

    private String environmentCampaign(WhatsAppNotificationEvent event) {
        return environment.getProperty("shivhub.whatsapp.aisensy.campaigns." + event.configKey(), "").trim();
    }

    private String cleanMappingValue(String value) {
        String clean = safeMappingValue(value);
        if (clean.contains("\n") || clean.contains("\r")) {
            throw new IllegalArgumentException("Template and campaign names must be a single line");
        }
        return clean;
    }

    private String safeMappingValue(String value) { return value == null ? "" : value.trim(); }
    private String sha256(String value) { try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception error) { throw new IllegalStateException("Unable to create notification key", error); } }

    public record EventConfiguration(String key, boolean campaignConfigured, String templateName,
            String campaignName, boolean enabled, String source) { }
}
