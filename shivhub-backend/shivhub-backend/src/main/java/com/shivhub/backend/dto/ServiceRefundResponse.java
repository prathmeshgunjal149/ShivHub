package com.shivhub.backend.dto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.shivhub.backend.enums.PaymentMethod;
import com.shivhub.backend.enums.ServiceRefundStatus;
import lombok.Data;
@Data public class ServiceRefundResponse { private Long id; private BigDecimal refundAmount; private PaymentMethod refundMethod; private ServiceRefundStatus refundStatus; private String transactionReference; private LocalDateTime initiatedAt; private LocalDateTime completedAt; }
