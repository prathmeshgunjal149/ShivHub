package com.shivhub.backend.entity;

import java.time.LocalDateTime;
import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Canonical customer identity for both registered ShivHub users and walk-in POS buyers.
 * A profile can exist without an online account; this prevents offline customers from
 * being lost while still allowing a single profile to span multiple seller shops.
 */
@Entity
@Table(name = "customer_profiles", indexes = {
        @Index(name = "idx_customer_profile_mobile", columnList = "mobile"),
        @Index(name = "idx_customer_profile_email", columnList = "email"),
        @Index(name = "idx_customer_profile_dob", columnList = "date_of_birth")
})
@Data
public class CustomerProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "online_user_id", unique = true)
    private User onlineUser;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 30)
    private String mobile;

    @Column(length = 150)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String district;

    @Column(length = 100)
    private String state;

    @Column(length = 12)
    private String pincode;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /** Consent applies to optional marketing only; transactional messages remain unaffected. */
    @Column(name = "communication_consent", nullable = false)
    private boolean communicationConsent = true;

    /** Explicit seller-captured consent for WhatsApp invoices and optional updates. */
    @Column(name = "whatsapp_consent", nullable = false)
    private boolean whatsappConsent;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist void created() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate void updated() { updatedAt = LocalDateTime.now(); }
}
