package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImeiHistoryResponse {

    // =========================
    // IMEI / SERIAL
    // =========================

    private Long serialId;

    private String imei1;

    private String imei2;

    private String serialNumber;


    // =========================
    // PRODUCT
    // =========================

    private Long productId;

    private String productName;

    private String brand;


    // =========================
    // PURCHASE
    // =========================

    private Long purchaseId;

    private String purchaseInvoiceNumber;

    private Long distributorId;

    private String distributorName;

    private LocalDateTime purchaseDate;


    // =========================
    // SALE STATUS
    // =========================

    private String saleStatus;


    // =========================
    // SALE BILL
    // =========================

    private Long saleBillId;

    private String saleBillNumber;

    private LocalDateTime saleDate;


    // =========================
    // CUSTOMER
    // =========================

    private Long customerId;

    private String customerName;

    private String customerMobile;

    private String customerEmail;


    // =========================
    // SALE PRICE
    // =========================

    private BigDecimal sellingPrice;

    private BigDecimal taxableAmount;

    private BigDecimal cgst;

    private BigDecimal sgst;

    private BigDecimal igst;

    private BigDecimal totalPrice;
}