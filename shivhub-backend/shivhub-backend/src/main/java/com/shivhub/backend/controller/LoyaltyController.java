package com.shivhub.backend.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.shivhub.backend.dto.LoyaltyAdjustmentRequest;
import com.shivhub.backend.dto.LoyaltySettingsRequest;
import com.shivhub.backend.dto.CustomerLoyaltySummaryResponse;
import com.shivhub.backend.dto.CustomerLoyaltyTransactionResponse;
import org.springframework.data.domain.Page;
import com.shivhub.backend.entity.LoyaltySettings;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.repository.UserRepository;
import com.shivhub.backend.service.LoyaltyService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class LoyaltyController {
    private final LoyaltyService loyalty; private final UserRepository users;
    public LoyaltyController(LoyaltyService loyalty, UserRepository users) { this.loyalty = loyalty; this.users = users; }
    @GetMapping("/customer/loyalty") @PreAuthorize("hasRole('CUSTOMER')")
    public Map<String, Object> mine(Authentication auth) { return loyalty.customerLoyalty(user(auth).getId()); }
    @GetMapping("/customer/loyalty/summary") @PreAuthorize("hasRole('CUSTOMER')")
    public CustomerLoyaltySummaryResponse summary(Authentication auth) { return loyalty.customerSummary(user(auth).getId()); }
    @GetMapping("/customer/loyalty/transactions") @PreAuthorize("hasRole('CUSTOMER')")
    public Page<CustomerLoyaltyTransactionResponse> transactions(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, Authentication auth) { return loyalty.customerTransactions(user(auth).getId(), page, size); }
    @GetMapping("/seller/loyalty/lookup") @PreAuthorize("hasRole('SELLER')")
    public Map<String, Object> lookup(@RequestParam String mobile, Authentication auth) { return loyalty.loyaltyForMobile(mobile, user(auth).getId()); }
    @GetMapping("/admin/loyalty/settings") @PreAuthorize("hasRole('ADMIN')")
    public List<LoyaltySettings> settings() { return loyalty.settings(); }
    @PutMapping("/admin/loyalty/settings") @PreAuthorize("hasRole('ADMIN')")
    public LoyaltySettings settings(@Valid @RequestBody LoyaltySettingsRequest request) { return loyalty.saveSettings(request); }
    @PostMapping("/admin/loyalty/adjustments") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> adjust(@Valid @RequestBody LoyaltyAdjustmentRequest request, Authentication auth) { return ResponseEntity.ok(loyalty.adjust(request, user(auth).getId())); }
    private User user(Authentication auth) { if (auth == null) throw new IllegalStateException("Authentication required"); return users.findByEmail(auth.getName()).orElseThrow(() -> new IllegalStateException("User not found")); }
}
