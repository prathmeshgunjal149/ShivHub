package com.shivhub.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.shivhub.backend.entity.Product;
import com.shivhub.backend.entity.StockMovement;
import com.shivhub.backend.entity.User;

@Repository
public interface StockMovementRepository
        extends JpaRepository<StockMovement, Long> {

    /*
     * =========================================================
     * GET ALL MOVEMENTS OF ONE PRODUCT
     * =========================================================
     */

    List<StockMovement> findByProductOrderByCreatedAtDesc(
            Product product
    );


    /*
     * =========================================================
     * GET MOVEMENTS BY PRODUCT ID
     * =========================================================
     */

    List<StockMovement> findByProductIdOrderByCreatedAtDesc(
            Long productId
    );


    /*
     * =========================================================
     * GET SELLER STOCK HISTORY
     * =========================================================
     */

    List<StockMovement> findBySellerOrderByCreatedAtDesc(
            User seller
    );


    /*
     * =========================================================
     * GET SELLER + PRODUCT HISTORY
     * =========================================================
     */

    List<StockMovement> findBySellerAndProductOrderByCreatedAtDesc(
            User seller,
            Product product
    );


    /*
     * =========================================================
     * GET MOVEMENTS OF ONE PURCHASE
     * =========================================================
     *
     * referenceType = PURCHASE
     * referenceId   = Purchase ID
     *
     * =========================================================
     */

    List<StockMovement> findByReferenceTypeAndReferenceId(
            String referenceType,
            Long referenceId
    );


    /*
     * =========================================================
     * CHECK DUPLICATE MOVEMENT
     * =========================================================
     *
     * Useful to prevent the same purchase stock from being
     * added twice.
     *
     * =========================================================
     */

    boolean existsByReferenceTypeAndReferenceIdAndProductId(
            String referenceType,
            Long referenceId,
            Long productId
    );
}