package com.shivhub.backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shivhub.backend.dto.ProductCardResponse;
import com.shivhub.backend.entity.Product;
import com.shivhub.backend.entity.RecentlyViewedProduct;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.repository.ProductRepository;
import com.shivhub.backend.repository.RecentlyViewedProductRepository;
import com.shivhub.backend.repository.UserRepository;

@Service
public class RecentlyViewedProductService {

    private static final int MAX_RECENT_ITEMS = 20;

    private final RecentlyViewedProductRepository recentlyViewedRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductRecommendationService recommendationService;

    public RecentlyViewedProductService(
            RecentlyViewedProductRepository recentlyViewedRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            ProductRecommendationService recommendationService) {
        this.recentlyViewedRepository = recentlyViewedRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.recommendationService = recommendationService;
    }

    @Transactional
    public void remember(String customerEmail, Long productId) {
        User customer = activeCustomer(customerEmail);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!recommendationService.isEligiblePublicProduct(product)) {
            throw new RuntimeException("Product is not available");
        }

        RecentlyViewedProduct entry = recentlyViewedRepository
                .findByCustomerIdAndProductId(customer.getId(), productId)
                .orElseGet(RecentlyViewedProduct::new);
        entry.setCustomer(customer);
        entry.setProduct(product);
        entry.setLastViewedAt(LocalDateTime.now());
        recentlyViewedRepository.save(entry);

        List<RecentlyViewedProduct> all = recentlyViewedRepository.findByCustomerOrderByLastViewedAtDesc(customer);
        if (all.size() > MAX_RECENT_ITEMS) {
            recentlyViewedRepository.deleteAll(all.subList(MAX_RECENT_ITEMS, all.size()));
        }
    }

    @Transactional(readOnly = true)
    public List<ProductCardResponse> getRecent(String customerEmail, Long excludeProductId) {
        User customer = activeCustomer(customerEmail);
        return recentlyViewedRepository.findTop20ByCustomerOrderByLastViewedAtDesc(customer)
                .stream()
                .map(RecentlyViewedProduct::getProduct)
                .filter(Objects::nonNull)
                .filter(product -> !Objects.equals(product.getId(), excludeProductId))
                .filter(recommendationService::isEligiblePublicProduct)
                .map(recommendationService::toCard)
                .toList();
    }

    private User activeCustomer(String email) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Authentication required");
        }
        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        if (customer.getRole() != Role.CUSTOMER || !customer.isEnabled()) {
            throw new RuntimeException("Only active customers can use recently viewed products");
        }
        return customer;
    }
}
