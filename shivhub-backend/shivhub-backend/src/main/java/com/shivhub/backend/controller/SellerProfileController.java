package com.shivhub.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.shivhub.backend.dto.SellerProfileResponse;
import com.shivhub.backend.dto.UpdateSellerProfileRequest;
import com.shivhub.backend.service.SellerProfileService;
import jakarta.validation.Valid;

/** Seller account and business settings API. */
@RestController
@RequestMapping("/api/seller/profile")
public class SellerProfileController {
    private final SellerProfileService service;
    public SellerProfileController(SellerProfileService service) { this.service = service; }
    @GetMapping public ResponseEntity<SellerProfileResponse> get(Authentication auth) { return ResponseEntity.ok(service.get(email(auth))); }
    @PutMapping public ResponseEntity<SellerProfileResponse> update(Authentication auth, @Valid @RequestBody UpdateSellerProfileRequest request) { return ResponseEntity.ok(service.update(email(auth), request)); }
    private String email(Authentication auth) { if (auth == null || auth.getName() == null) throw new RuntimeException("Authentication required"); return auth.getName(); }
}
