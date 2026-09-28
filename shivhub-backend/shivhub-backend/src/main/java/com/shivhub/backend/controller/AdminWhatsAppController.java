package com.shivhub.backend.controller;

import com.shivhub.backend.entity.WhatsAppDeliveryLog;
import com.shivhub.backend.dto.WhatsAppCampaignMappingRequest;
import com.shivhub.backend.repository.WhatsAppDeliveryLogRepository;
import com.shivhub.backend.service.WhatsAppNotificationService;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

/** Safe operational visibility; provider secrets remain environment-only. */
@RestController
@RequestMapping("/api/admin/whatsapp")
@PreAuthorize("hasRole('ADMIN')")
public class AdminWhatsAppController {
    private final WhatsAppNotificationService whatsapp;
    private final WhatsAppDeliveryLogRepository logs;
    public AdminWhatsAppController(WhatsAppNotificationService whatsapp, WhatsAppDeliveryLogRepository logs) { this.whatsapp=whatsapp; this.logs=logs; }
    @GetMapping("/diagnostics") public Map<String, Object> diagnostics() { return Map.of("configured", whatsapp.isConfigured(), "provider", whatsapp.configuredProvider(), "providerCredentialsStoredIn", "environment variables only"); }
    @GetMapping("/events") public List<WhatsAppNotificationService.EventConfiguration> events() { return whatsapp.eventConfigurations(); }
    @PutMapping("/campaign-mappings/{eventKey}")
    public WhatsAppNotificationService.EventConfiguration updateCampaignMapping(
            @PathVariable String eventKey, @Valid @RequestBody WhatsAppCampaignMappingRequest request) {
        return whatsapp.updateCampaignMapping(eventKey, request);
    }
    @GetMapping("/delivery-logs") public Page<WhatsAppDeliveryLog> logs(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="30") int size) { return logs.findAllByOrderByCreatedAtDesc(PageRequest.of(Math.max(0,page), Math.min(100,Math.max(1,size)))); }
}
