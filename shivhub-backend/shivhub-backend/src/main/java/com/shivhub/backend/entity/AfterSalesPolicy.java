package com.shivhub.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

/** Global, category, or product policy. The most specific active policy wins. */
@Entity
@Table(name = "after_sales_policies", indexes = {
        @Index(name = "idx_after_sales_policy_category", columnList = "category_id,active"),
        @Index(name = "idx_after_sales_policy_product", columnList = "product_id,active")
})
@Data
public class AfterSalesPolicy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "category_id") private Long categoryId;
    @Column(name = "product_id") private Long productId;
    @Column(name = "warranty_type", nullable = false, length = 30) private String warrantyType = "NONE";
    @Column(name = "warranty_months", nullable = false) private Integer warrantyMonths = 0;
    @Column(nullable = false) private boolean returnable;
    @Column(name = "return_window_days", nullable = false) private Integer returnWindowDays = 0;
    @Column(name = "replacement_window_days", nullable = false) private Integer replacementWindowDays = 0;
    @Column(name = "doa_window_days", nullable = false) private Integer doaWindowDays = 0;
    @Column(name = "physical_damage_allowed", nullable = false) private boolean physicalDamageAllowed;
    @Column(name = "liquid_damage_allowed", nullable = false) private boolean liquidDamageAllowed;
    @Column(name = "opened_box_return_allowed", nullable = false) private boolean openedBoxReturnAllowed;
    @Column(name = "change_of_mind_allowed", nullable = false) private boolean changeOfMindAllowed;
    @Column(name = "required_evidence", columnDefinition = "TEXT") private String requiredEvidence;
    @Column(name = "policy_terms", columnDefinition = "TEXT") private String policyTerms;
    @Column(nullable = false) private boolean active = true;
    @Column(name = "created_by_user_id") private Long createdByUserId;
    @Column(name = "updated_by_user_id") private Long updatedByUserId;
}
