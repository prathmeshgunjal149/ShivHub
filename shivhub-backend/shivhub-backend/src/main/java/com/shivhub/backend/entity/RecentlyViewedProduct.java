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
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "recently_viewed_products",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_recent_customer_product",
                columnNames = {"customer_id", "product_id"}
        ),
        indexes = {
                @Index(name = "idx_recent_customer_time", columnList = "customer_id,last_viewed_at"),
                @Index(name = "idx_recent_product", columnList = "product_id")
        }
)
@Data
@NoArgsConstructor
public class RecentlyViewedProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "last_viewed_at", nullable = false)
    private LocalDateTime lastViewedAt;

    @PrePersist
    void onCreate() {
        if (lastViewedAt == null) {
            lastViewedAt = LocalDateTime.now();
        }
    }
}
