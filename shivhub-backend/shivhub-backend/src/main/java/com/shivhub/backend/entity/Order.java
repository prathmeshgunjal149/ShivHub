package com.shivhub.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.shivhub.backend.enums.OrderStatus;
import com.shivhub.backend.enums.PaymentMethod;
import com.shivhub.backend.enums.PaymentStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/*
 * =========================================================
 * Order Entity
 * =========================================================
 *
 * Represents a customer order.
 *
 *
 * One Order can contain multiple OrderItems.
 *
 *
 * Example:
 *
 * Order #1001
 *
 *    ├── iPhone 15       x1
 *    ├── Boat Earbuds    x2
 *    └── Mobile Cover    x1
 *
 *
 * =========================================================
 */

@Entity
@Table(name = "orders")

@Data
@NoArgsConstructor
@AllArgsConstructor

public class Order {


    /*
     * =========================================================
     * PRIMARY KEY
     * =========================================================
     */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    /*
     * =========================================================
     * ORDER NUMBER
     * =========================================================
     *
     * Customer-facing order number.
     *
     * Example:
     *
     * SH-1756200000000-A1B2C3
     *
     */

    @Column(
        name = "order_number",
        nullable = false,
        unique = true
    )
    private String orderNumber;


    /*
     * =========================================================
     * CUSTOMER ID
     * =========================================================
     *
     * Customer is identified using User ID.
     *
     */

    @Column(
        name = "customer_id",
        nullable = false
    )
    private Long customerId;


    /*
     * =========================================================
     * SUBTOTAL
     * =========================================================
     */

    @Column(
        name = "subtotal",
        precision = 12,
        scale = 2,
        nullable = false
    )
    private BigDecimal subtotal = BigDecimal.ZERO;


    /*
     * =========================================================
     * DISCOUNT
     * =========================================================
     */

    @Column(
        name = "discount",
        precision = 12,
        scale = 2,
        nullable = false
    )
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "coupon_code", length = 80)
    private String couponCode;

    @Column(name = "coupon_discount", precision = 12, scale = 2)
    private BigDecimal couponDiscount = BigDecimal.ZERO;

    /** Immutable record of the customer reward redemption used to price this order. */
    @Column(name = "loyalty_points_redeemed", nullable = false)
    private long loyaltyPointsRedeemed = 0;

    @Column(name = "loyalty_discount", precision = 12, scale = 2, nullable = false)
    private BigDecimal loyaltyDiscount = BigDecimal.ZERO;


    /*
     * =========================================================
     * TAX
     * =========================================================
     */

    @Column(
        name = "tax",
        precision = 12,
        scale = 2,
        nullable = false
    )
    private BigDecimal tax = BigDecimal.ZERO;


    /*
     * =========================================================
     * DELIVERY CHARGE
     * =========================================================
     */

    @Column(
        name = "delivery_charge",
        precision = 12,
        scale = 2,
        nullable = false
    )
    private BigDecimal deliveryCharge = BigDecimal.ZERO;


    /*
     * =========================================================
     * GRAND TOTAL
     * =========================================================
     */

    @Column(
        name = "grand_total",
        precision = 12,
        scale = 2,
        nullable = false
    )
    private BigDecimal grandTotal = BigDecimal.ZERO;


    /*
     * =========================================================
     * ORDER STATUS
     * =========================================================
     *
     * PENDING
     * CONFIRMED
     * PROCESSING
     * SHIPPED
     * OUT_FOR_DELIVERY
     * DELIVERED
     * CANCELLED
     *
     */

    @Enumerated(EnumType.STRING)
    @Column(
        name = "order_status",
        nullable = false,
        columnDefinition = "varchar(30)"
    )
    private OrderStatus orderStatus =
            OrderStatus.PENDING;

    @Column(name = "stock_deducted_at")
    private LocalDateTime stockDeductedAt;

    /* Seller proposes a delivery-state change; admin approval applies the actual status. */
    @Enumerated(EnumType.STRING)
    @Column(name = "seller_requested_status", columnDefinition = "varchar(30)")
    private OrderStatus sellerRequestedStatus;

    @Column(name = "seller_status_requested_at")
    private LocalDateTime sellerStatusRequestedAt;

    @Column(name = "delivery_person_name", length = 120)
    private String deliveryPersonName;

    @Column(name = "delivery_person_mobile", length = 15)
    private String deliveryPersonMobile;


    /*
     * =========================================================
     * PAYMENT STATUS
     * =========================================================
     */

    @Enumerated(EnumType.STRING)
    @Column(
        name = "payment_status",
        nullable = false,
        columnDefinition = "varchar(30)"
    )
    private PaymentStatus paymentStatus =
            PaymentStatus.PENDING;

    /* Stored as a preference only. Online gateway capture is intentionally separate. */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30)
    private PaymentMethod paymentMethod = PaymentMethod.CASH;


    /*
     * =========================================================
     * SHIPPING ADDRESS
     * =========================================================
     */

    @Column(
        name = "shipping_address",
        columnDefinition = "TEXT"
    )
    private String shippingAddress;

    @Column(length = 120) private String deliveryName;
    @Column(length = 10) private String deliveryMobile;
    @Column(length = 255) private String deliveryAddressLine1;
    @Column(length = 255) private String deliveryAddressLine2;
    @Column(length = 255) private String deliveryLandmark;
    @Column(length = 120) private String deliveryCity;
    @Column(length = 120) private String deliveryDistrict;
    @Column(length = 120) private String deliveryState;
    @Column(length = 6) private String deliveryPincode;


    /*
     * =========================================================
     * ORDER ITEMS
     * =========================================================
     *
     * One Order → Many OrderItems.
     *
     */

    @OneToMany(
        mappedBy = "order",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<OrderItem> items =
            new ArrayList<>();


    /*
     * =========================================================
     * CANCELLATION REASON
     * =========================================================
     *
     * Customer must select a reason when cancelling.
     *
     * Examples:
     *
     * I ordered by mistake
     * Changed my mind
     * Found a better price elsewhere
     *
     */

    @Column(
        name = "cancellation_reason",
        length = 255
    )
    private String cancellationReason;


    /*
     * =========================================================
     * CANCELLATION COMMENT
     * =========================================================
     *
     * Optional additional explanation from customer.
     *
     */

    @Column(
        name = "cancellation_comment",
        columnDefinition = "TEXT"
    )
    private String cancellationComment;


    /*
     * =========================================================
     * CANCELLED BY
     * =========================================================
     *
     * CUSTOMER
     * ADMIN
     *
     */

    @Column(
        name = "cancelled_by",
        length = 50
    )
    private String cancelledBy;


    /*
     * =========================================================
     * CANCELLED AT
     * =========================================================
     */

    @Column(
        name = "cancelled_at"
    )
    private LocalDateTime cancelledAt;


    /*
     * =========================================================
     * CREATED AT
     * =========================================================
     */

    @Column(
        name = "created_at",
        nullable = false,
        updatable = false
    )
    private LocalDateTime createdAt;


    /*
     * =========================================================
     * UPDATED AT
     * =========================================================
     */

    @Column(
        name = "updated_at"
    )
    private LocalDateTime updatedAt;


    /*
     * =========================================================
     * PRE-PERSIST
     * =========================================================
     */

    @PrePersist
    protected void onCreate() {

        LocalDateTime now =
                LocalDateTime.now();

        createdAt = now;
        updatedAt = now;
    }


    /*
     * =========================================================
     * PRE-UPDATE
     * =========================================================
     */

    @PreUpdate
    protected void onUpdate() {

        updatedAt =
                LocalDateTime.now();
    }


    /*
     * =========================================================
     * ADD ORDER ITEM
     * =========================================================
     */

    public void addItem(OrderItem item) {

        items.add(item);

        item.setOrder(this);
    }


    /*
     * =========================================================
     * REMOVE ORDER ITEM
     * =========================================================
     */

    public void removeItem(OrderItem item) {

        items.remove(item);

        item.setOrder(null);
    }
    
}
