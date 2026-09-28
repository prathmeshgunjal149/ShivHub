package com.shivhub.backend.entity;
import jakarta.persistence.*; import lombok.Data;
@Entity @Table(name="marketplace_categories", uniqueConstraints=@UniqueConstraint(columnNames="name")) @Data
public class MarketplaceCategory { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(nullable=false,length=100) private String name; private boolean active=true; @Column(name="min_listing_days",nullable=false) private int minListingDays=1; @Column(name="max_listing_days",nullable=false) private int maxListingDays=10; }
