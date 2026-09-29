package com.shivhub.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import com.shivhub.backend.dto.LoginRequest;
import com.shivhub.backend.dto.LoginResponse;
import com.shivhub.backend.dto.ForgotPasswordRequest;
import com.shivhub.backend.dto.RegisterRequest;
import com.shivhub.backend.dto.ResetPasswordRequest;
import com.shivhub.backend.dto.ChangePasswordRequest;
import com.shivhub.backend.dto.SellerRegistrationOtpRequest;
import com.shivhub.backend.dto.SocialLoginRequest;
import com.shivhub.backend.dto.UserResponse;
import com.shivhub.backend.dto.VerifyLoginOtpRequest;
import com.shivhub.backend.dto.VerifySellerRegistrationOtpRequest;
import com.shivhub.backend.dto.WhatsAppOtpRequest;
import com.shivhub.backend.dto.VerifyWhatsAppOtpRequest;
import com.shivhub.backend.dto.PublicSocialLoginConfigurationResponse;
import com.shivhub.backend.service.AuthService;
import com.shivhub.backend.service.SocialLoginConfigurationService;

import jakarta.validation.Valid;


/*
 * =========================================================
 * AuthController
 * =========================================================
 *
 * Handles authentication-related APIs.
 *
 * APIs:
 *
 * 1. Customer Registration
 * 2. Seller Registration
 * 3. Login
 *
 *
 * Customer:
 *
 * POST /api/auth/register
 *
 *
 * Seller:
 *
 * POST /api/auth/register/seller
 *
 *
 * Login:
 *
 * POST /api/auth/login
 *
 * =========================================================
 */

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {


    /*
     * =========================================================
     * AuthService
     * =========================================================
     *
     * Handles the actual business logic.
     */

    private final AuthService authService;
    private final SocialLoginConfigurationService socialLoginConfiguration;


    /*
     * =========================================================
     * CONSTRUCTOR INJECTION
     * =========================================================
     */

    public AuthController(AuthService authService, SocialLoginConfigurationService socialLoginConfiguration) {

        this.authService = authService;
        this.socialLoginConfiguration = socialLoginConfiguration;
    }


    /*
     * =========================================================
     * CUSTOMER REGISTRATION
     * =========================================================
     *
     * URL:
     *
     * POST http://localhost:8080/api/auth/register
     *
     *
     * Request:
     *
     * {
     *     "name": "Test Customer",
     *     "email": "customer@shivhub.com",
     *     "password": "123456",
     *     "mobile": "9876543210"
     * }
     *
     *
     * Backend automatically sets:
     *
     * role    = CUSTOMER
     * status  = APPROVED
     * enabled = true
     *
     * =========================================================
     */

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request) {


        /*
         * Send registration data
         * to AuthService.
         */

        UserResponse userResponse =
                authService.registerCustomer(request);


        /*
         * HTTP 201 CREATED
         */

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userResponse);
    }


    /*
     * =========================================================
     * SELLER REGISTRATION
     * =========================================================
     *
     * URL:
     *
     * POST http://localhost:8080/api/auth/register/seller
     *
     *
     * Request:
     *
     * {
     *     "name": "Shiv Mobile Seller",
     *     "email": "seller@shivhub.com",
     *     "password": "123456",
     *     "mobile": "9876543212"
     * }
     *
     *
     * Backend automatically sets:
     *
     * role    = SELLER
     * status  = PENDING
     * enabled = false
     *
     *
     * Seller must wait for Admin approval.
     *
     * =========================================================
     */

    @PostMapping("/register/seller")
    public ResponseEntity<UserResponse> registerSeller(
            @Valid @RequestBody RegisterRequest request) {


        /*
         * Send seller registration data
         * to AuthService.
         */

        UserResponse userResponse =
                authService.registerSeller(request);


        /*
         * HTTP 201 CREATED
         */

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userResponse);
    }

    @PostMapping("/register/send-whatsapp-otp")
    public ResponseEntity<java.util.Map<String, String>> sendWhatsAppRegistrationOtp(@Valid @RequestBody WhatsAppOtpRequest request) {
        authService.sendCustomerWhatsAppOtp(request.email());
        return ResponseEntity.accepted().body(java.util.Map.of("message", "WhatsApp verification OTP sent"));
    }

    @PostMapping("/register/resend-whatsapp-otp")
    public ResponseEntity<java.util.Map<String, String>> resendWhatsAppRegistrationOtp(@Valid @RequestBody WhatsAppOtpRequest request) {
        authService.sendCustomerWhatsAppOtp(request.email());
        return ResponseEntity.accepted().body(java.util.Map.of("message", "WhatsApp verification OTP resent"));
    }

    @PostMapping("/register/verify-whatsapp-otp")
    public ResponseEntity<java.util.Map<String, String>> verifyWhatsAppRegistrationOtp(@Valid @RequestBody VerifyWhatsAppOtpRequest request) {
        authService.verifyCustomerWhatsAppOtp(request.email(), request.otp());
        return ResponseEntity.ok(java.util.Map.of("message", "WhatsApp mobile number verified. You can now log in."));
    }

    @PostMapping("/register/seller/request-otp")
    public ResponseEntity<java.util.Map<String, String>> requestSellerRegistrationOtp(
            @Valid @RequestBody SellerRegistrationOtpRequest request) {

        authService.requestSellerRegistrationOtp(request);

        return ResponseEntity.accepted().body(
                java.util.Map.of(
                        "message",
                        "Verification OTP sent to seller email"
                )
        );
    }

    @PostMapping("/register/seller/verify-otp")
    public ResponseEntity<UserResponse> verifySellerRegistrationOtp(
            @Valid @RequestBody VerifySellerRegistrationOtpRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authService.verifySellerRegistrationOtp(request));
    }


    /*
     * =========================================================
     * LOGIN
     * =========================================================
     *
     * URL:
     *
     * POST http://localhost:8080/api/auth/login
     *
     *
     * Request:
     *
     * {
     *     "email": "customer@shivhub.com",
     *     "password": "123456"
     * }
     *
     *
     * Login flow:
     *
     * Email
     *   ↓
     * Find User
     *   ↓
     * Check Status
     *   ↓
     * Check Enabled
     *   ↓
     * Verify Password
     *   ↓
     * Generate JWT
     *   ↓
     * LoginResponse
     *
     * =========================================================
     */

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {


        /*
         * Send login request to AuthService.
         */

        LoginResponse response =
                authService.login(request);


        /*
         * HTTP 200 OK
         */

        return ResponseEntity.ok(response);
    }

    // Changed: verifies the emailed OTP and returns the JWT for customer/seller sessions.
    @PostMapping("/verify-login-otp")
    public ResponseEntity<LoginResponse> verifyLoginOtp(
            @Valid @RequestBody VerifyLoginOtpRequest request) {
        return ResponseEntity.ok(authService.verifyLoginOtp(request));
    }

    @PostMapping("/social-login")
    public ResponseEntity<LoginResponse> socialLogin(
            @Valid @RequestBody SocialLoginRequest request) {
        return ResponseEntity.ok(authService.socialLogin(request));
    }

    /** OAuth client/app IDs are public values required by the browser to start sign-in. */
    @org.springframework.web.bind.annotation.GetMapping("/social-login/config")
    public ResponseEntity<PublicSocialLoginConfigurationResponse> socialLoginConfiguration() {
        return ResponseEntity.ok(socialLoginConfiguration.publicConfiguration());
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<java.util.Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        authService.requestPasswordReset(request.getEmail());

        return ResponseEntity.accepted().body(java.util.Map.of(
                "message",
                "If the email belongs to a customer or seller account, a reset link has been sent."
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<java.util.Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        authService.resetPassword(request);

        return ResponseEntity.ok(java.util.Map.of(
                "message",
                "Password reset successfully. Please login with your new password."
        ));
    }

    @PostMapping("/change-password")
    public ResponseEntity<java.util.Map<String, String>> changePassword(Authentication authentication, @Valid @RequestBody ChangePasswordRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(java.util.Map.of("message", "Login is required"));
        authService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok(java.util.Map.of("message", "Password changed successfully."));
    }
}
