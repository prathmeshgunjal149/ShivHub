package com.shivhub.backend.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.shivhub.backend.dto.ManualPaymentRequest;
import com.shivhub.backend.dto.PaymentSummaryResponse;
import com.shivhub.backend.dto.PaymentTransactionResponse;
import com.shivhub.backend.dto.RazorpayOrderResponse;
import com.shivhub.backend.dto.RazorpayPaymentLinkResponse;
import com.shivhub.backend.dto.RazorpayVerificationRequest;
import com.shivhub.backend.service.RazorpayPaymentService;

@RestController
@RequestMapping("/api/seller/offline-bills/{billId}/payments")
@PreAuthorize("hasRole('SELLER')")
public class SellerOfflinePaymentController {
    private final RazorpayPaymentService payments;
    public SellerOfflinePaymentController(RazorpayPaymentService payments) { this.payments = payments; }
    private String email(Authentication authentication) { if (authentication == null || authentication.getName() == null) throw new SecurityException("Authentication required"); return authentication.getName(); }
    @PostMapping("/manual")
    public PaymentSummaryResponse manual(@PathVariable Long billId, @Valid @RequestBody ManualPaymentRequest request, Authentication authentication) { return payments.recordManualOfflinePayment(billId, request, email(authentication)); }
    @PostMapping("/razorpay/create")
    public RazorpayOrderResponse createRazorpay(@PathVariable Long billId, Authentication authentication) { return payments.createOfflineOrder(billId, email(authentication)); }
    @PostMapping("/razorpay/link")
    public RazorpayPaymentLinkResponse createRazorpayLink(@PathVariable Long billId, Authentication authentication) { return payments.createOfflinePaymentLink(billId, email(authentication)); }
    @PostMapping("/razorpay/verify")
    public PaymentSummaryResponse verifyRazorpay(@PathVariable Long billId, @Valid @RequestBody RazorpayVerificationRequest request, Authentication authentication) { return payments.verifyOffline(billId, request, email(authentication)); }
    @GetMapping
    public List<PaymentTransactionResponse> history(@PathVariable Long billId, Authentication authentication) { return payments.offlineHistory(billId, email(authentication)).getPayments(); }
    @GetMapping("/summary")
    public PaymentSummaryResponse summary(@PathVariable Long billId, Authentication authentication) { return payments.offlineHistory(billId, email(authentication)); }
}
