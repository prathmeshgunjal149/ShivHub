package com.shivhub.backend.entity;
import java.time.LocalDateTime; import jakarta.persistence.*; import lombok.Data;
@Entity @Table(name="referral_invitations") @Data public class ReferralInvitation { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(unique=true,nullable=false) private String code; @Column(nullable=false) private Long referrerCustomerId; @Column(nullable=false) private String invitedEmail; @Column(nullable=false) private boolean used=false; @Column(nullable=false) private LocalDateTime createdAt=LocalDateTime.now(); }
