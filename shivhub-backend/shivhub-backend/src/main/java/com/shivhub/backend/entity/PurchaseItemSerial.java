package com.shivhub.backend.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/*
 * =========================================================
 * PURCHASE ITEM SERIAL
 * =========================================================
 *
 * Stores individual physical-unit information.
 *
 * Mainly used for:
 *
 * Mobile
 * Laptop
 * TV
 * Refrigerator
 * Washing Machine
 * Other serialized products
 *
 *
 * Mobile example:
 *
 * Product = Samsung S26 Ultra
 * Quantity = 2
 *
 *      IMEI 1 -> 123456789012345 -> AVAILABLE
 *      IMEI 2 -> 987654321098765 -> AVAILABLE
 *
 *
 * After sale:
 *
 *      IMEI 1 -> 123456789012345 -> SOLD
 *      IMEI 2 -> 987654321098765 -> AVAILABLE
 *
 * =========================================================
 */

@Entity
@Table(name = "purchase_item_serials")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseItemSerial {


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
     * PURCHASE ITEM
     * =========================================================
     *
     * Many serial records belong to one PurchaseItem.
     *
     * PurchaseItem
     *      |
     *      |---- IMEI 1
     *      |---- IMEI 2
     *      |---- IMEI 3
     *
     * =========================================================
     */

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "purchase_item_id",
            nullable = false
    )
    private PurchaseItem purchaseItem;


    /*
     * =========================================================
     * IMEI 1
     * =========================================================
     */

    @Column(
            name = "imei1",
            length = 30
    )
    private String imei1;


    /*
     * =========================================================
     * IMEI 2
     * =========================================================
     *
     * Optional for dual-SIM phones.
     *
     * =========================================================
     */

    @Column(
            name = "imei2",
            length = 30
    )
    private String imei2;


    /*
     * =========================================================
     * SERIAL NUMBER
     * =========================================================
     *
     * Used for:
     *
     * TV
     * Laptop
     * Refrigerator
     * Washing Machine
     * Speaker
     * Other electronics
     *
     * =========================================================
     */

    @Column(
            name = "serial_number",
            length = 100
    )
    private String serialNumber;


    /*
     * =========================================================
     * UNIT STATUS
     * =========================================================
     *
     * AVAILABLE
     *      Physical unit is currently in shop stock.
     *
     * SOLD
     *      Physical unit has been sold to customer.
     *
     * RETURNED
     *      Physical unit was returned.
     *
     * =========================================================
     */

    @Column(
            name = "status",
            length = 20,
            nullable = false
    )
    private String status = "AVAILABLE";


    /*
     * =========================================================
     * SOLD OFFLINE BILL ITEM
     * =========================================================
     *
     * When this physical unit is sold through ShivHub POS,
     * this field points to the exact bill item.
     *
     * Example:
     *
     * IMEI
     *    |
     *    ↓
     * OfflineBillItem
     *    |
     *    ↓
     * Customer Bill
     *
     * =========================================================
     */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "sold_offline_bill_item_id"
    )
    @JsonIgnore
    private OfflineBillItem soldOfflineBillItem;

    /** A POS Razorpay bill may reserve, but must not sell, a serial until paid. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserved_offline_bill_item_id")
    @JsonIgnore
    private OfflineBillItem reservedOfflineBillItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserved_order_item_id")
    @JsonIgnore
    private OrderItem reservedOrderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sold_order_item_id")
    @JsonIgnore
    private OrderItem soldOrderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_adjustment_id")
    @JsonIgnore
    private StockTransferAdjustment transferAdjustment;


    /*
     * =========================================================
     * SOLD AT
     * =========================================================
     *
     * Stores the exact date/time when this physical unit
     * was sold.
     *
     * =========================================================
     */

    @Column(
            name = "sold_at"
    )
    private LocalDateTime soldAt;


    /*
     * =========================================================
     * STATUS HELPERS
     * =========================================================
     */

    public boolean isAvailable() {

        return "AVAILABLE".equalsIgnoreCase(status);
    }


    public boolean isSold() {

        return "SOLD".equalsIgnoreCase(status);
    }

    public boolean isReserved() {

        return "RESERVED".equalsIgnoreCase(status);
    }


    public boolean isReturned() {

        return "RETURNED".equalsIgnoreCase(status);
    }
}
