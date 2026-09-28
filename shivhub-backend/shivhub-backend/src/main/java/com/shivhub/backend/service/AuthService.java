package com.shivhub.backend.service;

import java.security.SecureRandom;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.mail.MailException;

import com.shivhub.backend.dto.LoginRequest;
import com.shivhub.backend.dto.LoginResponse;
import com.shivhub.backend.dto.RegisterRequest;
import com.shivhub.backend.dto.ResetPasswordRequest;
import com.shivhub.backend.dto.ChangePasswordRequest;
import com.shivhub.backend.dto.SellerRegistrationOtpRequest;
import com.shivhub.backend.dto.SocialLoginRequest;
import com.shivhub.backend.dto.UserResponse;
import com.shivhub.backend.dto.VerifyLoginOtpRequest;
import com.shivhub.backend.dto.VerifySellerRegistrationOtpRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.shivhub.backend.entity.SellerRegistrationOtp;
import com.shivhub.backend.entity.CustomerProfile;
import com.shivhub.backend.entity.User;

import com.shivhub.backend.enums.Role;
import com.shivhub.backend.enums.UserStatus;

import com.shivhub.backend.repository.SellerRegistrationOtpRepository;
import com.shivhub.backend.repository.UserRepository;
import com.shivhub.backend.repository.CustomerProfileRepository;

import com.shivhub.backend.security.JwtService;


/*
 * =========================================================
 * AuthService
 * =========================================================
 *
 * Handles:
 *
 * 1. Customer Registration
 * 2. Seller Registration
 * 3. User Login
 * 4. Password Verification
 * 5. JWT Generation
 *
 *
 * Seller workflow:
 *
 * Seller Registration
 *        ↓
 * SELLER
 *        ↓
 * PENDING
 *        ↓
 * enabled = false
 *        ↓
 * Admin Approval
 *        ↓
 * APPROVED
 *        ↓
 * enabled = true
 *
 * =========================================================
 */

@Service
public class AuthService {


    /*
     * =========================================================
     * DEPENDENCIES
     * =========================================================
     */

    private final UserRepository userRepository;

    private final SellerRegistrationOtpRepository sellerRegistrationOtpRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    private final EmailService emailService;
    private final ReferralService referralService;
    private final ObjectMapper objectMapper;
    private final CustomerProfileRepository customerProfiles;
    private final WhatsAppOtpService whatsAppOtpService;

    @Value("${shivhub.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${shivhub.oauth.google.client-id:}")
    private String googleClientId;

    @Value("${shivhub.oauth.facebook.app-id:}")
    private String facebookAppId;

    // Changed: cryptographically secure OTP generation for email verification.
    private final SecureRandom secureRandom = new SecureRandom();


    /*
     * =========================================================
     * CONSTRUCTOR INJECTION
     * =========================================================
     */

    public AuthService(
            UserRepository userRepository,
            SellerRegistrationOtpRepository sellerRegistrationOtpRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            EmailService emailService,
            ReferralService referralService,
            ObjectMapper objectMapper,
            CustomerProfileRepository customerProfiles,
            WhatsAppOtpService whatsAppOtpService) {

        this.userRepository = userRepository;

        this.sellerRegistrationOtpRepository = sellerRegistrationOtpRepository;

        this.passwordEncoder = passwordEncoder;

        this.jwtService = jwtService;
        this.emailService = emailService;
        this.referralService = referralService;
        this.objectMapper = objectMapper;
        this.customerProfiles = customerProfiles;
        this.whatsAppOtpService = whatsAppOtpService;
    }


    /*
     * =========================================================
     * CUSTOMER REGISTRATION
     * =========================================================
     *
     * POST /api/auth/register
     *
     * Customer does not require Admin approval.
     *
     * role    = CUSTOMER
     * status  = APPROVED
     * enabled = true
     *
     * =========================================================
     */

    public UserResponse registerCustomer(
            RegisterRequest request) {

        String email = normalizeEmail(request.getEmail());
        String mobile = normalizeMobile(request.getMobile());

        /*
         * Check duplicate email.
         */

        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {

            throw new RuntimeException(
                    "Email already registered"
            );
        }


        /*
         * Check duplicate mobile.
         */

        if (userRepository.existsByMobile(mobile)) {

            throw new RuntimeException(
                    "Mobile number already registered"
            );
        }


        /*
         * Create new User.
         */

        User user = new User();


        /*
         * Set customer information.
         */

        user.setName(
                request.getName()
        );

        user.setEmail(
                email
        );

        user.setMobile(
                mobile
        );

        validateNewCustomerDateOfBirth(request.getDateOfBirth());
        user.setDateOfBirth(request.getDateOfBirth());


        /*
         * Encrypt password using BCrypt.
         */

        user.setPassword(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );


        /*
         * Backend controls the role.
         *
         * Frontend cannot choose ADMIN or SELLER.
         */

        user.setRole(
                Role.CUSTOMER
        );


        /*
         * Customer is automatically approved.
         */

        user.setStatus(
                UserStatus.APPROVED
        );


        /*
         * Customer can login immediately.
         */

        user.setEnabled(true);
        // New customer accounts cannot receive a session until their mobile is verified.
        user.setWhatsappVerified(false);


        /*
         * Save customer.
         */

        User savedUser =
                userRepository.save(user);

        CustomerProfile profile = new CustomerProfile();
        profile.setOnlineUser(savedUser); profile.setName(savedUser.getName()); profile.setMobile(savedUser.getMobile());
        profile.setEmail(savedUser.getEmail()); profile.setDateOfBirth(savedUser.getDateOfBirth()); profile.setCommunicationConsent(true);
        customerProfiles.save(profile);

        referralService.registerReferral(savedUser, request.getReferralCode());


        /*
         * Return SAFE response.
         *
         * Password is never returned.
         */

        return new UserResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getMobile(),
                savedUser.getRole(),
                savedUser.getStatus()
        );
    }


    /*
     * =========================================================
     * SELLER REGISTRATION
     * =========================================================
     *
     * POST /api/auth/register/seller
     *
     *
     * Seller registration creates:
     *
     * role    = SELLER
     * status  = PENDING
     * enabled = false
     *
     *
     * Seller cannot login until Admin approves.
     *
     * =========================================================
     */

    public UserResponse registerSeller(
            RegisterRequest request) {

        String email = normalizeEmail(request.getEmail());
        String mobile = normalizeMobile(request.getMobile());

        /*
         * Check duplicate email.
         */

        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {

            throw new RuntimeException(
                    "Email already registered"
            );
        }


        /*
         * Check duplicate mobile.
         */

        if (userRepository.existsByMobile(mobile)) {

            throw new RuntimeException(
                    "Mobile number already registered"
            );
        }


        /*
         * Create new seller.
         */

        User user = new User();


        /*
         * Seller information.
         */

        user.setName(
                request.getName()
        );

        user.setEmail(
                email
        );

        user.setMobile(
                mobile
        );


        /*
         * Encrypt seller password.
         */

        user.setPassword(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );


        /*
         * Backend automatically assigns SELLER role.
         */

        user.setRole(
                Role.SELLER
        );


        /*
         * Seller needs Admin approval.
         */

        user.setStatus(
                UserStatus.PENDING
        );


        /*
         * Seller cannot login before approval.
         */

        user.setEnabled(false);


        /*
         * Save seller.
         */

        User savedUser =
                userRepository.save(user);


        /*
         * Return SAFE response.
         *
         * Password is NOT returned.
         */

        return new UserResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getMobile(),
                savedUser.getRole(),
                savedUser.getStatus()
        );
    }


    /*
     * =========================================================
     * SELLER REGISTRATION EMAIL OTP
     * =========================================================
     */

    public void requestSellerRegistrationOtp(
            SellerRegistrationOtpRequest request) {

        String email = normalizeEmail(request.getEmail());
        String mobile = normalizeMobile(request.getMobile());

        ensureUniqueEmailAndMobile(
                email,
                mobile
        );

        String otp =
                String.format(
                        "%06d",
                        secureRandom.nextInt(
                                1_000_000
                        )
                );

        SellerRegistrationOtp pending =
                new SellerRegistrationOtp();

        pending.setName(clean(request.getName()));
        pending.setEmail(email);
        pending.setMobile(mobile);
        pending.setPasswordHash(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );
        pending.setBusinessName(clean(request.getBusinessName()));
        pending.setGstin(clean(request.getGstin()).toUpperCase());
        pending.setBusinessAddress(clean(request.getBusinessAddress()));
        pending.setOtpHash(passwordEncoder.encode(otp));
        pending.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        pending.setVerified(false);

        sellerRegistrationOtpRepository.save(pending);

        try {
            emailService.sendSellerRegistrationOtpEmail(
                    pending.getEmail(),
                    pending.getName(),
                    pending.getBusinessName(),
                    otp
            );
        } catch (MailException exception) {
            throw new RuntimeException(
                    "OTP email could not be sent. Please check email configuration."
            );
        }
    }

    public UserResponse verifySellerRegistrationOtp(
            VerifySellerRegistrationOtpRequest request) {

        SellerRegistrationOtp pending =
                sellerRegistrationOtpRepository
                        .findTopByEmailIgnoreCaseOrderByCreatedAtDesc(
                                normalizeEmail(request.getEmail())
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Invalid or expired verification code"
                                )
                        );

        ensureUniqueEmailAndMobile(
                pending.getEmail(),
                pending.getMobile()
        );

        if (pending.isVerified()
                || LocalDateTime.now().isAfter(pending.getExpiresAt())
                || !passwordEncoder.matches(
                        request.getOtp(),
                        pending.getOtpHash()
                )) {

            throw new RuntimeException(
                    "Invalid or expired verification code"
            );
        }

        pending.setVerified(true);
        sellerRegistrationOtpRepository.save(pending);

        User seller = new User();
        seller.setName(pending.getName());
        seller.setEmail(pending.getEmail());
        seller.setMobile(pending.getMobile());
        seller.setPassword(pending.getPasswordHash());
        seller.setBusinessName(pending.getBusinessName());
        seller.setLegalBusinessName(pending.getBusinessName());
        seller.setGstin(pending.getGstin());
        seller.setBusinessAddress(pending.getBusinessAddress());
        seller.setRole(Role.SELLER);
        seller.setStatus(UserStatus.PENDING);
        seller.setEnabled(false);

        User savedUser =
                userRepository.save(seller);

        return new UserResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getMobile(),
                savedUser.getRole(),
                savedUser.getStatus()
        );
    }

    public void sendCustomerWhatsAppOtp(String email) { whatsAppOtpService.send(normalizeEmail(email)); }

    public void verifyCustomerWhatsAppOtp(String email, String otp) { whatsAppOtpService.verify(normalizeEmail(email), otp); }

    private void ensureUniqueEmailAndMobile(
            String email,
            String mobile) {

        String normalizedEmail = normalizeEmail(email);
        String normalizedMobile = normalizeMobile(mobile);

        if (userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {

            throw new RuntimeException(
                    "Email already registered"
            );
        }

        if (userRepository.existsByMobile(normalizedMobile)) {

            throw new RuntimeException(
                    "Mobile number already registered"
            );
        }
    }

    private String clean(String value) {

        return value == null ? "" : value.trim();
    }

    private String normalizeEmail(String value) {
        return clean(value).toLowerCase();
    }

    private String normalizeMobile(String value) {
        return clean(value);
    }

    public void requestPasswordReset(String email) {
        if (email == null || email.isBlank()) {
            return;
        }

        userRepository.findByEmailIgnoreCase(email.trim())
                .filter(user -> user.getRole() == Role.CUSTOMER || user.getRole() == Role.SELLER)
                .ifPresent(user -> {
                    // BCrypt accepts at most 72 bytes. A 32-byte URL-safe token is secure and only 43 ASCII characters.
                    byte[] tokenBytes = new byte[32];
                    secureRandom.nextBytes(tokenBytes);
                    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
                    user.setPasswordResetTokenHash(passwordEncoder.encode(token));
                    user.setPasswordResetExpiresAt(LocalDateTime.now().plusMinutes(30));
                    userRepository.save(user);

                    try {
                        String link = frontendUrl.replaceAll("/+$", "") + "/reset-password?token=" + token;
                        emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), link);
                    } catch (MailException ignored) {
                        // Keep response generic; do not reveal email existence or mail config.
                    }
                });
    }

    public void resetPassword(ResetPasswordRequest request) {
        if (request == null || request.getToken() == null || request.getToken().isBlank()) {
            throw new RuntimeException("Invalid or expired reset link");
        }

        validatePasswordBytes(request.getNewPassword());
        User user = userRepository.findAll().stream()
                .filter(candidate -> candidate.getRole() == Role.CUSTOMER || candidate.getRole() == Role.SELLER)
                .filter(candidate -> candidate.getPasswordResetTokenHash() != null)
                .filter(candidate -> candidate.getPasswordResetExpiresAt() != null
                        && LocalDateTime.now().isBefore(candidate.getPasswordResetExpiresAt()))
                .filter(candidate -> passwordEncoder.matches(request.getToken(), candidate.getPasswordResetTokenHash()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset link"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetTokenHash(null);
        user.setPasswordResetExpiresAt(null);
        user.setLoginOtpHash(null);
        user.setLoginOtpExpiresAt(null);
        userRepository.save(user);
    }

    /** Authenticated customer/seller password change; the caller identity comes only from JWT. */
    public void changePassword(String email, ChangePasswordRequest request) {
        if (request == null || request.getCurrentPassword() == null || request.getNewPassword() == null) throw new IllegalArgumentException("Current and new password are required");
        validatePasswordBytes(request.getNewPassword());
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(email)).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getRole() != Role.CUSTOMER && user.getRole() != Role.SELLER) throw new IllegalArgumentException("Password change is not available for this account");
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) throw new IllegalArgumentException("Current password is incorrect");
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) throw new IllegalArgumentException("New password must be different from current password");
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetTokenHash(null); user.setPasswordResetExpiresAt(null); user.setLoginOtpHash(null); user.setLoginOtpExpiresAt(null);
        userRepository.save(user);
    }

    private void validatePasswordBytes(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) throw new IllegalArgumentException("Password must be 72 bytes or shorter");
    }


    /*
     * =========================================================
     * LOGIN
     * =========================================================
     *
     * Steps:
     *
     * 1. Find user by email
     * 2. Check seller approval
     * 3. Check account enabled
     * 4. Verify password
     * 5. Generate JWT
     * 6. Return LoginResponse
     *
     * =========================================================
     */

    public LoginResponse login(
            LoginRequest request) {


        /*
         * Find user by email.
         */

        User user =
                userRepository
                        .findByEmailIgnoreCase(
                                normalizeEmail(request.getEmail())
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Invalid email or password"
                                )
                        );


        /*
         * =====================================================
         * SELLER PENDING CHECK
         * =====================================================
         */

        if (user.getRole() == Role.SELLER
                && user.getStatus() == UserStatus.PENDING) {

            throw new RuntimeException(
                    "Your seller account is waiting for Admin approval"
            );
        }


        /*
         * =====================================================
         * SELLER REJECTED CHECK
         * =====================================================
         */

        if (user.getRole() == Role.SELLER
                && user.getStatus() == UserStatus.REJECTED) {

            throw new RuntimeException(
                    "Your seller application was rejected"
            );
        }


        /*
         * =====================================================
         * ACCOUNT ENABLED CHECK
         * =====================================================
         */

        if (!user.isEnabled()) {

            throw new RuntimeException(
                    "Your account has been disabled"
            );
        }


        /*
         * =====================================================
         * PASSWORD VERIFICATION
         * =====================================================
         */

        boolean passwordMatches =
                passwordEncoder.matches(
                        request.getPassword(),
                        user.getPassword()
                );


        /*
         * Wrong password.
         */

        if (!passwordMatches) {

            throw new RuntimeException(
                    "Invalid email or password"
            );
        }


        // Changed: customer and seller must confirm a fresh emailed OTP before receiving a JWT.
        if (user.getRole() == Role.CUSTOMER || user.getRole() == Role.SELLER) {
            String otp = String.format("%06d", secureRandom.nextInt(1_000_000));
            user.setLoginOtpHash(passwordEncoder.encode(otp));
            user.setLoginOtpExpiresAt(LocalDateTime.now().plusMinutes(10));
            userRepository.save(user);

            try {
                emailService.sendLoginOtpEmail(user.getEmail(), otp);
            } catch (MailException exception) {
                // Changed: discard an unsent code and explain the real SMTP setup problem to the developer.
                user.setLoginOtpHash(null);
                user.setLoginOtpExpiresAt(null);
                userRepository.save(user);
                throw new RuntimeException(
                        "OTP email could not be sent. Configure SHIVHUB_MAIL_USERNAME "
                        + "and SHIVHUB_MAIL_PASSWORD with a Gmail App Password.");
            }

            LoginResponse response = new LoginResponse();
            response.setEmail(user.getEmail());
            response.setRole(user.getRole());
            response.setOtpRequired(true);
            return response;
        }

        if (user.getRole() == Role.CUSTOMER && !user.isWhatsappVerified()) {
            throw new RuntimeException("Verify your WhatsApp mobile number before logging in. Request a new OTP from registration.");
        }

        return createLoginResponse(user);
    }

    // Changed: completes login only when the code is valid, unexpired, and unused.
    public LoginResponse verifyLoginOtp(VerifyLoginOtpRequest request) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.getEmail())).orElseThrow(
                () -> new RuntimeException("Invalid or expired verification code"));

        if ((user.getRole() != Role.CUSTOMER && user.getRole() != Role.SELLER)
                || user.getLoginOtpHash() == null
                || user.getLoginOtpExpiresAt() == null
                || LocalDateTime.now().isAfter(user.getLoginOtpExpiresAt())
                || !passwordEncoder.matches(request.getOtp(), user.getLoginOtpHash())) {
            throw new RuntimeException("Invalid or expired verification code");
        }

        user.setLoginOtpHash(null);
        user.setLoginOtpExpiresAt(null);
        userRepository.save(user);
        return createLoginResponse(user);
    }

    public LoginResponse socialLogin(SocialLoginRequest request) {
        SocialProfile profile = verifySocialToken(request);
        if (profile.email() == null || profile.email().isBlank()) {
            throw new RuntimeException("Social account did not provide a verified email address");
        }

        String email = normalizeEmail(profile.email());
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);

        if (user != null && user.getRole() != Role.CUSTOMER) {
            throw new RuntimeException("Google/Facebook login is available only for customer accounts");
        }

        if (user == null) {
            user = new User();
            user.setName(profile.name() == null || profile.name().isBlank() ? email : profile.name().trim());
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setRole(Role.CUSTOMER);
            user.setStatus(UserStatus.APPROVED);
            user.setEnabled(true);
            user.setWhatsappVerified(true);
        }

        if (user.getSocialProviderUserId() != null
                && (!profile.provider().equals(user.getSocialProvider())
                || !profile.providerUserId().equals(user.getSocialProviderUserId()))) {
            throw new RuntimeException("This customer account is already linked to another social identity");
        }

        user.setSocialProvider(profile.provider());
        user.setSocialProviderUserId(profile.providerUserId());
        if (user.getProfilePhotoUrl() == null || user.getProfilePhotoUrl().isBlank()) {
            user.setProfilePhotoUrl(profile.pictureUrl());
        }
        userRepository.save(user);
        return createLoginResponse(user);
    }

    private SocialProfile verifySocialToken(SocialLoginRequest request) {
        String provider = normalizeEmail(request.getProvider()).toUpperCase();
        return switch (provider) {
            case "GOOGLE" -> verifyGoogle(request.getToken());
            case "FACEBOOK" -> verifyFacebook(request.getToken());
            default -> throw new RuntimeException("Unsupported social login provider");
        };
    }

    private SocialProfile verifyGoogle(String idToken) {
        try {
            String url = "https://oauth2.googleapis.com/tokeninfo?id_token="
                    + URLEncoder.encode(idToken, StandardCharsets.UTF_8);
            JsonNode data = getJson(url, null);
            if (!"true".equalsIgnoreCase(data.path("email_verified").asText())) {
                throw new RuntimeException("Google email is not verified");
            }
            if (googleClientId != null && !googleClientId.isBlank()
                    && !googleClientId.equals(data.path("aud").asText())) {
                throw new RuntimeException("Google token audience mismatch");
            }
            return new SocialProfile("GOOGLE", data.path("sub").asText(), data.path("email").asText(),
                    data.path("name").asText(), data.path("picture").asText());
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new RuntimeException("Google login verification failed");
        }
    }

    private SocialProfile verifyFacebook(String accessToken) {
        try {
            String url = "https://graph.facebook.com/me?fields=id,name,email,picture&access_token="
                    + URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
            JsonNode data = getJson(url, null);
            String picture = data.path("picture").path("data").path("url").asText(null);
            return new SocialProfile("FACEBOOK", data.path("id").asText(), data.path("email").asText(),
                    data.path("name").asText(), picture);
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new RuntimeException("Facebook login verification failed");
        }
    }

    private JsonNode getJson(String url, String bearerToken) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).GET();
        if (bearerToken != null && !bearerToken.isBlank()) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("Social provider rejected the token");
        }
        return objectMapper.readTree(response.body());
    }

    private record SocialProfile(String provider, String providerUserId, String email, String name, String pictureUrl) {}

    // Changed: one response builder keeps JWT creation exclusively after successful authentication.
    private LoginResponse createLoginResponse(User user) {
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        return new LoginResponse(jwtService.generateToken(user), user.getId(), user.getName(),
                user.getEmail(), user.getRole());
    }

    private void validateNewCustomerDateOfBirth(LocalDate value) {
        if (value == null) throw new IllegalArgumentException("Date of birth is required for new customer registration");
        if (value.isAfter(LocalDate.now())) throw new IllegalArgumentException("Date of birth cannot be in the future");
        if (value.isBefore(LocalDate.now().minusYears(120))) throw new IllegalArgumentException("Enter a reasonable date of birth");
    }
}
