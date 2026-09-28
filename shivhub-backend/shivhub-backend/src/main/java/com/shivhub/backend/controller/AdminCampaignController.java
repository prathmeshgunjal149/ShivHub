package com.shivhub.backend.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import com.shivhub.backend.dto.AdminCampaignRequest;
import com.shivhub.backend.dto.CampaignScheduleRequest;
import com.shivhub.backend.dto.CampaignTestRequest;
import com.shivhub.backend.entity.MarketingCampaign;
import com.shivhub.backend.service.CampaignService;
import com.shivhub.backend.service.FileStorageService;

/** Dedicated API for offer/festival campaigns; all routes are ADMIN-only. */
@RestController
@RequestMapping("/api/admin/campaigns")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCampaignController {
    private final CampaignService campaigns;
    private final FileStorageService fileStorage;
    public AdminCampaignController(CampaignService campaigns, FileStorageService fileStorage) {
        this.campaigns = campaigns;
        this.fileStorage = fileStorage;
    }

    @GetMapping public List<MarketingCampaign> list() { return campaigns.list(); }
    @GetMapping("/{id}") public MarketingCampaign get(@PathVariable Long id) { return campaigns.get(id); }
    @PostMapping public ResponseEntity<MarketingCampaign> create(@Valid @RequestBody AdminCampaignRequest request) { return ResponseEntity.ok(campaigns.create(request)); }
    @PostMapping(path = "/banner", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> uploadBanner(@RequestPart("file") MultipartFile file) {
        return Map.of("bannerUrl", fileStorage.storeCampaignBanner(file));
    }
    @PutMapping("/{id}") public MarketingCampaign update(@PathVariable Long id, @Valid @RequestBody AdminCampaignRequest request) { return campaigns.update(id, request); }
    @PostMapping("/audience-preview") public Map<String, Object> preview(@RequestBody AdminCampaignRequest request) { return campaigns.preview(request); }
    @PostMapping("/{id}/send") public Map<String, Object> send(@PathVariable Long id) { return campaigns.queue(id); }
    @PostMapping("/{id}/retry-failed") public Map<String, Object> retryFailed(@PathVariable Long id) { return campaigns.retryFailed(id); }
    @PostMapping("/{id}/schedule") public MarketingCampaign schedule(@PathVariable Long id, @Valid @RequestBody CampaignScheduleRequest request) { return campaigns.schedule(id, request); }
    @PostMapping("/{id}/test") public Map<String, Object> test(@PathVariable Long id, @Valid @RequestBody CampaignTestRequest request) { return campaigns.test(id, request); }
    @GetMapping("/{id}/report") public Map<String, Object> report(@PathVariable Long id,
            @RequestParam(required = false) String status, @RequestParam(required = false) String query) { return campaigns.report(id, status, query); }
}
