package com.shivhub.backend.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.shivhub.backend.dto.*;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.repository.UserRepository;
import com.shivhub.backend.service.SellerEntitlementService;

@RestController
@RequestMapping("/api/admin/subscriptions")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSubscriptionController {
    private final SellerEntitlementService subscriptions;
    private final UserRepository users;
    public AdminSubscriptionController(SellerEntitlementService subscriptions, UserRepository users) { this.subscriptions = subscriptions; this.users = users; }
    private Long adminId(Authentication authentication) { return users.findByEmail(authentication.getName()).map(User::getId).orElse(null); }

    @GetMapping("/overview") public Map<String, Object> overview() { return subscriptions.adminOverview(); }
    @GetMapping("/plans") public List<SubscriptionPlanResponse> plans() { return subscriptions.allPlans(); }
    @GetMapping("/features") public List<SubscriptionFeatureResponse> features() { return subscriptions.allFeatures(); }
    @PostMapping("/plans") public ResponseEntity<SubscriptionPlanResponse> createPlan(@Valid @RequestBody SubscriptionPlanRequest request) { return ResponseEntity.ok(subscriptions.savePlan(null, request)); }
    @PutMapping("/plans/{id}") public SubscriptionPlanResponse updatePlan(@PathVariable Long id, @Valid @RequestBody SubscriptionPlanRequest request) { return subscriptions.savePlan(id, request); }
    @GetMapping("/sellers") public List<SellerSubscriptionResponse> sellers() { return subscriptions.adminSubscriptions(); }
    @GetMapping("/payments") public List<SubscriptionPaymentResponse> payments() { return subscriptions.adminPayments(); }
    @PatchMapping("/sellers/{sellerId}/extend") public SellerSubscriptionResponse extend(@PathVariable Long sellerId, @Valid @RequestBody SubscriptionAdminActionRequest request, Authentication authentication) { return subscriptions.adminExtend(sellerId, request, adminId(authentication)); }
    @PatchMapping("/sellers/{sellerId}/suspend") public SellerSubscriptionResponse suspend(@PathVariable Long sellerId, @Valid @RequestBody SubscriptionAdminActionRequest request, Authentication authentication) { return subscriptions.adminSetSuspended(sellerId, true, request, adminId(authentication)); }
    @PatchMapping("/sellers/{sellerId}/reactivate") public SellerSubscriptionResponse reactivate(@PathVariable Long sellerId, @Valid @RequestBody SubscriptionAdminActionRequest request, Authentication authentication) { return subscriptions.adminSetSuspended(sellerId, false, request, adminId(authentication)); }
}
