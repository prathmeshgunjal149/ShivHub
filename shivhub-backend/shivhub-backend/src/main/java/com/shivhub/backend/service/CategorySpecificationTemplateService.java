package com.shivhub.backend.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.shivhub.backend.dto.CategorySpecificationTemplateRequest;
import com.shivhub.backend.dto.CategorySpecificationTemplateResponse;
import com.shivhub.backend.entity.Category;
import com.shivhub.backend.entity.CategorySpecificationTemplate;
import com.shivhub.backend.entity.SubCategory;
import com.shivhub.backend.repository.CategoryRepository;
import com.shivhub.backend.repository.CategorySpecificationTemplateRepository;
import com.shivhub.backend.repository.SubCategoryRepository;

@Service
public class CategorySpecificationTemplateService {
    private final CategorySpecificationTemplateRepository templates;
    private final CategoryRepository categories;
    private final SubCategoryRepository subCategories;
    public CategorySpecificationTemplateService(CategorySpecificationTemplateRepository templates, CategoryRepository categories, SubCategoryRepository subCategories) { this.templates=templates; this.categories=categories; this.subCategories=subCategories; }

    @Transactional(readOnly = true)
    public List<CategorySpecificationTemplateResponse> sellerTemplates(Long categoryId, Long subCategoryId) {
        Category category = activeCategory(categoryId);
        if (subCategoryId != null) activeSubCategory(category, subCategoryId);
        return templates.findActiveForCategory(category.getId(), subCategoryId).stream().map(this::response).toList();
    }
    /** Customer-safe field metadata. Product values are still limited to approved public listings. */
    @Transactional(readOnly = true)
    public List<CategorySpecificationTemplateResponse> customerFilterFields(Long subCategoryId) {
        SubCategory sub = subCategories.findById(subCategoryId).orElseThrow(() -> new IllegalArgumentException("Subcategory not found"));
        Category category = sub.getCategory();
        if (category == null || !category.isActive() || !sub.isActive() || "mobiles".equalsIgnoreCase(category.getName())) return List.of();
        return templates.findActiveForCategory(category.getId(), sub.getId()).stream()
                .filter(CategorySpecificationTemplate::isFilterable).map(this::response).toList();
    }
    @Transactional(readOnly = true)
    public List<CategorySpecificationTemplateResponse> all() { return templates.findAllByOrderByCategoryIdAscSubCategoryIdAscDisplayOrderAsc().stream().map(this::response).toList(); }
    @Transactional
    public CategorySpecificationTemplateResponse save(Long id, CategorySpecificationTemplateRequest request) {
        Category category = categories.findById(request.categoryId()).orElseThrow(() -> new IllegalArgumentException("Category not found"));
        SubCategory sub = request.subCategoryId() == null ? null : subCategories.findById(request.subCategoryId()).orElseThrow(() -> new IllegalArgumentException("Subcategory not found"));
        if (sub != null && (sub.getCategory() == null || !sub.getCategory().getId().equals(category.getId()))) throw new IllegalArgumentException("Selected subcategory does not belong to selected category");
        CategorySpecificationTemplate template = id == null ? new CategorySpecificationTemplate() : templates.findById(id).orElseThrow(() -> new IllegalArgumentException("Specification template not found"));
        if ("mobiles".equalsIgnoreCase(category.getName())) throw new IllegalArgumentException("Mobile fields are managed by the separate Mobile module");
        template.setCategory(category); template.setSubCategory(sub); template.setSpecificationKey(key(request.specificationKey())); template.setDisplayLabel(request.displayLabel().trim()); template.setInputType(inputType(request.inputType())); template.setOptionsJson(options(request.optionsJson(), template.getInputType())); template.setRequiredField(request.requiredField()); template.setFilterable(request.filterable()); template.setVariantEnabled(request.variantEnabled()); template.setDisplayOrder(request.displayOrder() == null ? 0 : Math.max(0, request.displayOrder())); template.setActive(request.active());
        return response(templates.save(template));
    }
    @Transactional
    public void deactivate(Long id) { CategorySpecificationTemplate template=templates.findById(id).orElseThrow(() -> new IllegalArgumentException("Specification template not found")); template.setActive(false); templates.save(template); }
    private Category activeCategory(Long id) { Category c=categories.findById(id).orElseThrow(() -> new IllegalArgumentException("Category not found")); if(!c.isActive()) throw new IllegalArgumentException("Selected category is inactive"); return c; }
    private SubCategory activeSubCategory(Category category, Long id) { SubCategory s=subCategories.findById(id).orElseThrow(() -> new IllegalArgumentException("Subcategory not found")); if(!s.isActive() || s.getCategory()==null || !s.getCategory().getId().equals(category.getId())) throw new IllegalArgumentException("Selected subcategory is invalid or inactive"); return s; }
    private String key(String value) { String key=value.trim().toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", ""); if(key.isBlank()) throw new IllegalArgumentException("Specification key is invalid"); return key; }
    private String inputType(String value) { String type=value==null?"TEXT":value.trim().toUpperCase(); if(!List.of("TEXT","NUMBER","BOOLEAN","TEXTAREA","SELECT","MULTI_SELECT").contains(type)) throw new IllegalArgumentException("Unsupported specification input type"); return type; }
    private String options(String value, String inputType) { String text=value==null?null:value.trim(); if (("SELECT".equals(inputType)||"MULTI_SELECT".equals(inputType)) && (text==null||text.isBlank())) throw new IllegalArgumentException("Selectable fields require option values"); return text==null||text.isBlank()?null:text; }
    private CategorySpecificationTemplateResponse response(CategorySpecificationTemplate t) { return new CategorySpecificationTemplateResponse(t.getId(),t.getCategory().getId(),t.getSubCategory()==null?null:t.getSubCategory().getId(),t.getSpecificationKey(),t.getDisplayLabel(),t.getInputType(),t.getOptionsJson(),t.isRequiredField(),t.isFilterable(),t.isVariantEnabled(),t.getDisplayOrder(),t.isActive()); }
}
