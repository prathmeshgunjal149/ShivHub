package com.shivhub.backend.dto;
import java.math.BigDecimal; import java.time.LocalDate; import lombok.Data;
@Data public class DistributorCreditNoteRequest { private Long purchaseId; private String creditNoteNumber, attachmentUrl, reason, remarks; private LocalDate creditNoteDate; private BigDecimal amount, gstAmount; }
