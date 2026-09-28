package com.shivhub.backend.dto;

import java.math.BigDecimal;

import com.shivhub.backend.enums.ShopPaymentMode;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class InstantMobileBillRequest {
    @NotBlank private String customerName;
    @NotBlank @Pattern(regexp = "^[6-9][0-9]{9}$", message = "Enter a valid 10 digit Indian mobile number") private String customerMobile;
    private String customerEmail;
    private String customerAddress;
    private boolean whatsappConsent;
    private String brand;
    @NotBlank private String model;
    private String color;
    private String ram;
    private String storage;
    private String imeiOrSerial;
    @NotNull @Min(1) private Integer quantity = 1;
    @NotNull @DecimalMin("0.01") private BigDecimal unitPrice;
    @DecimalMin("0.00") private BigDecimal discount = BigDecimal.ZERO;
    @NotNull private ShopPaymentMode paymentMode;
    private String paymentReference;
    private String notes;
}
