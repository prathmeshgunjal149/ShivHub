package com.shivhub.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * Stores the two public OAuth application identifiers that the browser needs
 * to open Google and Facebook sign-in. These identifiers are not secrets.
 */
@Entity
@Table(name = "social_login_configuration")
@Data
public class SocialLoginConfiguration {
    /** A singleton configuration row keeps the settings simple and auditable. */
    @Id
    private Long id = 1L;

    @Column(name = "google_client_id", length = 400)
    private String googleClientId;

    @Column(name = "facebook_app_id", length = 400)
    private String facebookAppId;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void updated() {
        updatedAt = LocalDateTime.now();
    }
}
