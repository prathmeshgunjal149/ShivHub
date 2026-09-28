package com.shivhub.backend.repository;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shivhub.backend.entity.OrderItem;


/*
 * =========================================================
 * OrderItemRepository
 * =========================================================
 *
 * Handles database operations for order_items table.
 *
 * =========================================================
 */

public interface OrderItemRepository
        extends JpaRepository<OrderItem, Long> {


    /*
     * =========================================================
     * FIND ITEMS BY ORDER
     * =========================================================
     *
     * Returns all products belonging to an order.
     *
     * Example:
     *
     * Order #SH1001
     *
     *    Product A
     *    Product B
     *    Product C
     *
     */

    List<OrderItem> findByOrderId(
            Long orderId
    );


    /*
     * =========================================================
     * FIND ITEMS BY PRODUCT
     * =========================================================
     *
     * Useful later for:
     *
     * Product sales analytics
     * Top selling products
     * Product reports
     *
     */

    List<OrderItem> findByProductId(
            Long productId
    );


    /* Seller ID is saved as a sale-time snapshot, so reports remain correct if a profile changes. */
    @Query("""
            select coalesce(sum(item.totalPrice), 0)
            from OrderItem item
            where item.sellerId = :sellerId
              and item.order.createdAt between :start and :end
              and item.order.orderStatus <> com.shivhub.backend.enums.OrderStatus.CANCELLED
            """)
    BigDecimal sumSellerOnlineSalesBetween(
            @Param("sellerId") Long sellerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
            select count(distinct item.order.id)
            from OrderItem item
            where item.sellerId = :sellerId
              and item.order.createdAt between :start and :end
              and item.order.orderStatus <> com.shivhub.backend.enums.OrderStatus.CANCELLED
            """)
    long countSellerOnlineOrdersBetween(
            @Param("sellerId") Long sellerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    /** Seller-facing order queue; only that seller's line-items are returned. */
    @Query("""
            select item from OrderItem item join fetch item.order
            where item.sellerId = :sellerId
            order by item.order.createdAt desc
            """)
    List<OrderItem> findSellerOrderItems(@Param("sellerId") Long sellerId);

    /** Read-only source for seller sales reports, bounded by the requested date range. */
    @Query("""
            select item from OrderItem item join fetch item.order
            where item.sellerId = :sellerId
              and item.order.createdAt >= :start
              and item.order.createdAt < :end
              and item.order.orderStatus <> com.shivhub.backend.enums.OrderStatus.CANCELLED
            order by item.order.createdAt desc
            """)
    List<OrderItem> findSellerReportItemsBetween(
            @Param("sellerId") Long sellerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    /**
     * Recommendation evidence source.
     * Qualifying statuses: SHIPPED, OUT_FOR_DELIVERY and DELIVERED.
     * Cancelled/pending/processing orders are intentionally excluded.
     */
    @Query("""
            select item from OrderItem item join fetch item.order
            where item.productId = :productId
              and item.order.orderStatus in :statuses
            order by item.order.createdAt desc
            """)
    List<OrderItem> findQualifyingRecommendationItemsByProductId(
            @Param("productId") Long productId,
            @Param("statuses") List<com.shivhub.backend.enums.OrderStatus> statuses
    );
}
