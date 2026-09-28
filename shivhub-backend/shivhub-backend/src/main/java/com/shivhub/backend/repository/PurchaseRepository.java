package com.shivhub.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.shivhub.backend.entity.Purchase;
import com.shivhub.backend.entity.SellerDistributor;
import com.shivhub.backend.entity.User;


/*
 * =========================================================
 * PURCHASE REPOSITORY
 * =========================================================
 *
 * Handles database operations for:
 *
 *                 purchases
 *
 * Purchase belongs to:
 *
 * Seller
 *    +
 * Distributor
 *
 * Example:
 *
 * Sunny Mobile
 *      ↓
 * OPPO Distributor
 *      ↓
 * Purchase Invoice
 *
 * =========================================================
 */

@Repository
public interface PurchaseRepository
        extends JpaRepository<Purchase, Long> {


    /*
     * =========================================================
     * GET ALL PURCHASES OF A SELLER
     * =========================================================
     *
     * Important:
     *
     * Seller should see ONLY his own purchases.
     *
     * Example:
     *
     * Seller ID = 5
     *
     * returns:
     *
     * Purchase 1 → Seller 5
     * Purchase 2 → Seller 5
     *
     * but NOT:
     *
     * Purchase 3 → Seller 8
     *
     * =========================================================
     */

    List<Purchase> findBySellerOrderByPurchaseDateDesc(
            User seller
    );


    /*
     * =========================================================
     * GET PURCHASES BY SELLER ID
     * =========================================================
     *
     * Useful when we have seller ID from JWT.
     *
     * =========================================================
     */

    List<Purchase> findBySellerIdOrderByPurchaseDateDesc(
            Long sellerId
    );


    /*
     * =========================================================
     * GET PURCHASES FROM ONE DISTRIBUTOR
     * =========================================================
     *
     * Example:
     *
     * OPPO Distributor
     *       ↓
     * Purchase 1
     * Purchase 2
     * Purchase 3
     *
     * =========================================================
     */

    List<Purchase> findByDistributorOrderByPurchaseDateDesc(
            SellerDistributor distributor
    );

    List<Purchase> findByDistributorInOrderByPurchaseDateDesc(
            List<SellerDistributor> distributors
    );


    /*
     * =========================================================
     * GET PURCHASES OF SELLER FROM ONE DISTRIBUTOR
     * =========================================================
     *
     * This is very important for ShivHub.
     *
     * Same seller can have:
     *
     * OPPO Distributor
     * VIVO Distributor
     * Samsung Distributor
     *
     * We need to make sure the seller sees only
     * his own distributor purchases.
     *
     * =========================================================
     */

    List<Purchase> findBySellerAndDistributorOrderByPurchaseDateDesc(
            User seller,
            SellerDistributor distributor
    );


    /*
     * =========================================================
     * FIND INVOICE BY NUMBER
     * =========================================================
     *
     * Used to prevent duplicate distributor invoices
     * for the same seller.
     *
     * Example:
     *
     * Seller 5
     * Invoice: OPPO-INV-001
     *
     * If seller tries to add the same invoice again,
     * we can reject it.
     *
     * =========================================================
     */

    Optional<Purchase> findBySellerAndInvoiceNumber(
            User seller,
            String invoiceNumber
    );


    /*
     * =========================================================
     * CHECK DUPLICATE INVOICE
     * =========================================================
     *
     * Useful before creating a purchase.
     *
     * =========================================================
     */

    boolean existsBySellerAndInvoiceNumber(
            User seller,
            String invoiceNumber
    );


    /*
     * =========================================================
     * COUNT SELLER PURCHASES
     * =========================================================
     *
     * Useful for Seller Dashboard.
     *
     * =========================================================
     */

    long countBySeller(
            User seller
    );

    /** Purchases whose received products still require catalogue approval. */
    @Query("""
            select distinct purchase from Purchase purchase
            join fetch purchase.seller seller
            join fetch purchase.distributor sellerDistributor
            join fetch sellerDistributor.distributor distributor
            join fetch purchase.items item
            join fetch item.product product
            where purchase.status = com.shivhub.backend.enums.PurchaseStatus.COMPLETED
              and product.approvalStatus = :status
            order by purchase.createdAt desc
            """)
    List<Purchase> findForProductApproval(@Param("status") com.shivhub.backend.enums.ProductStatus status);

    @Query("""
            select (count(purchase) > 0) from Purchase purchase
            join purchase.items item
            where item.product.id = :productId
              and purchase.status = com.shivhub.backend.enums.PurchaseStatus.COMPLETED
            """)
    boolean hasCompletedPurchaseForProduct(@Param("productId") Long productId);
}
