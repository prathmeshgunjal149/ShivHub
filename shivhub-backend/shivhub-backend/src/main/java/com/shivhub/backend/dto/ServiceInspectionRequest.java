package com.shivhub.backend.dto;
import com.shivhub.backend.enums.AfterSalesInspectionFinding;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data
public class ServiceInspectionRequest {
 private AfterSalesInspectionFinding finding;
 @Size(max = 5000) private String sellerDiagnosis;
 @Size(max = 5000) private String internalNotes;
 @Size(max = 5000) private String customerVisibleRemarks;
 private Boolean warrantyEligible;
 @Size(max = 5000) private String rejectionReason;
}
