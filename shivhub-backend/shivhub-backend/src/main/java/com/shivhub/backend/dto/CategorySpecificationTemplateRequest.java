package com.shivhub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CategorySpecificationTemplateRequest(
        @NotNull Long categoryId,
        Long subCategoryId,
        @NotBlank String specificationKey,
        @NotBlank String displayLabel,
        String inputType,
        String optionsJson,
        boolean requiredField,
        boolean filterable,
        boolean variantEnabled,
        Integer displayOrder,
        boolean active
) { }
