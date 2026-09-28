package com.shivhub.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "site_settings")
@Data
@NoArgsConstructor
public class SiteSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String brandName;

    @Column(nullable = false, length = 200)
    private String legalBusinessName;

    @Column(nullable = false, length = 180)
    private String supportEmail;

    @Column(length = 80)
    private String supportPhone;

    @Column(columnDefinition = "TEXT")
    private String businessAddress;

    @Column(length = 180)
    private String supportHours;

    @Column(length = 160)
    private String grievanceContactName;

    @Column(length = 180)
    private String grievanceEmail;

    @Column(length = 2000)
    private String websiteUrl;

    @Column(length = 2000)
    private String instagramUrl;

    @Column(length = 2000)
    private String facebookUrl;

    @Column(length = 2000)
    private String whatsappUrl;

    @Column(length = 2000)
    private String youtubeUrl;

    @Column(length = 500)
    private String enabledPaymentMethods;

    @Column(nullable = false)
    private boolean demoData = true;

    @Column(nullable = false)
    private boolean requiresReview = true;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
