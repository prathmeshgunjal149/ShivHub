package com.shivhub.backend.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.shivhub.backend.dto.CategorySpecificationTemplateRequest;
import com.shivhub.backend.dto.CategorySpecificationTemplateResponse;
import com.shivhub.backend.service.CategorySpecificationTemplateService;

@RestController
@RequiredArgsConstructor
public class CategorySpecificationTemplateController {
    private final CategorySpecificationTemplateService service;
    @GetMapping("/api/seller/product-specification-templates") @PreAuthorize("hasRole('SELLER')")
    public List<CategorySpecificationTemplateResponse> seller(@RequestParam Long categoryId, @RequestParam(required=false) Long subCategoryId) { return service.sellerTemplates(categoryId,subCategoryId); }
    @GetMapping("/api/admin/product-specification-templates") @PreAuthorize("hasRole('ADMIN')")
    public List<CategorySpecificationTemplateResponse> all() { return service.all(); }
    @PostMapping("/api/admin/product-specification-templates") @PreAuthorize("hasRole('ADMIN')")
    public CategorySpecificationTemplateResponse create(@Valid @RequestBody CategorySpecificationTemplateRequest request) { return service.save(null,request); }
    @PutMapping("/api/admin/product-specification-templates/{id}") @PreAuthorize("hasRole('ADMIN')")
    public CategorySpecificationTemplateResponse update(@PathVariable Long id,@Valid @RequestBody CategorySpecificationTemplateRequest request) { return service.save(id,request); }
    @DeleteMapping("/api/admin/product-specification-templates/{id}") @PreAuthorize("hasRole('ADMIN')")
    public void deactivate(@PathVariable Long id) { service.deactivate(id); }
}
