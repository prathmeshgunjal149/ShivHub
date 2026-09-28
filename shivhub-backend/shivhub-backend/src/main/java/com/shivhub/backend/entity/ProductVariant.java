package com.shivhub.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Data;

/** Non-mobile sellable SKU/stock combination. Mobile IMEI records remain unchanged. */
@Entity
@Table(name = "product_variants", uniqueConstraints = @UniqueConstraint(name = "uk_product_variant_signature", columnNames = {"product_id", "attribute_signature"}), indexes = @Index(name = "idx_product_variant_product_active", columnList = "product_id,active"))
@Data
public class ProductVariant {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "product_id", nullable = false) private Product product;
    @Column(name = "variant_sku", length = 100) private String variantSku;
    @Column(length = 80, unique = true) private String barcode;
    @Column(name = "attributes_json", nullable = false, columnDefinition = "TEXT") private String attributesJson;
    @Column(name = "attribute_signature", nullable = false, length = 500) private String attributeSignature;
    @Column(name = "selling_price_including_gst", nullable = false, precision = 12, scale = 2) private BigDecimal sellingPriceIncludingGst;
    /** Optional supplier cost for stock receipts; legacy variants remain valid. */
    @Column(name = "purchase_price", precision = 12, scale = 2) private BigDecimal purchasePrice;
    @Column(name = "compare_at_price", precision = 12, scale = 2) private BigDecimal compareAtPrice;
    @Column(name = "stock_quantity", nullable = false) private Integer stockQuantity = 0;
    @Column(name = "reserved_quantity", nullable = false) private Integer reservedQuantity = 0;
    @Column(name = "image_url", length = 1000) private String imageUrl;
    @Column(nullable = false) private boolean active = true;
    @Column(name = "reorder_threshold") private Integer reorderThreshold;
    @Version private Long version;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;
    @PrePersist void create(){ createdAt=LocalDateTime.now(); updatedAt=createdAt; }
    @PreUpdate void update(){ updatedAt=LocalDateTime.now(); }
    public int getAvailableStock(){ return Math.max(0,(stockQuantity==null?0:stockQuantity)-(reservedQuantity==null?0:reservedQuantity)); }
}
