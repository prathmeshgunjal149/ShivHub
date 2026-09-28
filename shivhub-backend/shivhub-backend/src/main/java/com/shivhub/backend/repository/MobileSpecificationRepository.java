package com.shivhub.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shivhub.backend.entity.MobileSpecification;

public interface MobileSpecificationRepository extends JpaRepository<MobileSpecification, Long> {

    List<MobileSpecification> findByActiveTrueOrderByBrandAscModelNameAscVariantNameAsc();

    List<MobileSpecification> findByBrandContainingIgnoreCaseOrModelNameContainingIgnoreCaseOrderByBrandAscModelNameAscVariantNameAsc(
            String brand,
            String modelName
    );
}
