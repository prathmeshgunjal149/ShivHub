package com.shivhub.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.shivhub.backend.entity.Product;
import com.shivhub.backend.entity.Purchase;
import com.shivhub.backend.entity.PurchaseItem;


/*
 * =========================================================
 * PURCHASE ITEM REPOSITORY
 * =========================================================
 *
 * Handles database operations for:
 *
 *                 purchase_items
 *
 *
 * Structure:
 *
 * Purchase
 *    ↓
 * PurchaseItem
 *    ↓
 * Product
 *
 *
 * Example:
 *
 * Purchase Invoice
 *       ↓
 * ┌─────────────────────────────┐
 * │ OPPO A5      Qty 5          │
 * │ OPPO A6      Qty 3          │
 * │ OPPO Reno    Qty 2          │
 * └─────────────────────────────┘
 *
 * Each row is a PurchaseItem.
 *
 * =========================================================
 */

@Repository
public interface PurchaseItemRepository
        extends JpaRepository<PurchaseItem, Long> {


    /*
     * =========================================================
     * GET ITEMS OF ONE PURCHASE
     * =========================================================
     *
     * Example:
     *
     * Purchase #10
     *      ↓
     *      ├── OPPO A5
     *      ├── OPPO A6
     *      └── OPPO Reno
     *
     * =========================================================
     */

    List<PurchaseItem> findByPurchase(
            Purchase purchase
    );


    /*
     * =========================================================
     * GET ITEMS BY PURCHASE ID
     * =========================================================
     *
     * Useful for Purchase Details page.
     *
     * =========================================================
     */

    List<PurchaseItem> findByPurchaseId(
            Long purchaseId
    );


    /*
     * =========================================================
     * GET ALL PURCHASE RECORDS OF A PRODUCT
     * =========================================================
     *
     * Example:
     *
     * Product = OPPO A5
     *
     * returns:
     *
     * Purchase #1 → Qty 5
     * Purchase #8 → Qty 10
     * Purchase #15 → Qty 3
     *
     * Useful for purchase history.
     *
     * =========================================================
     */

    List<PurchaseItem> findByProduct(
            Product product
    );


    /*
     * =========================================================
     * GET PURCHASE RECORDS BY PRODUCT ID
     * =========================================================
     *
     * Useful when Product ID is available.
     *
     * =========================================================
     */

    List<PurchaseItem> findByProductId(
            Long productId
    );


    /*
     * =========================================================
     * GET ITEMS OF PRODUCT INSIDE ONE PURCHASE
     * =========================================================
     *
     * Normally one product should appear only once
     * in one invoice.
     *
     * This method is useful for validation.
     *
     * =========================================================
     */

    List<PurchaseItem> findByPurchaseAndProduct(
            Purchase purchase,
            Product product
    );


    /*
     * =========================================================
     * CHECK PRODUCT INSIDE PURCHASE
     * =========================================================
     *
     * Useful for duplicate product validation.
     *
     * =========================================================
     */

    boolean existsByPurchaseAndProduct(
            Purchase purchase,
            Product product
    );
}