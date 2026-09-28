package com.shivhub.backend.dto;
import java.math.BigDecimal; import java.time.LocalDateTime; import com.shivhub.backend.enums.PaymentMethod; import lombok.Data;
@Data public class CustomerPaymentRequest { private BigDecimal amount; private PaymentMethod paymentMethod; private String transactionReference, notes; private LocalDateTime paymentDate; }
