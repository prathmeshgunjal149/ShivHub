package com.shivhub.backend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.shivhub.backend.enums.ImageType;

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
import jakarta.persistence.Table;


/*
 * =========================================================
 * ProductImage Entity
 * =========================================================
 *
 * Stores information about a product image.
 *
 * Actual image file:
 *
 * uploads/products/1/abc.jpg
 *
 * Database stores:
 *
 * imageUrl
 * imageType
 * displayOrder
 *
 * =========================================================
 */

@Entity
@Table(name = "product_images")
public class ProductImage {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    /*
     * Path of the actual image file.
     */

    /** External CDN/image-provider URLs routinely exceed the default varchar(255). */
    @Column(name = "image_url", nullable = false, length = 2048)
    private String imageUrl;


    /*
     * PRODUCT
     *     → Normal product image
     *
     * DESCRIPTION
     *     → Product description/specification image
     */

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImageType imageType;


    /*
     * Controls the order in which images appear.
     *
     * 1
     * 2
     * 3
     * 4
     * 5
     */

    private Integer displayOrder;


    /*
     * =========================================================
     * PRODUCT RELATIONSHIP
     * =========================================================
     *
     * Many images belong to one Product.
     *
     * JsonBackReference tells Jackson:
     *
     * "Do NOT serialize the Product again here."
     *
     * This fixes:
     *
     * Product
     *   ↓
     * images
     *   ↓
     * product
     *   ↓
     * images
     *   ↓
     * product
     *
     * infinite JSON recursion.
     *
     * =========================================================
     */

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "product_id",
            nullable = false
    )
    private Product product;


    /*
     * Default constructor
     */

    public ProductImage() {
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }


    public ImageType getImageType() {
        return imageType;
    }

    public void setImageType(ImageType imageType) {
        this.imageType = imageType;
    }


    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }


    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }
}
