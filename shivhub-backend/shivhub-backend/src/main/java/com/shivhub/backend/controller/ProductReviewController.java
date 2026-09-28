package com.shivhub.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shivhub.backend.dto.ProductReviewRequest;
import com.shivhub.backend.dto.ProductReviewResponse;
import com.shivhub.backend.dto.ProductReviewSummary;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.repository.UserRepository;
import com.shivhub.backend.service.ProductReviewService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products/{productId}/reviews")
public class ProductReviewController {
    private final ProductReviewService reviewService;
    private final UserRepository userRepository;

    public ProductReviewController(ProductReviewService reviewService, UserRepository userRepository) {
        this.reviewService = reviewService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<ProductReviewSummary> getReviews(@PathVariable Long productId, Authentication authentication) {
        return ResponseEntity.ok(reviewService.getReviews(productId, customerIdOrNull(authentication)));
    }

    @PostMapping
    public ResponseEntity<ProductReviewResponse> create(@PathVariable Long productId, @Valid @RequestBody ProductReviewRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.create(productId, requireCustomerId(authentication), request));
    }

    @PutMapping("/mine")
    public ResponseEntity<ProductReviewResponse> updateMine(@PathVariable Long productId, @Valid @RequestBody ProductReviewRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(reviewService.updateMine(productId, requireCustomerId(authentication), request));
    }

    @DeleteMapping("/mine")
    public ResponseEntity<Void> deleteMine(@PathVariable Long productId, Authentication authentication) {
        reviewService.deleteMine(productId, requireCustomerId(authentication));
        return ResponseEntity.noContent().build();
    }

    private Long customerIdOrNull(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;
        return userRepository.findByEmail(authentication.getName()).filter(user -> user.getRole() == Role.CUSTOMER)
                .map(User::getId).orElse(null);
    }

    private Long requireCustomerId(Authentication authentication) {
        Long customerId = customerIdOrNull(authentication);
        if (customerId == null) throw new RuntimeException("Customer authentication is required");
        return customerId;
    }
}
