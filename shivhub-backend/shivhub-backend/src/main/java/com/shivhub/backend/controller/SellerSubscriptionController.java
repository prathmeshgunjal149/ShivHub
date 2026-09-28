package com.shivhub.backend.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.shivhub.backend.dto.*;
import com.shivhub.backend.service.SellerEntitlementService;

@RestController
@RequestMapping("/api/seller/subscription")
@PreAuthorize("hasRole('SELLER')")
public class SellerSubscriptionController {
    private final SellerEntitlementService subscriptions;
    public SellerSubscriptionController(SellerEntitlementService subscriptions) { this.subscriptions = subscriptions; }
    private String email(Authentication authentication) { if (authentication == null || authentication.getName() == null) throw new SecurityException("Authentication required"); return authentication.getName(); }

    @GetMapping public SellerSubscriptionResponse current(Authentication authentication) { return subscriptions.sellerSubscription(email(authentication)); }
    @GetMapping("/entitlements") public SubscriptionEntitlementsResponse entitlements(Authentication authentication) { return subscriptions.entitlementResponse(email(authentication)); }
    @GetMapping("/plans") public List<SubscriptionPlanResponse> plans() { return subscriptions.availablePlans(); }
    @GetMapping("/payments") public List<SubscriptionPaymentResponse> payments(Authentication authentication) { return subscriptions.sellerPayments(email(authentication)); }
    @PostMapping("/checkout/{planId}") public SubscriptionCheckoutResponse checkout(@PathVariable Long planId, Authentication authentication) { return subscriptions.createCheckout(email(authentication), planId); }
    @PostMapping("/verify") public SellerSubscriptionResponse verify(@Valid @RequestBody SubscriptionPaymentVerificationRequest request, Authentication authentication) { return subscriptions.verifyCheckout(email(authentication), request); }
}
