package com.shivhub.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shivhub.backend.entity.ProductCompatibility;

public interface ProductCompatibilityRepository extends JpaRepository<ProductCompatibility, Long> {

    List<ProductCompatibility> findBySourceProductIdAndActiveTrue(Long sourceProductId);

    List<ProductCompatibility> findBySourceProductIdAndSellerId(Long sourceProductId, Long sellerId);

    Optional<ProductCompatibility> findBySourceProductIdAndAccessoryProductId(Long sourceProductId, Long accessoryProductId);
}
