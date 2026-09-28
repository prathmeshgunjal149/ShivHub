package com.shivhub.backend.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class AfterSalesPolicyRequest {
 private Long categoryId; private Long productId;
 @NotBlank @Size(max=30) private String warrantyType;
 @NotNull @Min(0) private Integer warrantyMonths;
 private boolean returnable;
 @NotNull @Min(0) private Integer returnWindowDays;
 @NotNull @Min(0) private Integer replacementWindowDays;
 @NotNull @Min(0) private Integer doaWindowDays;
 private boolean physicalDamageAllowed; private boolean liquidDamageAllowed; private boolean openedBoxReturnAllowed; private boolean changeOfMindAllowed;
 private String requiredEvidence; private String policyTerms; private boolean active = true;
}
