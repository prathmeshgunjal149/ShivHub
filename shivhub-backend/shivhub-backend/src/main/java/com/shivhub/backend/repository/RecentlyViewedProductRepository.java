package com.shivhub.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shivhub.backend.entity.RecentlyViewedProduct;
import com.shivhub.backend.entity.User;

public interface RecentlyViewedProductRepository extends JpaRepository<RecentlyViewedProduct, Long> {

    Optional<RecentlyViewedProduct> findByCustomerIdAndProductId(Long customerId, Long productId);

    List<RecentlyViewedProduct> findTop20ByCustomerOrderByLastViewedAtDesc(User customer);

    List<RecentlyViewedProduct> findByCustomerOrderByLastViewedAtDesc(User customer);
}
