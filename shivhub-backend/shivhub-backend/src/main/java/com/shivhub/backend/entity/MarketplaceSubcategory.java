package com.shivhub.backend.entity;
import jakarta.persistence.*; import lombok.Data;
@Entity @Table(name="marketplace_subcategories", uniqueConstraints=@UniqueConstraint(columnNames={"category_id","name"})) @Data
public class MarketplaceSubcategory { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="category_id",nullable=false) private MarketplaceCategory category; @Column(nullable=false,length=100) private String name; private boolean active=true; }
