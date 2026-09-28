package com.shivhub.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

/** Admin-managed, category-aware form field definition for non-mobile products. */
@Entity
@Table(name = "category_specification_templates", indexes = {
        @Index(name = "idx_spec_template_category", columnList = "category_id,subcategory_id,active,display_order")
})
@Data
public class CategorySpecificationTemplate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "subcategory_id")
    private SubCategory subCategory;
    @Column(name = "specification_key", nullable = false, length = 80)
    private String specificationKey;
    @Column(name = "display_label", nullable = false, length = 120)
    private String displayLabel;
    @Column(name = "input_type", nullable = false, length = 20)
    private String inputType = "TEXT";
    @Column(name = "options_json", columnDefinition = "TEXT")
    private String optionsJson;
    @Column(name = "required_field", nullable = false)
    private boolean requiredField;
    @Column(nullable = false)
    private boolean filterable;
    @Column(name = "variant_enabled", nullable = false)
    private boolean variantEnabled;
    @Column(name = "display_order", nullable = false)
    private int displayOrder;
    @Column(nullable = false)
    private boolean active = true;
}
