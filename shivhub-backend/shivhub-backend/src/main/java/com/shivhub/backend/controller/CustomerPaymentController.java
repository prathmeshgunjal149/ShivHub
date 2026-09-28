package com.shivhub.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.shivhub.backend.dto.PaymentSummaryResponse;
import com.shivhub.backend.dto.RazorpayOrderResponse;
import com.shivhub.backend.dto.RazorpayVerificationRequest;
import com.shivhub.backend.dto.RazorpayVerificationResponse;
import com.shivhub.backend.service.RazorpayPaymentService;

@RestController
@RequestMapping("/api/customer/payments")
public class CustomerPaymentController {
    private final RazorpayPaymentService payments;
    public CustomerPaymentController(RazorpayPaymentService payments) { this.payments = payments; }
    private String email(Authentication authentication) { if (authentication == null || authentication.getName() == null) throw new SecurityException("Authentication required"); return authentication.getName(); }
    @PostMapping("/online-order/create")
    public RazorpayOrderResponse create(@RequestBody CreateGatewayOrderRequest request, Authentication authentication) { return payments.createOnlineOrder(request.orderId(), email(authentication)); }
    @PostMapping("/verify")
    public RazorpayVerificationResponse verify(@Valid @RequestBody RazorpayVerificationRequest request, Authentication authentication) { return payments.verifyOnline(request, email(authentication)); }
    @GetMapping("/order/{orderId}")
    public PaymentSummaryResponse status(@PathVariable Long orderId, Authentication authentication) { return payments.onlineStatus(orderId, email(authentication)); }
    public record CreateGatewayOrderRequest(Long orderId) { }
}
