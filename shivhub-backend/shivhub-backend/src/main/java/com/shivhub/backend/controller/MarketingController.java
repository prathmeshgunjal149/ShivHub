package com.shivhub.backend.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.shivhub.backend.dto.MarketingCampaignRequest;
import com.shivhub.backend.entity.MarketingCampaign;
import com.shivhub.backend.entity.MarketingDelivery;
import com.shivhub.backend.service.MarketingService;

@RestController
@RequestMapping("/api")
public class MarketingController {
    private final MarketingService service;
    public MarketingController(MarketingService service) { this.service = service; }
    @GetMapping("/marketing/offers")
    public List<MarketingCampaign> activeOffers(@RequestParam String audience) { return service.activeFor(audience); }
    @GetMapping("/admin/marketing") @PreAuthorize("hasRole('ADMIN')")
    public List<MarketingCampaign> list(@RequestParam(required = false) String type) { return service.list(type); }
    @GetMapping("/admin/marketing/{id}") @PreAuthorize("hasRole('ADMIN')")
    public MarketingCampaign get(@PathVariable Long id) { return service.get(id); }
    @PostMapping("/admin/marketing") @PreAuthorize("hasRole('ADMIN')")
    public MarketingCampaign create(@Valid @RequestBody MarketingCampaignRequest request) { return service.create(request); }
    @PutMapping("/admin/marketing/{id}") @PreAuthorize("hasRole('ADMIN')")
    public MarketingCampaign update(@PathVariable Long id, @Valid @RequestBody MarketingCampaignRequest request) { return service.update(id, request); }
    @DeleteMapping("/admin/marketing/{id}") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.noContent().build(); }
    @PostMapping("/admin/marketing/{id}/send-email") @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Integer> sendEmail(@PathVariable Long id) { return service.sendEmail(id); }
    @GetMapping("/admin/marketing/{id}/history") @PreAuthorize("hasRole('ADMIN')")
    public List<MarketingDelivery> history(@PathVariable Long id) { return service.history(id); }
}
