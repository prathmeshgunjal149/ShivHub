package com.shivhub.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shivhub.backend.entity.Category;
import com.shivhub.backend.entity.SubCategory;

public interface SubCategoryRepository
        extends JpaRepository<SubCategory, Long> {

    List<SubCategory> findByCategoryAndActiveTrue(
            Category category
    );

    Optional<SubCategory> findByNameIgnoreCaseAndCategory(
            String name,
            Category category
    );

    @Query("""
            select subCategory from SubCategory subCategory join fetch subCategory.category category
            where subCategory.active = true and category.active = true
              and lower(subCategory.name) like lower(concat('%', :query, '%'))
            order by subCategory.name asc
            """)
    List<SubCategory> searchPublicSuggestions(@Param("query") String query, Pageable pageable);

    @Query("""
            select subCategory from SubCategory subCategory join fetch subCategory.category category
            where lower(subCategory.name) like lower(concat('%', :query, '%'))
            order by subCategory.name asc
            """)
    List<SubCategory> searchAdminSuggestions(@Param("query") String query, Pageable pageable);
}
