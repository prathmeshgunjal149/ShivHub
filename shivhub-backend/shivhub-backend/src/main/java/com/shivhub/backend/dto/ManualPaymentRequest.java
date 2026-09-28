package com.shivhub.backend.dto;

import java.math.BigDecimal;
import com.shivhub.backend.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ManualPaymentRequest {
    @NotNull private PaymentMethod paymentMethod;
    @NotNull @DecimalMin(value = "0.01") private BigDecimal amount;
    private String transactionReference;
    private String notes;
}
