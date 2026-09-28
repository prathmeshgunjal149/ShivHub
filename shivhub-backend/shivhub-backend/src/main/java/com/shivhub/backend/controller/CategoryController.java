package com.shivhub.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shivhub.backend.entity.Category;
import com.shivhub.backend.entity.SubCategory;
import com.shivhub.backend.service.CategoryService;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "http://localhost:5173")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(
            CategoryService categoryService) {

        this.categoryService = categoryService;
    }


    /*
     * GET ALL CATEGORIES
     *
     * GET /api/categories
     */

    @GetMapping
    public ResponseEntity<List<Category>> getCategories() {

        return ResponseEntity.ok(
                categoryService.getCategories()
        );
    }


    /*
     * GET SUBCATEGORIES
     *
     * GET /api/categories/{categoryId}/subcategories
     */

    @GetMapping("/{categoryId}/subcategories")
    public ResponseEntity<List<SubCategory>>
    getSubCategories(
            @PathVariable Long categoryId) {

        return ResponseEntity.ok(
                categoryService
                        .getSubCategories(categoryId)
        );
    }
}