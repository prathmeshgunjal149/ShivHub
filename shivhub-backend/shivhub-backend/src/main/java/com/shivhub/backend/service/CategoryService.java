package com.shivhub.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shivhub.backend.entity.Category;
import com.shivhub.backend.entity.SubCategory;
import com.shivhub.backend.repository.CategoryRepository;
import com.shivhub.backend.repository.SubCategoryRepository;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    private final SubCategoryRepository subCategoryRepository;

    public CategoryService(
            CategoryRepository categoryRepository,
            SubCategoryRepository subCategoryRepository) {

        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
    }


    /*
     * Get all active categories
     */

    @Transactional(readOnly = true)
    public List<Category> getCategories() {

        return categoryRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public List<Category> getAllCategories() { return categoryRepository.findAll(); }

    @Transactional
    public Category createCategory(String name) {
        String cleaned = cleanName(name);
        if (categoryRepository.findByNameIgnoreCase(cleaned).isPresent()) {
            throw new IllegalArgumentException("A category with this name already exists");
        }
        Category category = new Category();
        category.setName(cleaned);
        category.setActive(true);
        return categoryRepository.save(category);
    }

    @Transactional
    public Category updateCategory(Long id, String name, Boolean active) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        if (name != null) {
            String cleaned = cleanName(name);
            categoryRepository.findByNameIgnoreCase(cleaned)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> { throw new IllegalArgumentException("A category with this name already exists"); });
            category.setName(cleaned);
        }
        if (active != null) category.setActive(active);
        return categoryRepository.save(category);
    }

    private String cleanName(String name) {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Category name is required");
        return name.trim();
    }


    /*
     * Get subcategories of category
     */

    @Transactional(readOnly = true)
    public List<SubCategory> getSubCategories(
            Long categoryId) {

        Category category =
                categoryRepository.findById(categoryId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Category not found"
                                )
                        );

        return subCategoryRepository
                .findByCategoryAndActiveTrue(category);
    }
}
