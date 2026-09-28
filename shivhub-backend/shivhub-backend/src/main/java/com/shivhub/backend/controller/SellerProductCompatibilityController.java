package com.shivhub.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shivhub.backend.dto.ProductCardResponse;
import com.shivhub.backend.dto.ProductCompatibilityRequest;
import com.shivhub.backend.service.ProductCompatibilityService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/seller/products/{productId}/compatible-accessories")
public class SellerProductCompatibilityController {

    private final ProductCompatibilityService compatibilityService;

    public SellerProductCompatibilityController(ProductCompatibilityService compatibilityService) {
        this.compatibilityService = compatibilityService;
    }

    @GetMapping
    public ResponseEntity<List<ProductCardResponse>> getAccessories(
            @PathVariable Long productId,
            Authentication authentication) {
        return ResponseEntity.ok(
                compatibilityService.getSellerAccessories(requireEmail(authentication), productId)
        );
    }

    @PostMapping
    public ResponseEntity<ProductCardResponse> saveAccessory(
            @PathVariable Long productId,
            @Valid @RequestBody ProductCompatibilityRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(
                compatibilityService.saveSellerAccessory(requireEmail(authentication), productId, request)
        );
    }

    @DeleteMapping("/{accessoryProductId}")
    public ResponseEntity<Void> deleteAccessory(
            @PathVariable Long productId,
            @PathVariable Long accessoryProductId,
            Authentication authentication) {
        compatibilityService.deleteSellerAccessory(requireEmail(authentication), productId, accessoryProductId);
        return ResponseEntity.noContent().build();
    }

    private String requireEmail(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Authentication required");
        }
        return authentication.getName();
    }
}
