package com.shivhub.backend.dto;

import com.shivhub.backend.enums.OrderStatus;
import com.shivhub.backend.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class RazorpayVerificationResponse {
    private boolean success;
    private PaymentStatus paymentStatus;
    private OrderStatus orderStatus;
    private String invoiceNumber;
}
