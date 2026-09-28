package com.shivhub.backend.dto;
import lombok.Data;
@Data public class AfterSalesPolicyResponse { private Long id; private Long categoryId; private Long productId; private String warrantyType; private Integer warrantyMonths; private boolean returnable; private Integer returnWindowDays; private Integer replacementWindowDays; private Integer doaWindowDays; private boolean physicalDamageAllowed; private boolean liquidDamageAllowed; private boolean openedBoxReturnAllowed; private boolean changeOfMindAllowed; private String requiredEvidence; private String policyTerms; private boolean active; }
