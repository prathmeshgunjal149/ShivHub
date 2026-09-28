package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.util.List;
import com.shivhub.backend.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class PaymentSummaryResponse {
    private BigDecimal billTotal;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private BigDecimal changeAmount;
    private PaymentStatus paymentStatus;
    private List<PaymentTransactionResponse> payments;
}
