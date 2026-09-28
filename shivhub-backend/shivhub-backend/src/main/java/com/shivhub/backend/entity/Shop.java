package com.shivhub.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;

/** A seller-owned physical branch. Products remain seller-owned until branch inventory is enabled. */
@Entity
@Table(name = "shops", uniqueConstraints = @UniqueConstraint(name = "uk_shop_owner_code", columnNames = {"owner_id", "shop_code"}))
@Data
public class Shop {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "shop_code", nullable = false, length = 30)
    private String shopCode;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 120)
    private String city;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
}
