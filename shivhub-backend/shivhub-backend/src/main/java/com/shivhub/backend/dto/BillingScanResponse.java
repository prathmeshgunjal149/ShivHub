package com.shivhub.backend.dto;

import java.math.BigDecimal;
import com.shivhub.backend.enums.BillingScanType;
import lombok.Data;

@Data
public class BillingScanResponse {
    private Long variantId;
    private String selectedAttributes;
    private BillingScanType scanType;
    private String scannedCode;
    private Long productId;
    private String productName;
    private String brand;
    private String model;
    private String color;
    private String ram;
    private String storage;
    private String sku;
    private String barcode;
    private String hsnCode;
    private BigDecimal sellingPrice;
    private BigDecimal gstRate;
    private BigDecimal cgstRate;
    private BigDecimal sgstRate;
    private Integer stockAvailable;
    private boolean imeiTracked;
    private Long serialId;
    private String imei1;
    private String imei2;
    private String serialNumber;
    private String imageUrl;
    private String warrantyDetails;
    private String message;
}
