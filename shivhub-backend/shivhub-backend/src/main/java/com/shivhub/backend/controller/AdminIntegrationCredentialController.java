package com.shivhub.backend.controller;

import com.shivhub.backend.dto.AdminIntegrationCredentialsResponse;
import com.shivhub.backend.dto.RazorpayCredentialRequest;
import com.shivhub.backend.dto.SmtpCredentialRequest;
import com.shivhub.backend.service.AdminIntegrationCredentialService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin-only operations. Secret values are write-only and never serialized. */
@RestController
@RequestMapping("/api/admin/integration-credentials")
@PreAuthorize("hasRole('ADMIN')")
public class AdminIntegrationCredentialController {
    private final AdminIntegrationCredentialService credentials;
    public AdminIntegrationCredentialController(AdminIntegrationCredentialService credentials) { this.credentials = credentials; }
    @GetMapping public AdminIntegrationCredentialsResponse status() { return credentials.status(); }
    @PutMapping("/smtp") public AdminIntegrationCredentialsResponse saveSmtp(@Valid @RequestBody SmtpCredentialRequest request) { return credentials.saveSmtp(request); }
    @PutMapping("/razorpay") public AdminIntegrationCredentialsResponse saveRazorpay(@Valid @RequestBody RazorpayCredentialRequest request) { return credentials.saveRazorpay(request); }
}
