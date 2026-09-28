package com.shivhub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellerDistributorResponse {

    private Long id;

    private Long sellerId;
    private String sellerName;
    private String sellerEmail;

    private Long distributorId;
    private String distributorName;
    private String distributorGstin;
    private String distributorMobile;
    private String distributorEmail;
    private String distributorAddress;
    private String distributorCity;
    private String distributorDistrict;
    private String distributorState;
    private String distributorPincode;
    private String distributorWhatsapp;
    private String distributorAlternateMobile;
    private String distributorAccountsEmail;
    private String productCategories;
    private String distributorType;
    private String salespersonName;
    private String salespersonMobile;
    private String claimsContact;
    private String deliveryTerms;
    private String usualDeliveryTime;

    private String brand;
    private String assignedBrands;
    private String paymentTerms;
    private Integer creditPeriodDays;
    private BigDecimal creditLimit;
    private BigDecimal openingBalance;
    private LocalDate openingBalanceDate;
    private String openingBalanceType;
    private String preferredPaymentMethod;
    private String assignedSalesperson;
    private String sellerNotes;

    private Boolean active;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
