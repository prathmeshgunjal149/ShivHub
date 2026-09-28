package com.shivhub.backend.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.shivhub.backend.entity.Order;
import com.shivhub.backend.enums.OrderStatus;
import com.shivhub.backend.enums.PaymentStatus;


/*
 * =========================================================
 * OrderRepository
 * =========================================================
 *
 * Handles database operations for:
 *
 *     orders
 *
 * table.
 *
 * =========================================================
 */

@Repository
public interface OrderRepository
        extends JpaRepository<Order, Long> {


    /*
     * =========================================================
     * FIND ORDER BY ORDER NUMBER
     * =========================================================
     */

    Optional<Order> findByOrderNumber(
            String orderNumber
    );


    /*
     * =========================================================
     * CHECK ORDER NUMBER
     * =========================================================
     */

    boolean existsByOrderNumber(
            String orderNumber
    );


    /*
     * =========================================================
     * FIND CUSTOMER ORDERS
     * =========================================================
     *
     * Newest orders first.
     *
     * =========================================================
     */

    List<Order>
            findByCustomerIdOrderByCreatedAtDesc(
                    Long customerId
            );


    /*
     * =========================================================
     * FIND ALL ORDERS
     * =========================================================
     *
     * Admin order page.
     *
     * Newest orders first.
     *
     * =========================================================
     */

    List<Order>
            findAllByOrderByCreatedAtDesc();


    /*
     * =========================================================
     * FIND ORDERS BY STATUS
     * =========================================================
     *
     * Example:
     *
     * PENDING
     * CONFIRMED
     * PROCESSING
     * SHIPPED
     * OUT_FOR_DELIVERY
     * DELIVERED
     * CANCELLED
     *
     * =========================================================
     */

    List<Order>
            findByOrderStatusOrderByCreatedAtDesc(
                    OrderStatus orderStatus
            );


    /*
     * =========================================================
     * FIND ORDERS BY PAYMENT STATUS
     * =========================================================
     */

    List<Order>
            findByPaymentStatusOrderByCreatedAtDesc(
                    PaymentStatus paymentStatus
            );


    /*
     * =========================================================
     * COUNT ORDERS BY STATUS
     * =========================================================
     */

    long countByOrderStatus(
            OrderStatus orderStatus
    );


    /*
     * =========================================================
     * COUNT ORDERS BY PAYMENT STATUS
     * =========================================================
     */

    long countByPaymentStatus(
            PaymentStatus paymentStatus
    );


    /*
     * =========================================================
     * COUNT CUSTOMER ORDERS
     * =========================================================
     */

    long countByCustomerId(
            Long customerId
    );

    long countByOrderStatusNot(
            OrderStatus orderStatus
    );

    @Query("""
            select coalesce(sum(o.grandTotal), 0)
            from Order o
            where o.orderStatus <> com.shivhub.backend.enums.OrderStatus.CANCELLED
            """)
    BigDecimal sumRevenueExcludingCancelled();
}
