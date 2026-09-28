package com.shivhub.backend.repository;

import java.util.List;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shivhub.backend.entity.OfflineBillItem;


/*
 * =========================================================
 * OfflineBillItemRepository
 * =========================================================
 *
 * Database operations for OfflineBillItem.
 *
 * Used by:
 *
 * 1. Offline Billing / POS
 * 2. Bill details
 * 3. Product-wise sales
 * 4. Reports
 *
 * =========================================================
 */

public interface OfflineBillItemRepository
        extends JpaRepository<OfflineBillItem, Long> {


    /*
     * =========================================================
     * FIND ITEMS BY BILL
     * =========================================================
     *
     * Returns all products belonging to one offline bill.
     *
     */

    List<OfflineBillItem> findByOfflineBillId(
            Long offlineBillId
    );


    /*
     * =========================================================
     * FIND ITEMS BY PRODUCT
     * =========================================================
     *
     * Useful for:
     *
     * Product-wise sales report
     * Product purchase history
     *
     */

    List<OfflineBillItem> findByProductId(
            Long productId
    );


    /*
     * =========================================================
     * FIND PRODUCT ITEMS ORDERED BY BILL
     * =========================================================
     *
     * Newest bills first.
     *
     */

    List<OfflineBillItem> findByProductIdOrderByIdDesc(
            Long productId
    );


    /*
     * =========================================================
     * COUNT ITEMS BY PRODUCT
     * =========================================================
     *
     * Number of bill entries for a product.
     *
     */

    long countByProductId(
            Long productId
    );

    @Query("""
            select item from OfflineBillItem item join fetch item.offlineBill bill
            where bill.sellerId = :sellerId
              and bill.createdAt >= :start and bill.createdAt < :end
            """)
    List<OfflineBillItem> findSellerReportItemsBetween(
            @Param("sellerId") Long sellerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
