package com.shivhub.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shivhub.backend.entity.ProductReview;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {
    List<ProductReview> findByProductIdOrderByUpdatedAtDesc(Long productId);
    Optional<ProductReview> findByProductIdAndCustomerId(Long productId, Long customerId);
}
