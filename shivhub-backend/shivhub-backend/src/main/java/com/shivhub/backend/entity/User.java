package com.shivhub.backend.entity;

import java.time.LocalDateTime;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.enums.UserStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/*
 * =========================================================
 * User Entity
 * =========================================================
 *
 * This class represents the "users" table in ShivHub.
 *
 * ShivHub has three types of users:
 *
 * 1. CUSTOMER
 * 2. SELLER
 * 3. ADMIN
 *
 *
 * Seller approval flow:
 *
 * Seller Registration
 *        ↓
 * status = PENDING
 * enabled = false
 *        ↓
 * Admin Approval
 *        ↓
 * status = APPROVED
 * enabled = true
 *
 * Customer registration:
 *
 * Registration
 *      ↓
 * status = APPROVED
 * enabled = true
 *
 * =========================================================
 */

@Entity
@Table(name = "users")

@Data
@NoArgsConstructor
@AllArgsConstructor

public class User {
    @com.fasterxml.jackson.annotation.JsonIgnore @jakarta.persistence.Column(precision=10,scale=7) private java.math.BigDecimal businessLatitude;
    @com.fasterxml.jackson.annotation.JsonIgnore @jakarta.persistence.Column(precision=10,scale=7) private java.math.BigDecimal businessLongitude;
    @com.fasterxml.jackson.annotation.JsonIgnore @jakarta.persistence.Column(length=64) private String businessGeocodedAddressHash;


    /*
     * =========================================================
     * PRIMARY KEY
     * =========================================================
     *
     * Unique ID of every user.
     *
     * Example:
     *
     * 1
     * 2
     * 3
     * =========================================================
     */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    /*
     * =========================================================
     * USER NAME
     * =========================================================
     */

    @Column(nullable = false)
    private String name;


    /*
     * =========================================================
     * EMAIL
     * =========================================================
     *
     * Every user must have a unique email.
     * =========================================================
     */

    @Column(nullable = false, unique = true)
    private String email;


    /*
     * =========================================================
     * PASSWORD
     * =========================================================
     *
     * IMPORTANT:
     *
     * We never store the original password.
     *
     * PasswordEncoder / BCrypt will convert the password
     * into a secure hash before saving it.
     *
     * Example:
     *
     * 123456
     *
     * becomes something like:
     *
     * $2a$10$........
     *
     * =========================================================
     */

    @Column(nullable = false)
    @JsonIgnore
    private String password;


    /*
     * =========================================================
     * MOBILE
     * =========================================================
     *
     * Mobile number must be unique.
     *
     * It is allowed to be NULL because mobile may not
     * always be mandatory for every registration flow.
     * =========================================================
     */

    @Column(unique = true)
    private String mobile;

    /** Shareable code generated for every customer. */
    @Column(name = "referral_code", unique = true, length = 20)
    private String referralCode;

    /** Customer who referred this customer; null when no referral was used. */
    @Column(name = "referred_by_customer_id")
    private Long referredByCustomerId;

    /** Seller/distributor accounting profile. Optional for customers. */
    @Column(name = "business_name")
    private String businessName;

    @Column(name = "gstin", length = 15)
    private String gstin;

    @Column(name = "business_address", columnDefinition = "TEXT")
    private String businessAddress;

    @Column(name = "invoice_terms", columnDefinition = "TEXT")
    private String invoiceTerms;

    /* Complete seller shop profile; these are optional until the shop completes Settings. */
    @Column(name = "legal_business_name") private String legalBusinessName;
    @Column(name = "shop_logo_url", columnDefinition = "TEXT") private String shopLogoUrl;
    @Column(name = "alternate_mobile", length = 15) private String alternateMobile;
    @Column(name = "business_city", length = 100) private String businessCity;
    @Column(name = "business_district", length = 100) private String businessDistrict;
    @Column(name = "business_state", length = 100) private String businessState;
    @Column(name = "business_pincode", length = 10) private String businessPincode;
    @Column(name = "google_maps_url", length = 2000) private String googleMapsUrl;
    @Column(name = "website_url", columnDefinition = "TEXT") private String websiteUrl;
    @Column(name = "instagram_url", columnDefinition = "TEXT") private String instagramUrl;
    @Column(name = "facebook_url", columnDefinition = "TEXT") private String facebookUrl;
    @Column(name = "whatsapp_url", columnDefinition = "TEXT") private String whatsappUrl;
    @Column(name = "youtube_url", columnDefinition = "TEXT") private String youtubeUrl;
    @Column(name = "shop_opening_date") private java.time.LocalDate shopOpeningDate;
    @Column(name = "shop_type", length = 20) private String shopType;
    @Column(name = "pan_number", length = 20) private String panNumber;
    @Column(name = "business_registration_number", length = 100) private String businessRegistrationNumber;
    @Column(name = "gst_registration_type", length = 50) private String gstRegistrationType;
    @Column(name = "state_code", length = 5) private String stateCode;
    @Column(name = "tax_settings", columnDefinition = "TEXT") private String taxSettings;
    @Column(name = "gst_rates", length = 100) private String gstRates;
    @Column(name = "hsn_sac_settings", columnDefinition = "TEXT") private String hsnSacSettings;
    @Column(name = "invoice_prefix", length = 30) private String invoicePrefix;
    @Column(name = "warranty_period", length = 100) private String warrantyPeriod;
    @Column(name = "warranty_type", length = 50) private String warrantyType;
    @Column(name = "return_policy", columnDefinition = "TEXT") private String returnPolicy;
    @Column(name = "replacement_policy", columnDefinition = "TEXT") private String replacementPolicy;
    @Column(name = "exchange_policy", columnDefinition = "TEXT") private String exchangePolicy;
    @Column(name = "refund_policy", columnDefinition = "TEXT") private String refundPolicy;
    @Column(name = "warranty_terms", columnDefinition = "TEXT") private String warrantyTerms;

    /* Seller-controlled fields for newly generated POS invoices. */
    @Column(nullable = false) private boolean invoiceShowAddress = true;
    @Column(nullable = false) private boolean invoiceShowMobile = true;
    @Column(nullable = false) private boolean invoiceShowGstin = true;
    @Column(nullable = false) private boolean invoiceShowCustomerDetails = true;
    @Column(nullable = false) private boolean invoiceShowNotes = true;
    @Column(nullable = false) private boolean invoiceShowWebsite = true;
    @Column(nullable = false) private boolean invoiceShowSocialLinks = false;


    /*
     * =========================================================
     * USER ROLE
     * =========================================================
     *
     * Possible values:
     *
     * CUSTOMER
     * SELLER
     * ADMIN
     *
     * EnumType.STRING stores the actual text in MySQL.
     *
     * Example:
     *
     * CUSTOMER
     *
     * instead of:
     *
     * 0
     *
     * =========================================================
     */

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;


    /*
     * =========================================================
     * ACCOUNT STATUS
     * =========================================================
     *
     * Possible values:
     *
     * PENDING
     * APPROVED
     * REJECTED
     *
     *
     * SELLER:
     *
     * Registration
     *      ↓
     * PENDING
     *      ↓
     * Admin Approval
     *      ↓
     * APPROVED
     *
     *
     * CUSTOMER:
     *
     * Registration
     *      ↓
     * APPROVED
     *
     * =========================================================
     */

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    /** Admin-provided explanation when a seller application is rejected. */
    @Column(columnDefinition = "TEXT")
    private String rejectionReason;


    /*
     * =========================================================
     * ENABLED
     * =========================================================
     *
     * true:
     * Account can login/use the system.
     *
     * false:
     * Account is disabled.
     *
     *
     * Seller before Admin approval:
     *
     * enabled = false
     *
     *
     * Seller after Admin approval:
     *
     * enabled = true
     *
     * =========================================================
     */

    @Column(nullable = false)
    private boolean enabled = true;

    /** A mobile number becomes a WhatsApp notification destination only after OTP verification. */
    @Column(name = "whatsapp_verified", nullable = false)
    private boolean whatsappVerified = false;

    /** Customers may opt out of non-essential WhatsApp messages without affecting transactional email. */
    @Column(name = "whatsapp_opt_in", nullable = false)
    private boolean whatsappOptIn = true;

    // Changed: login OTP is stored as a BCrypt hash and removed once it is used or expires.
    @JsonIgnore
    private String loginOtpHash;

    // Changed: OTP expiry prevents an old email code from being reused.
    private LocalDateTime loginOtpExpiresAt;

    @Column(name = "profile_photo_url", columnDefinition = "TEXT")
    private String profilePhotoUrl;

    @Column(name = "marketing_opt_out", nullable = false)
    private boolean marketingOptOut = false;

    /** Optional for migrated accounts; required by the new customer registration flow. */
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "social_provider", length = 30)
    private String socialProvider;

    @Column(name = "social_provider_user_id", length = 255)
    private String socialProviderUserId;

    @JsonIgnore
    @Column(name = "password_reset_token_hash", length = 255)
    private String passwordResetTokenHash;

    @Column(name = "password_reset_expires_at")
    private LocalDateTime passwordResetExpiresAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;


    /*
     * =========================================================
     * CREATED AT
     * =========================================================
     *
     * Stores when the account was created.
     * =========================================================
     */

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;


    /*
     * =========================================================
     * UPDATED AT
     * =========================================================
     *
     * Stores when the account was last updated.
     * =========================================================
     */

    private LocalDateTime updatedAt;


    /*
     * =========================================================
     * PRE-PERSIST
     * =========================================================
     *
     * Runs automatically before inserting a new user.
     *
     * Sets:
     *
     * createdAt
     * updatedAt
     * =========================================================
     */

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;
    }


    /*
     * =========================================================
     * PRE-UPDATE
     * =========================================================
     *
     * Runs automatically before updating an existing user.
     *
     * Updates updatedAt.
     * =========================================================
     */

    @PreUpdate
    protected void onUpdate() {

        updatedAt = LocalDateTime.now();
    }
}
