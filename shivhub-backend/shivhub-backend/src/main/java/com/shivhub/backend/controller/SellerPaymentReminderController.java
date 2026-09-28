package com.shivhub.backend.controller;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shivhub.backend.entity.User;
import com.shivhub.backend.repository.UserRepository;
import com.shivhub.backend.service.PaymentReminderService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/seller/payment-reminders")
@PreAuthorize("hasRole('SELLER')")
@RequiredArgsConstructor
public class SellerPaymentReminderController {
    private final PaymentReminderService reminders;
    private final UserRepository users;

    /** Safe manual test trigger: a seller can only request their own consolidated reminder. */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> send(Authentication authentication) {
        User seller = users.findByEmail(authentication.getName()).orElseThrow(() -> new IllegalArgumentException("Seller not found"));
        boolean sent = reminders.sendForSeller(seller, LocalDate.now(ZoneId.of("Asia/Kolkata")), false);
        return ResponseEntity.ok(Map.of("sent", sent, "message", sent ? "Daily payment reminder sent." : "A reminder was already sent today, or there are no due payments."));
    }
}
