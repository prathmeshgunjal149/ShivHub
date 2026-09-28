package com.shivhub.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

/** Append-only audit records for non-GST cashbook changes and soft deletions. */
@Entity
@Table(name = "shop_register_audits")
@Data
public class ShopRegisterAudit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "seller_id", nullable = false) private User seller;
    @Column(name = "entry_id") private Long entryId;
    @Column(nullable = false, length = 30) private String action;
    @Column(nullable = false, length = 150) private String actionByName;
    @Column(columnDefinition = "TEXT") private String previousSnapshot;
    @Column(columnDefinition = "TEXT") private String nextSnapshot;
    @Column(columnDefinition = "TEXT") private String reason;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @PrePersist void created() { createdAt = LocalDateTime.now(); }
}
