package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.shivhub.backend.enums.PaymentContext;
import com.shivhub.backend.enums.PaymentMethod;
import com.shivhub.backend.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

/** Safe payment-history projection: never exposes gateway signatures or secrets. */
@Data @AllArgsConstructor
public class PaymentTransactionResponse {
    private Long id;
    private String paymentReference;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private PaymentContext paymentContext;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private BigDecimal amount;
    private String currency;
    private String transactionReference;
    private String notes;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
