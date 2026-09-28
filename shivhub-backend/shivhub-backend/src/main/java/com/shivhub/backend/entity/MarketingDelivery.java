package com.shivhub.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "marketing_deliveries", indexes = {
        @Index(name = "idx_marketing_delivery_campaign_status", columnList = "campaign_id,status"),
        @Index(name = "idx_marketing_delivery_customer", columnList = "customer_profile_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_campaign_customer_channel", columnNames = {"campaign_id", "customer_profile_id", "channel"})
})
@Data
public class MarketingDelivery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    @JsonIgnore
    private MarketingCampaign campaign;

    @Column(name = "recipient_user_id")
    private Long recipientUserId;

    /** Customer-profile identity works for both online and registered POS customers. */
    @Column(name = "customer_profile_id")
    private Long customerProfileId;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(name = "recipient_name")
    private String recipientName;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 20)
    private String channel;

    @Column(columnDefinition = "TEXT")
    private String message;

    private LocalDateTime sentAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;
    private LocalDateTime openedAt;
    private LocalDateTime clickedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void create() {
        createdAt = LocalDateTime.now();
    }
}
