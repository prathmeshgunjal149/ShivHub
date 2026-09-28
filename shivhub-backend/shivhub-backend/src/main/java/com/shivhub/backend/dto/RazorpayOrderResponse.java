package com.shivhub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class RazorpayOrderResponse {
    private Long internalOrderId;
    private Long offlineBillId;
    private String razorpayOrderId;
    private long amount;
    private String currency;
    private String keyId;
    private String customerName;
    private String customerEmail;
    private String customerMobile;
    private String description;
}
