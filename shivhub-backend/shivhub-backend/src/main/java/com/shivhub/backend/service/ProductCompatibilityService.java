package com.shivhub.backend.service;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shivhub.backend.dto.ProductCardResponse;
import com.shivhub.backend.dto.ProductCompatibilityRequest;
import com.shivhub.backend.entity.Product;
import com.shivhub.backend.entity.ProductCompatibility;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.repository.ProductCompatibilityRepository;
import com.shivhub.backend.repository.ProductRepository;
import com.shivhub.backend.repository.UserRepository;

@Service
public class ProductCompatibilityService {

    private final ProductCompatibilityRepository compatibilityRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductRecommendationService recommendationService;

    public ProductCompatibilityService(
            ProductCompatibilityRepository compatibilityRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            ProductRecommendationService recommendationService) {
        this.compatibilityRepository = compatibilityRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.recommendationService = recommendationService;
    }

    @Transactional(readOnly = true)
    public List<ProductCardResponse> getSellerAccessories(String email, Long productId) {
        User seller = seller(email);
        Product source = ownProduct(productId, seller);
        return compatibilityRepository.findBySourceProductIdAndSellerId(source.getId(), seller.getId())
                .stream()
                .map(ProductCompatibility::getAccessoryProduct)
                .filter(Objects::nonNull)
                .map(recommendationService::toCard)
                .toList();
    }

    @Transactional
    public ProductCardResponse saveSellerAccessory(String email, Long productId, ProductCompatibilityRequest request) {
        User seller = seller(email);
        Product source = ownProduct(productId, seller);
        Product accessory = ownProduct(request.accessoryProductId(), seller);
        if (Objects.equals(source.getId(), accessory.getId())) {
            throw new RuntimeException("A product cannot be compatible with itself");
        }

        ProductCompatibility compatibility = compatibilityRepository
                .findBySourceProductIdAndAccessoryProductId(source.getId(), accessory.getId())
                .orElseGet(ProductCompatibility::new);
        compatibility.setSourceProduct(source);
        compatibility.setAccessoryProduct(accessory);
        compatibility.setSeller(seller);
        compatibility.setNote(request.note());
        compatibility.setActive(request.active() == null || request.active());
        compatibilityRepository.save(compatibility);
        return recommendationService.toCard(accessory);
    }

    @Transactional
    public void deleteSellerAccessory(String email, Long productId, Long accessoryProductId) {
        User seller = seller(email);
        ownProduct(productId, seller);
        ProductCompatibility compatibility = compatibilityRepository
                .findBySourceProductIdAndAccessoryProductId(productId, accessoryProductId)
                .orElseThrow(() -> new RuntimeException("Compatibility relation not found"));
        if (compatibility.getSeller() == null || !Objects.equals(compatibility.getSeller().getId(), seller.getId())) {
            throw new RuntimeException("You are not allowed to remove this relation");
        }
        compatibilityRepository.delete(compatibility);
    }

    private Product ownProduct(Long productId, User seller) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        if (product.getSeller() == null || !Objects.equals(product.getSeller().getId(), seller.getId())) {
            throw new RuntimeException("You can manage compatibility only for your own products");
        }
        return product;
    }

    private User seller(String email) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Authentication required");
        }
        User seller = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Seller not found"));
        if (seller.getRole() != Role.SELLER || !seller.isEnabled()) {
            throw new RuntimeException("Only active sellers can manage compatible accessories");
        }
        return seller;
    }
}
