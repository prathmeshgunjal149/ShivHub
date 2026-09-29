package com.shivhub.backend.controller;

import com.shivhub.backend.dto.AdminSocialLoginConfigurationResponse;
import com.shivhub.backend.dto.SocialLoginProviderRequest;
import com.shivhub.backend.service.SocialLoginConfigurationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin-only editing for the public OAuth client IDs used by the login screen. */
@RestController
@RequestMapping("/api/admin/social-login")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSocialLoginConfigurationController {
    private final SocialLoginConfigurationService socialLogin;

    public AdminSocialLoginConfigurationController(SocialLoginConfigurationService socialLogin) {
        this.socialLogin = socialLogin;
    }

    @GetMapping
    public AdminSocialLoginConfigurationResponse status() {
        return socialLogin.adminConfiguration();
    }

    @PutMapping("/google")
    public AdminSocialLoginConfigurationResponse saveGoogle(@Valid @RequestBody SocialLoginProviderRequest request) {
        return socialLogin.saveGoogle(request);
    }

    @PutMapping("/facebook")
    public AdminSocialLoginConfigurationResponse saveFacebook(@Valid @RequestBody SocialLoginProviderRequest request) {
        return socialLogin.saveFacebook(request);
    }
}
