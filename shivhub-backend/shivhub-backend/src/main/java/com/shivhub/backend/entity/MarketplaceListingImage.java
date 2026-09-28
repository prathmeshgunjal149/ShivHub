package com.shivhub.backend.entity;
import jakarta.persistence.*; import lombok.Data;
@Entity @Table(name="marketplace_listing_images") @Data public class MarketplaceListingImage { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="listing_id",nullable=false) private MarketplaceListing listing; @Column(name="image_url",nullable=false,length=1000) private String imageUrl; @Column(name="display_order",nullable=false) private int displayOrder; }
