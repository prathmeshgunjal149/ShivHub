package com.shivhub.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;

/** In-app campaign notification. External WhatsApp/SMS providers can be added without changing campaigns. */
@Entity
@Table(name = "notifications")
@Data
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "recipient_user_id")
    private User recipientUser;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "campaign_id")
    private MarketingCampaign campaign;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(length = 1000)
    private String imageUrl;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime readAt;

    @PrePersist void created() { createdAt = LocalDateTime.now(); }
}
