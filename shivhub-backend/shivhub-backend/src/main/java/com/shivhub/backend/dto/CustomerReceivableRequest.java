package com.shivhub.backend.dto;
import java.math.BigDecimal; import java.time.LocalDate; import lombok.Data;
@Data public class CustomerReceivableRequest { private String customerName, customerMobile, customerEmail, invoiceNumber, productDetails, imei, notes; private BigDecimal saleAmount; private LocalDate saleDate, dueDate; }
