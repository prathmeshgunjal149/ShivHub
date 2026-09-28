package com.shivhub.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shivhub.backend.dto.ProductCardResponse;
import com.shivhub.backend.dto.ProductRecommendationResponse;
import com.shivhub.backend.entity.OrderItem;
import com.shivhub.backend.entity.Product;
import com.shivhub.backend.entity.ProductCompatibility;
import com.shivhub.backend.entity.ProductImage;
import com.shivhub.backend.entity.ProductReview;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.OrderStatus;
import com.shivhub.backend.enums.ProductStatus;
import com.shivhub.backend.repository.OrderItemRepository;
import com.shivhub.backend.repository.ProductCompatibilityRepository;
import com.shivhub.backend.repository.ProductRepository;
import com.shivhub.backend.repository.ProductReviewRepository;

@Service
public class ProductRecommendationService {

    private static final int DEFAULT_LIMIT = 8;
    private static final int MIN_FREQUENTLY_BOUGHT_EVIDENCE = 2;

    private final ProductRepository productRepository;
    private final ProductCompatibilityRepository compatibilityRepository;
    private final ProductReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;

    public ProductRecommendationService(
            ProductRepository productRepository,
            ProductCompatibilityRepository compatibilityRepository,
            ProductReviewRepository reviewRepository,
            OrderItemRepository orderItemRepository) {
        this.productRepository = productRepository;
        this.compatibilityRepository = compatibilityRepository;
        this.reviewRepository = reviewRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional(readOnly = true)
    public ProductRecommendationResponse getRecommendations(Long productId, List<ProductCardResponse> recentlyViewed) {
        Product current = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        List<ProductCardResponse> compatible = explicitCompatible(current).stream()
                .limit(DEFAULT_LIMIT)
                .map(this::toCard)
                .toList();

        List<ProductCardResponse> frequentlyBought = frequentlyBought(current).stream()
                .limit(DEFAULT_LIMIT)
                .map(this::toCard)
                .toList();

        String boughtLabel = frequentlyBought.isEmpty()
                ? "Complete your purchase"
                : "Frequently bought together";
        String boughtSource = frequentlyBought.isEmpty()
                ? "Fell back to explicit compatible accessories because qualifying order evidence is insufficient."
                : "Based on delivered, shipped or out-for-delivery orders; cancelled orders are excluded.";

        List<ProductCardResponse> boughtOrFallback = frequentlyBought.isEmpty()
                ? compatible
                : frequentlyBought;

        return new ProductRecommendationResponse(
                boughtLabel,
                boughtSource,
                boughtOrFallback,
                "Compatible accessories",
                compatible,
                "Similar products",
                similarProducts(current).stream().limit(DEFAULT_LIMIT).map(this::toCard).toList(),
                "Recently viewed",
                recentlyViewed == null ? List.of() : recentlyViewed
        );
    }

    @Transactional(readOnly = true)
    public ProductCardResponse toCard(Product product) {
        List<ProductReview> reviews = reviewRepository.findByProductIdOrderByUpdatedAtDesc(product.getId());
        BigDecimal average = reviews.isEmpty()
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(reviews.stream().mapToInt(ProductReview::getRating).average().orElse(0))
                        .setScale(1, RoundingMode.HALF_UP);

        return new ProductCardResponse(
                product.getId(),
                product.getName(),
                product.getBrand(),
                product.getModel(),
                product.getRam(),
                product.getStorage(),
                product.getColorOptions(),
                product.getHsnCode(),
                product.getPrice(),
                product.getFinalSellingPrice(),
                product.getOfferPercentage(),
                product.getStock(),
                product.getAvailableStock(),
                isEligiblePublicProduct(product),
                firstImage(product),
                product.getCategoryEntity() == null ? product.getCategory() : product.getCategoryEntity().getName(),
                product.getSubCategory() == null ? null : product.getSubCategory().getName(),
                product.getSeller() == null ? "ShivHub" : safeSellerName(product.getSeller()),
                average,
                reviews.size()
        );
    }

    private List<Product> similarProducts(Product current) {
        List<ScoredProduct> scored = productRepository.findByActiveTrueAndApprovalStatus(ProductStatus.APPROVED)
                .stream()
                .filter(candidate -> !Objects.equals(candidate.getId(), current.getId()))
                .filter(this::isEligiblePublicProduct)
                .map(candidate -> new ScoredProduct(candidate, score(current, candidate)))
                .filter(scoredProduct -> scoredProduct.score > 0)
                .sorted(Comparator
                        .comparingInt(ScoredProduct::score).reversed()
                        .thenComparing(scoredProduct -> scoredProduct.product.getId()))
                .toList();

        if (scored.size() >= DEFAULT_LIMIT) {
            return scored.stream().map(ScoredProduct::product).toList();
        }

        return productRepository.findByActiveTrueAndApprovalStatus(ProductStatus.APPROVED)
                .stream()
                .filter(candidate -> !Objects.equals(candidate.getId(), current.getId()))
                .filter(this::isEligiblePublicProduct)
                .sorted(Comparator.comparing(Product::getId))
                .limit(DEFAULT_LIMIT)
                .toList();
    }

    private int score(Product current, Product candidate) {
        int score = 0;

        if (current.getSubCategory() != null && candidate.getSubCategory() != null
                && Objects.equals(current.getSubCategory().getId(), candidate.getSubCategory().getId())) {
            score += 50;
        } else if (current.getCategoryEntity() != null && candidate.getCategoryEntity() != null
                && Objects.equals(current.getCategoryEntity().getId(), candidate.getCategoryEntity().getId())) {
            score += 30;
        } else if (sameText(current.getCategory(), candidate.getCategory())) {
            score += 20;
        }

        if (sameText(current.getRam(), candidate.getRam())) score += 12;
        if (sameText(current.getStorage(), candidate.getStorage())) score += 12;
        if (sameText(current.getBrand(), candidate.getBrand())) score += 8;

        if (withinPriceRange(current.getFinalSellingPrice(), candidate.getFinalSellingPrice(), BigDecimal.valueOf(20))) {
            score += 20;
        } else if (withinPriceRange(current.getFinalSellingPrice(), candidate.getFinalSellingPrice(), BigDecimal.valueOf(50))) {
            score += 10;
        }

        return score;
    }

    private List<Product> explicitCompatible(Product current) {
        return compatibilityRepository.findBySourceProductIdAndActiveTrue(current.getId())
                .stream()
                .map(ProductCompatibility::getAccessoryProduct)
                .filter(Objects::nonNull)
                .filter(this::isEligiblePublicProduct)
                .filter(product -> !Objects.equals(product.getId(), current.getId()))
                .distinct()
                .sorted(Comparator.comparing(Product::getId))
                .toList();
    }

    private List<Product> frequentlyBought(Product current) {
        List<OrderItem> sourceItems = orderItemRepository.findQualifyingRecommendationItemsByProductId(
                current.getId(),
                List.of(OrderStatus.SHIPPED, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED)
        );

        Map<Long, Integer> evidence = new LinkedHashMap<>();
        for (OrderItem sourceItem : sourceItems) {
            for (OrderItem peer : orderItemRepository.findByOrderId(sourceItem.getOrder().getId())) {
                if (Objects.equals(peer.getProductId(), current.getId())) continue;
                evidence.merge(peer.getProductId(), 1, Integer::sum);
            }
        }

        return evidence.entrySet()
                .stream()
                .filter(entry -> entry.getValue() >= MIN_FREQUENTLY_BOUGHT_EVIDENCE)
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> productRepository.findById(entry.getKey()))
                .flatMap(Optional::stream)
                .filter(this::isEligiblePublicProduct)
                .limit(DEFAULT_LIMIT)
                .toList();
    }

    public boolean isEligiblePublicProduct(Product product) {
        if (product == null || !product.isActive() || product.getApprovalStatus() != ProductStatus.APPROVED) {
            return false;
        }
        if (product.getAvailableStock() <= 0) {
            return false;
        }
        User seller = product.getSeller();
        return seller != null && seller.isEnabled();
    }

    private String firstImage(Product product) {
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            return product.getImages().stream()
                    .filter(image -> image.getImageUrl() != null && !image.getImageUrl().isBlank())
                    .sorted(Comparator.comparing(ProductImage::getDisplayOrder, Comparator.nullsLast(Integer::compareTo)))
                    .map(ProductImage::getImageUrl)
                    .findFirst()
                    .orElse(product.getImageUrl());
        }
        return product.getImageUrl();
    }

    private boolean withinPriceRange(BigDecimal base, BigDecimal candidate, BigDecimal percent) {
        if (base == null || candidate == null || base.signum() <= 0) {
            return false;
        }
        BigDecimal difference = candidate.subtract(base).abs();
        BigDecimal allowed = base.multiply(percent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return difference.compareTo(allowed) <= 0;
    }

    private boolean sameText(String first, String second) {
        return first != null && second != null && first.trim().equalsIgnoreCase(second.trim());
    }

    private String safeSellerName(User seller) {
        if (seller.getBusinessName() != null && !seller.getBusinessName().isBlank()) return seller.getBusinessName();
        return seller.getName();
    }

    private record ScoredProduct(Product product, int score) {
    }
}
