package com.shivhub.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

/** Single admin-managed birthday email template. No credentials are stored here. */
@Entity
@Table(name = "birthday_greeting_settings")
@Data
public class BirthdayGreetingSetting {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "email_subject", nullable = false, length = 250)
    private String emailSubject;
    @Column(name = "message_content", nullable = false, columnDefinition = "TEXT")
    private String messageContent;
    @Column(name = "coupon_code", length = 100)
    private String couponCode;
    @Column(name = "banner_url", length = 2000)
    private String bannerUrl;
    @Column(name = "is_active", nullable = false)
    private boolean active;
}
