package com.shivhub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RazorpayPaymentLinkResponse {
    private Long offlineBillId;
    private String paymentLinkId;
    private String paymentLinkUrl;
    private long amount;
    private String currency;
    private String description;
}
