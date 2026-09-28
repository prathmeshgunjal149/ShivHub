package com.shivhub.backend.dto;

public record CategorySpecificationTemplateResponse(
        Long id, Long categoryId, Long subCategoryId, String specificationKey,
        String displayLabel, String inputType, String optionsJson, boolean requiredField,
        boolean filterable, boolean variantEnabled, int displayOrder, boolean active
) { }
