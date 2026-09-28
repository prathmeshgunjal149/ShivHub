package com.shivhub.backend.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.shivhub.backend.enums.ProductSource;
import com.shivhub.backend.enums.ProductStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/*
 * =========================================================
 * Product Entity
 * =========================================================
 *
 * Product creation flow:
 *
 * SELLER
 *    ↓
 * Add Product
 *    ↓
 * PENDING
 *    ↓
 * ADMIN REVIEW
 *    ├── APPROVED
 *    └── REJECTED
 *
 *
 * ADMIN
 *    ↓
 * Add Product
 *    ↓
 * PENDING
 *    ↓
 * ADMIN REVIEW
 *    ├── APPROVED
 *    └── REJECTED
 *
 *
 * CUSTOMER:
 *
 * approvalStatus = APPROVED
 * AND
 * active = true
 *
 * =========================================================
 */

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    /*
     * =====================================================
     * FINAL SELLING PRICE
     * =====================================================
     *
     * Current payable price after optional admin offer.
     *
     * =====================================================
     */

    public BigDecimal getFinalSellingPrice() {

        if (price == null
                || offerPercentage == null
                || offerPercentage.signum() <= 0) {

            return price;
        }

        return price
                .multiply(
                        BigDecimal.ONE.subtract(
                                offerPercentage.movePointLeft(2)
                        )
                )
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                );
    }


    /*
     * =====================================================
     * PRIMARY KEY
     * =====================================================
     */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    /*
     * =====================================================
     * PRODUCT NAME
     * =====================================================
     */

    @Column(nullable = false)
    private String name;


    /*
     * =====================================================
     * BRAND
     * =====================================================
     *
     * Example:
     *
     * OPPO
     * VIVO
     * SAMSUNG
     * APPLE
     * ONEPLUS
     *
     * This is also used when matching a product
     * with SellerDistributor brand.
     *
     * =====================================================
     */

    @Column(length = 100)
    private String brand;

    @Column(length = 150)
    private String model;

    @Column(length = 150)
    private String modelNumber;

    @Column(length = 50)
    private String ram;

    @Column(length = 50)
    private String storage;

    @Column(length = 500)
    private String colorOptions;

    @Column(length = 20)
    private String hsnCode;

    @Column(columnDefinition = "TEXT")
    private String specificationDetails;

    /** JSON object of category/subcategory fields. Existing specificationDetails remains supported. */
    @Column(name = "product_specifications", columnDefinition = "TEXT")
    private String productSpecifications;

    @Column(name = "variants_enabled", nullable = false)
    private boolean variantsEnabled = false;

    @JsonIgnore
    @OneToMany(mappedBy = "product", cascade = CascadeType.PERSIST)
    private List<ProductVariant> variants = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String shortHighlights;

    @Column(precision = 5, scale = 2)
    private BigDecimal gstRate;

    @Column(nullable = false)
    private boolean sellingPriceIncludesGst = true;

    @Column(precision = 12, scale = 2)
    private BigDecimal sellingTaxablePrice = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal sellingCgst = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal sellingSgst = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal sellingIgst = BigDecimal.ZERO;

    @Column(length = 80)
    private String sellerSku;

    /** Optional manufacturer/EAN barcode. Null keeps all existing products compatible. */
    @Column(length = 80)
    private String barcode;

    @Column(length = 40)
    private String productType = "NEW_MOBILE";

    @Column(length = 40)
    private String physicalCondition = "NEW";

    @Column(length = 40)
    private String purchaseTaxTreatment = "REGULAR_GST";

    @Column(length = 40)
    private String saleTaxTreatment = "REGULAR_GST";

    @Column(length = 120)
    private String accessoryType;

    @Column(length = 1000)
    private String compatibility;

    @Column(length = 500)
    private String warrantyDetails;

    @Column(length = 1000)
    private String packageContents;

    @Column(length = 1000)
    private String taxTreatmentBasis;

    @Column
    private Integer reorderThreshold;

    @Column(nullable = false)
    private boolean serialTrackingRequired = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalogue_parent_id")
    @JsonIgnore
    private Product catalogueParent;

    public Long getCatalogueParentId() {
        return catalogueParent == null ? null : catalogueParent.getId();
    }


    /*
     * =====================================================
     * DESCRIPTION
     * =====================================================
     */

    @Column(columnDefinition = "TEXT")
    private String description;


    /*
     * =====================================================
     * PRICE
     * =====================================================
     */

    @Column(
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal price;

    /*
     * =====================================================
     * OFFER PERCENTAGE
     * =====================================================
     *
     * Percentage discount configured by admin.
     *
     * Example:
     *
     * 10 = 10%
     * 20 = 20%
     *
     * =====================================================
     */

    @Column(
            precision = 5,
            scale = 2
    )
    private BigDecimal offerPercentage;


    /*
     * =====================================================
     * STOCK
     * =====================================================
     */

    @Column(nullable = false)
    private Integer stock;

    @Column(name = "reserved_stock", nullable = false)
    private Integer reservedStock = 0;

    public int getAvailableStock() {
        return Math.max(0, (stock == null ? 0 : stock) - (reservedStock == null ? 0 : reservedStock));
    }


    /*
     * =====================================================
     * OLD CATEGORY FIELD
     * =====================================================
     *
     * Kept for existing products/database compatibility.
     *
     * =====================================================
     */

    @Column
    private String category;


    /*
     * =====================================================
     * CATEGORY RELATIONSHIP
     * =====================================================
     */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category categoryEntity;


    /*
     * =====================================================
     * SUBCATEGORY RELATIONSHIP
     * =====================================================
     */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subcategory_id")
    private SubCategory subCategory;


    /*
     * =====================================================
     * OLD IMAGE URL
     * =====================================================
     *
     * Kept for old products/database compatibility.
     *
     * New products use ProductImage.
     *
     * =====================================================
     */

    private String imageUrl;


    /*
     * =====================================================
     * PRODUCT IMAGES
     * =====================================================
     */

    @JsonManagedReference
    @OneToMany(
            mappedBy = "product",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("displayOrder ASC")
    private List<ProductImage> images =
            new ArrayList<>();


    /*
     * =====================================================
     * ACTIVE
     * =====================================================
     */

    @Column(nullable = false)
    private boolean active = false;


    /*
     * =====================================================
     * APPROVAL STATUS
     * =====================================================
     */

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private ProductStatus approvalStatus =
            ProductStatus.PENDING;


    /*
     * =====================================================
     * PRODUCT SOURCE
     * =====================================================
     *
     * SELLER
     * ADMIN
     *
     * =====================================================
     */

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private ProductSource source =
            ProductSource.SELLER;


    /*
     * =====================================================
     * ADMIN REVIEW
     * =====================================================
     */

    @Column(columnDefinition = "TEXT")
    private String adminReview;


    /*
     * =====================================================
     * REVIEWED AT
     * =====================================================
     */

    private LocalDateTime reviewedAt;


    /*
     * =====================================================
     * SELLER / OWNER
     * =====================================================
     *
     * Existing database requires seller_id.
     *
     * =====================================================
     */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "seller_id",
            nullable = false
    )
    private User seller;


    /*
     * =====================================================
     * CREATED AT
     * =====================================================
     */

    @Column(
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;


    /*
     * =====================================================
     * UPDATED AT
     * =====================================================
     */

    @Column(nullable = false)
    private LocalDateTime updatedAt;


    /*
     * =====================================================
     * BEFORE INSERT
     * =====================================================
     */

    @PrePersist
    protected void onCreate() {

        LocalDateTime now =
                LocalDateTime.now();

        createdAt = now;

        updatedAt = now;


        /*
         * New products always start as PENDING.
         */

        if (approvalStatus == null) {

            approvalStatus =
                    ProductStatus.PENDING;
        }


        /*
         * Default source is SELLER.
         */

        if (source == null) {

            source =
                    ProductSource.SELLER;
        }


        /*
         * Product should not be visible before approval.
         */

        if (approvalStatus != ProductStatus.APPROVED) {

            active = false;
        }
    }


    /*
     * =====================================================
     * BEFORE UPDATE
     * =====================================================
     */

    @PreUpdate
    protected void onUpdate() {

        updatedAt =
                LocalDateTime.now();
    }
}
