package com.shivhub.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import com.shivhub.backend.entity.Category;

public interface CategoryRepository
        extends JpaRepository<Category, Long> {

    List<Category> findByActiveTrue();

    Optional<Category> findByNameIgnoreCase(String name);

    List<Category> findByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(String query, Pageable pageable);

    List<Category> findByNameContainingIgnoreCaseOrderByNameAsc(String query, Pageable pageable);
}
