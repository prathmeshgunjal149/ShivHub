package com.shivhub.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shivhub.backend.dto.ProductReviewRequest;
import com.shivhub.backend.dto.ProductReviewResponse;
import com.shivhub.backend.dto.ProductReviewSummary;
import com.shivhub.backend.entity.Product;
import com.shivhub.backend.entity.ProductReview;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.repository.ProductRepository;
import com.shivhub.backend.repository.ProductReviewRepository;
import com.shivhub.backend.repository.UserRepository;

@Service
public class ProductReviewService {
    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public ProductReviewService(ProductReviewRepository reviewRepository, ProductRepository productRepository, UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public ProductReviewSummary getReviews(Long productId, Long viewerId) {
        requireProduct(productId);
        List<ProductReview> reviews = reviewRepository.findByProductIdOrderByUpdatedAtDesc(productId);
        long[] breakdown = new long[5];
        int total = 0;
        for (ProductReview review : reviews) {
            breakdown[review.getRating() - 1]++;
            total += review.getRating();
        }
        BigDecimal average = reviews.isEmpty() ? BigDecimal.ZERO : BigDecimal.valueOf(total)
                .divide(BigDecimal.valueOf(reviews.size()), 1, RoundingMode.HALF_UP);
        return new ProductReviewSummary(average, reviews.size(), breakdown,
                reviews.stream().map(review -> toResponse(review, viewerId)).toList());
    }

    @Transactional
    public ProductReviewResponse create(Long productId, Long customerId, ProductReviewRequest request) {
        if (reviewRepository.findByProductIdAndCustomerId(productId, customerId).isPresent()) {
            throw new RuntimeException("You have already reviewed this product. Edit your existing review instead.");
        }
        return save(productId, customerId, request);
    }

    @Transactional
    public ProductReviewResponse updateMine(Long productId, Long customerId, ProductReviewRequest request) {
        ProductReview review = reviewRepository.findByProductIdAndCustomerId(productId, customerId)
                .orElseThrow(() -> new RuntimeException("Your review was not found"));
        review.setRating(request.getRating());
        review.setComment(request.getComment().trim());
        return toResponse(reviewRepository.save(review), customerId);
    }

    @Transactional
    public void deleteMine(Long productId, Long customerId) {
        ProductReview review = reviewRepository.findByProductIdAndCustomerId(productId, customerId)
                .orElseThrow(() -> new RuntimeException("Your review was not found"));
        reviewRepository.delete(review);
    }

    private ProductReviewResponse save(Long productId, Long customerId, ProductReviewRequest request) {
        ProductReview review = new ProductReview();
        review.setProduct(requireProduct(productId));
        review.setCustomer(requireCustomer(customerId));
        review.setRating(request.getRating());
        review.setComment(request.getComment().trim());
        return toResponse(reviewRepository.save(review), customerId);
    }

    private Product requireProduct(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));
    }

    private User requireCustomer(Long id) {
        User customer = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Customer not found"));
        if (customer.getRole() != Role.CUSTOMER || !customer.isEnabled()) {
            throw new RuntimeException("Only active customers can submit reviews");
        }
        return customer;
    }

    private ProductReviewResponse toResponse(ProductReview review, Long viewerId) {
        User customer = review.getCustomer();
        return new ProductReviewResponse(review.getId(), customer.getId(), customer.getName(), review.getRating(),
                review.getComment(), review.getCreatedAt(), review.getUpdatedAt(), customer.getId().equals(viewerId));
    }
}
