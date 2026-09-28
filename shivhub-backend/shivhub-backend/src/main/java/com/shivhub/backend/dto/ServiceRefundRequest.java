package com.shivhub.backend.dto;
import java.math.BigDecimal;
import com.shivhub.backend.enums.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data public class ServiceRefundRequest { @NotNull @DecimalMin(value="0.01") private BigDecimal refundAmount; @NotNull private PaymentMethod refundMethod; private Long paymentTransactionId; @Size(max=120) private String transactionReference; }
