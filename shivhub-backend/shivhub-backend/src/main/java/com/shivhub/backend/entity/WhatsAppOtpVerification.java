package com.shivhub.backend.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Data;

/** One-use registration OTP. Only the BCrypt hash is retained. */
@Entity
@Table(name = "whatsapp_otp_verifications", indexes = @Index(name = "idx_whatsapp_otp_user", columnList = "user_id"))
@Data
public class WhatsAppOtpVerification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(name = "otp_hash", nullable = false, length = 255) private String otpHash;
    @Column(nullable = false) private LocalDateTime expiresAt;
    @Column(nullable = false) private int attempts = 0;
    @Column(nullable = false) private LocalDateTime sentAt;
    private LocalDateTime verifiedAt;
}
