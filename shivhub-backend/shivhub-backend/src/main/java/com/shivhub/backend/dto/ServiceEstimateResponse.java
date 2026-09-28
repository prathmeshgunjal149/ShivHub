package com.shivhub.backend.dto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.shivhub.backend.enums.EstimateApprovalStatus;
import lombok.Data;
@Data public class ServiceEstimateResponse {
 private BigDecimal inspectionCharge; private BigDecimal partsAmount; private BigDecimal labourAmount; private BigDecimal discount; private BigDecimal taxableAmount;
 private BigDecimal cgst; private BigDecimal sgst; private BigDecimal igst; private BigDecimal grandTotal; private BigDecimal advanceAmount; private BigDecimal remainingAmount;
 private EstimateApprovalStatus customerApprovalStatus; private LocalDateTime approvedAt; private List<ServicePartRequest> parts;
}
