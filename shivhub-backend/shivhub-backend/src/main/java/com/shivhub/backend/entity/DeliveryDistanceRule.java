package com.shivhub.backend.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity @Table(name="delivery_distance_rules", indexes=@Index(name="idx_delivery_scope_active",columnList="product_scope,active")) @Getter @Setter
public class DeliveryDistanceRule {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=120) private String name;
    @Column(name="product_scope",nullable=false,length=30) private String productScope="MOBILE_ONLY";
    @Column(nullable=false,precision=10,scale=3) private BigDecimal minimumDistanceKm;
    @Column(precision=10,scale=3) private BigDecimal maximumDistanceKm;
    @Column(nullable=false,length=500) private String estimatedDeliveryText;
    private Integer estimatedMinutes;
    @Column(nullable=false) private boolean active=true;
    @Column(nullable=false) private int priority;
    @Column(nullable=false,precision=12,scale=2) private BigDecimal deliveryCharge=BigDecimal.ZERO;
    @Column(nullable=false) private boolean serviceAvailable=true;
    @Version private Long version;
}
