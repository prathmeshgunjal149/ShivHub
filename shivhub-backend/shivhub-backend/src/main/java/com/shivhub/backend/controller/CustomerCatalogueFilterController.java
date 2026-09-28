package com.shivhub.backend.controller;

import java.util.List;
import com.shivhub.backend.dto.CategorySpecificationTemplateResponse;
import com.shivhub.backend.service.CategorySpecificationTemplateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Returns only public filter labels/options, never seller or inventory information. */
@RestController
public class CustomerCatalogueFilterController {
    private final CategorySpecificationTemplateService templates;
    public CustomerCatalogueFilterController(CategorySpecificationTemplateService templates) { this.templates = templates; }

    @GetMapping("/api/customer/catalogue/subcategories/{subCategoryId}/filters")
    public List<CategorySpecificationTemplateResponse> filters(@PathVariable Long subCategoryId) {
        return templates.customerFilterFields(subCategoryId);
    }
}
