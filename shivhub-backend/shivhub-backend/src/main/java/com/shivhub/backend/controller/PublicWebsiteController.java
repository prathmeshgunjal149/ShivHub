package com.shivhub.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shivhub.backend.dto.EnquiryRequest;
import com.shivhub.backend.dto.EnquiryResponse;
import com.shivhub.backend.dto.FaqResponse;
import com.shivhub.backend.dto.PolicyPageResponse;
import com.shivhub.backend.dto.SiteSettingResponse;
import com.shivhub.backend.service.WebsiteContentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/public")
public class PublicWebsiteController {

    private final WebsiteContentService websiteContentService;

    public PublicWebsiteController(WebsiteContentService websiteContentService) {
        this.websiteContentService = websiteContentService;
    }

    @GetMapping("/site-settings")
    public ResponseEntity<SiteSettingResponse> settings() {
        return ResponseEntity.ok(websiteContentService.publicSettings());
    }

    @GetMapping("/policies/{slug}")
    public ResponseEntity<PolicyPageResponse> policy(@PathVariable String slug) {
        return ResponseEntity.ok(websiteContentService.getPublicPolicy(slug));
    }

    @GetMapping("/faqs")
    public ResponseEntity<List<FaqResponse>> faqs() {
        return ResponseEntity.ok(websiteContentService.publicFaqs());
    }

    @PostMapping("/enquiries")
    public ResponseEntity<EnquiryResponse> enquiry(@Valid @RequestBody EnquiryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(websiteContentService.submitEnquiry(request));
    }
}
