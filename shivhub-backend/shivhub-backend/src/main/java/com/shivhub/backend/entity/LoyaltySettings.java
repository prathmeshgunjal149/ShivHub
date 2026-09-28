package com.shivhub.backend.entity;

import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.Data;

/** Global default and optional seller override for transparent point calculations. */
@Entity
@Table(name = "loyalty_settings", uniqueConstraints =
        @UniqueConstraint(name = "uk_loyalty_scope_seller", columnNames = {"scope_type", "seller_id"}))
@Data
public class LoyaltySettings {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "scope_type", nullable = false, length = 12)
    private String scopeType = "GLOBAL";
    @Column(name = "seller_id")
    private Long sellerId;
    @Column(name = "minimum_purchase_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal minimumPurchaseAmount = BigDecimal.ZERO;
    @Column(name = "points_per_purchase_unit", nullable = false)
    private long pointsPerPurchaseUnit = 1;
    @Column(name = "purchase_unit_in_rupees", precision = 12, scale = 2, nullable = false)
    private BigDecimal purchaseUnitInRupees = BigDecimal.valueOf(100);
    @Column(name = "point_value_in_rupees", precision = 12, scale = 2, nullable = false)
    private BigDecimal pointValueInRupees = BigDecimal.ONE;
    @Column(name = "maximum_points_per_sale", nullable = false)
    private long maximumPointsPerSale = 0;
    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
