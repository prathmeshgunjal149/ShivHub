package com.shivhub.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shivhub.backend.entity.ProductImage;
import com.shivhub.backend.enums.ImageType;


/*
 * =========================================================
 * ProductImageRepository
 * =========================================================
 *
 * Handles database operations for:
 *
 *     product_images
 *
 * This repository provides:
 *
 * 1. Get all images of a product
 * 2. Get images by type
 * 3. Count product images
 * 4. Delete all images of a product
 *
 * =========================================================
 */

public interface ProductImageRepository
        extends JpaRepository<ProductImage, Long> {


    /*
     * =====================================================
     * FIND IMAGES BY PRODUCT
     * =====================================================
     *
     * Returns all images of a product.
     *
     * Images are returned according to:
     *
     * displayOrder ASC
     *
     * Example:
     *
     * Product ID = 1
     *
     * Result:
     *
     * Image 1
     * Image 2
     * Image 3
     * Image 4
     * Image 5
     *
     * =====================================================
     */

    List<ProductImage> findByProductIdOrderByDisplayOrderAsc(
            Long productId
    );


    /*
     * =====================================================
     * FIND IMAGES BY PRODUCT AND TYPE
     * =====================================================
     *
     * Image types:
     *
     * PRODUCT
     * DESCRIPTION
     *
     * Example:
     *
     * findByProductIdAndImageType(
     *      1,
     *      ImageType.PRODUCT
     * )
     *
     * returns only normal product images.
     *
     * =====================================================
     */

    List<ProductImage> findByProductIdAndImageType(
            Long productId,
            ImageType imageType
    );


    /*
     * =====================================================
     * COUNT IMAGES BY PRODUCT AND TYPE
     * =====================================================
     *
     * Used for image validation.
     *
     * For example:
     *
     * Product ID = 1
     * ImageType = PRODUCT
     *
     * Count = 5
     *
     * =====================================================
     */

    long countByProductIdAndImageType(
            Long productId,
            ImageType imageType
    );


    /*
     * =====================================================
     * DELETE ALL IMAGES OF A PRODUCT
     * =====================================================
     *
     * Deletes all ProductImage records belonging
     * to the specified product.
     *
     * =====================================================
     */

    void deleteByProductId(
            Long productId
    );
}