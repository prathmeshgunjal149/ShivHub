package com.shivhub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/*
 * =========================================================
 * PURCHASE ITEM SERIAL REQUEST
 * =========================================================
 *
 * Represents IMEI / Serial information for one
 * physical product unit.
 *
 * =========================================================
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseItemSerialRequest {


    /*
     * =========================================================
     * IMEI 1
     * =========================================================
     */

    private String imei1;


    /*
     * =========================================================
     * IMEI 2
     * =========================================================
     *
     * Used for dual-SIM devices.
     *
     * =========================================================
     */

    private String imei2;


    /*
     * =========================================================
     * SERIAL NUMBER
     * =========================================================
     *
     * Used for TV, Laptop, Refrigerator,
     * Washing Machine, Speaker etc.
     *
     * =========================================================
     */

    private String serialNumber;
}