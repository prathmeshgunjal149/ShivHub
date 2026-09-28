package com.shivhub.backend.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.shivhub.backend.dto.ShopRequest;
import com.shivhub.backend.dto.ShopResponse;
import com.shivhub.backend.dto.ShopStaffAssignmentRequest;
import com.shivhub.backend.dto.CreateShopStaffRequest;
import com.shivhub.backend.service.ShopService;
import jakarta.validation.Valid;

/** Seller-facing branch APIs. Every mutation verifies branch ownership in ShopService. */
@RestController
@RequestMapping("/api/seller/shops")
public class ShopController {
    private final ShopService shopService;
    public ShopController(ShopService shopService) { this.shopService = shopService; }

    @GetMapping
    public ResponseEntity<List<ShopResponse>> getMyShops(Authentication auth) {
        return ResponseEntity.ok(shopService.getMyShops(email(auth)));
    }
    @PostMapping
    public ResponseEntity<ShopResponse> create(Authentication auth, @Valid @RequestBody ShopRequest request) {
        return ResponseEntity.status(201).body(shopService.createShop(email(auth), request));
    }
    @PutMapping("/{shopId}")
    public ResponseEntity<ShopResponse> update(Authentication auth, @PathVariable Long shopId, @Valid @RequestBody ShopRequest request) {
        return ResponseEntity.ok(shopService.updateShop(email(auth), shopId, request));
    }
    @PatchMapping("/{shopId}/active")
    public ResponseEntity<Void> setActive(Authentication auth, @PathVariable Long shopId, @RequestParam boolean value) {
        shopService.setShopActive(email(auth), shopId, value); return ResponseEntity.noContent().build();
    }
    @PutMapping("/{shopId}/staff")
    public ResponseEntity<Void> assignStaff(Authentication auth, @PathVariable Long shopId,
            @Valid @RequestBody ShopStaffAssignmentRequest request) {
        shopService.assignStaff(email(auth), shopId, request); return ResponseEntity.noContent().build();
    }
    @PostMapping("/{shopId}/staff")
    public ResponseEntity<Void> createStaff(Authentication auth, @PathVariable Long shopId,
            @Valid @RequestBody CreateShopStaffRequest request) {
        shopService.createAndAssignStaff(email(auth), shopId, request);
        return ResponseEntity.status(201).build();
    }
    private String email(Authentication auth) {
        if (auth == null || auth.getName() == null) throw new RuntimeException("Authentication required");
        return auth.getName();
    }
}
