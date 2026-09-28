package com.shivhub.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shivhub.backend.dto.EnquiryResponse;
import com.shivhub.backend.dto.FaqRequest;
import com.shivhub.backend.dto.FaqResponse;
import com.shivhub.backend.dto.PolicyPageResponse;
import com.shivhub.backend.dto.PolicyUpdateRequest;
import com.shivhub.backend.dto.PolicyVersionResponse;
import com.shivhub.backend.dto.SiteSettingResponse;
import com.shivhub.backend.dto.UpdateEnquiryStatusRequest;
import com.shivhub.backend.dto.UpdateSiteSettingRequest;
import com.shivhub.backend.service.WebsiteContentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/website")
public class AdminWebsiteController {

    private final WebsiteContentService websiteContentService;

    public AdminWebsiteController(WebsiteContentService websiteContentService) {
        this.websiteContentService = websiteContentService;
    }

    @GetMapping("/settings")
    public ResponseEntity<SiteSettingResponse> settings() {
        return ResponseEntity.ok(websiteContentService.publicSettings());
    }

    @PutMapping("/settings")
    public ResponseEntity<SiteSettingResponse> updateSettings(@Valid @RequestBody UpdateSiteSettingRequest request) {
        return ResponseEntity.ok(websiteContentService.updateSettings(request));
    }

    @GetMapping("/policies")
    public ResponseEntity<List<PolicyPageResponse>> policies() {
        return ResponseEntity.ok(websiteContentService.listPolicies(true));
    }

    @GetMapping("/policies/{slug}")
    public ResponseEntity<PolicyPageResponse> policy(@PathVariable String slug) {
        return ResponseEntity.ok(websiteContentService.getAdminPolicy(slug));
    }

    @PutMapping("/policies/{slug}/draft")
    public ResponseEntity<PolicyPageResponse> saveDraft(
            @PathVariable String slug,
            @Valid @RequestBody PolicyUpdateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(websiteContentService.saveDraft(slug, request, email(authentication)));
    }

    @PutMapping("/policies/{slug}/publish")
    public ResponseEntity<PolicyPageResponse> publish(
            @PathVariable String slug,
            @Valid @RequestBody PolicyUpdateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(websiteContentService.publish(slug, request, email(authentication)));
    }

    @GetMapping("/policies/{slug}/versions")
    public ResponseEntity<List<PolicyVersionResponse>> versions(@PathVariable String slug) {
        return ResponseEntity.ok(websiteContentService.versions(slug));
    }

    @PostMapping("/policies/{slug}/versions/{versionId}/restore")
    public ResponseEntity<PolicyPageResponse> restore(
            @PathVariable String slug,
            @PathVariable Long versionId,
            Authentication authentication) {
        return ResponseEntity.ok(websiteContentService.restoreVersion(slug, versionId, email(authentication)));
    }

    @GetMapping("/faqs")
    public ResponseEntity<List<FaqResponse>> faqs() {
        return ResponseEntity.ok(websiteContentService.adminFaqs());
    }

    @PostMapping("/faqs")
    public ResponseEntity<FaqResponse> addFaq(@Valid @RequestBody FaqRequest request) {
        return ResponseEntity.ok(websiteContentService.saveFaq(request));
    }

    @DeleteMapping("/faqs/{id}")
    public ResponseEntity<Void> deleteFaq(@PathVariable Long id) {
        websiteContentService.deleteFaq(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/enquiries")
    public ResponseEntity<List<EnquiryResponse>> enquiries(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(websiteContentService.enquiries(status));
    }

    @PutMapping("/enquiries/{id}")
    public ResponseEntity<EnquiryResponse> updateEnquiry(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEnquiryStatusRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(websiteContentService.updateEnquiry(id, request, email(authentication)));
    }

    private String email(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Authentication required");
        }
        return authentication.getName();
    }
}
