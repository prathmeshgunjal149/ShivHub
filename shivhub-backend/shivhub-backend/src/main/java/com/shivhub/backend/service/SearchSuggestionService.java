package com.shivhub.backend.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.shivhub.backend.dto.SearchSuggestionItem;
import com.shivhub.backend.dto.SearchSuggestionsResponse;
import com.shivhub.backend.entity.Category;
import com.shivhub.backend.entity.CustomerProfile;
import com.shivhub.backend.entity.Distributor;
import com.shivhub.backend.entity.Product;
import com.shivhub.backend.entity.SellerDistributor;
import com.shivhub.backend.entity.SubCategory;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.ProductStatus;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.repository.CategoryRepository;
import com.shivhub.backend.repository.CustomerProfileRepository;
import com.shivhub.backend.repository.DistributorRepository;
import com.shivhub.backend.repository.ProductRepository;
import com.shivhub.backend.repository.SellerDistributorRepository;
import com.shivhub.backend.repository.SubCategoryRepository;
import com.shivhub.backend.repository.UserRepository;

/**
 * Small, role-scoped search projections used by the reusable browser autocomplete.
 * It intentionally returns DTOs rather than exposing entities or internal accounting data.
 */
@Service
@Transactional(readOnly = true)
public class SearchSuggestionService {

    private static final int MAX_LIMIT = 8;

    private final ProductRepository productRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final SellerDistributorRepository sellerDistributorRepository;
    private final DistributorRepository distributorRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final UserRepository userRepository;

    public SearchSuggestionService(
            ProductRepository productRepository,
            CustomerProfileRepository customerProfileRepository,
            SellerDistributorRepository sellerDistributorRepository,
            DistributorRepository distributorRepository,
            CategoryRepository categoryRepository,
            SubCategoryRepository subCategoryRepository,
            UserRepository userRepository) {
        this.productRepository = productRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.sellerDistributorRepository = sellerDistributorRepository;
        this.distributorRepository = distributorRepository;
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
        this.userRepository = userRepository;
    }

    public SearchSuggestionsResponse customerSuggestions(String requestedQuery, Integer requestedLimit) {
        String query = validatedQuery(requestedQuery);
        if (query == null) return empty(requestedQuery);
        int limit = safeLimit(requestedLimit);
        Pageable page = PageRequest.of(0, limit);
        List<SearchSuggestionItem> results = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        productRepository.searchPublicSuggestions(ProductStatus.APPROVED, query, page)
                .forEach(product -> add(results, seen, productItem(product, "PRODUCT", "/product/" + product.getId()), limit));

        categoryRepository.findByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(query, page)
                .forEach(category -> add(results, seen, categoryItem(category, "/customer/dashboard?category=" + category.getId()), limit));
        subCategoryRepository.searchPublicSuggestions(query, page)
                .forEach(subCategory -> add(results, seen, subCategoryItem(subCategory), limit));

        // Brands are derived only from customer-visible products, so a private seller brand never leaks.
        productRepository.searchPublicSuggestions(ProductStatus.APPROVED, query, page).stream()
                .map(Product::getBrand)
                .filter(this::hasText)
                .map(String::trim)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
                .forEach(brand -> add(results, seen, new SearchSuggestionItem(
                        null, "BRAND", brand, "Brand", null, null, null, null,
                        "/customer/dashboard?brand=" + urlValue(brand), Map.of("brand", brand)), limit));

        return new SearchSuggestionsResponse(query, results);
    }

    public SearchSuggestionsResponse sellerSuggestions(String email, String requestedType, String requestedQuery, Integer requestedLimit) {
        String query = validatedQuery(requestedQuery);
        if (query == null) return empty(requestedQuery);
        User seller = userRepository.findByEmailIgnoreCase(email)
                .filter(user -> user.getRole() == Role.SELLER)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Seller access is required."));
        int limit = safeLimit(requestedLimit);
        Pageable page = PageRequest.of(0, limit);
        String type = normalizedType(requestedType, "PRODUCT");
        List<SearchSuggestionItem> results = switch (type) {
            case "PRODUCT" -> productRepository.searchSellerSuggestions(seller, query, page).stream()
                    .map(product -> productItem(product, "PRODUCT", null)).toList();
            case "CUSTOMER" -> customerProfileRepository.searchSellerSuggestions(seller, query, page).stream()
                    .map(this::sellerCustomerItem).toList();
            case "DISTRIBUTOR" -> sellerDistributorRepository.searchActiveSuggestions(seller, query, page).stream()
                    .map(this::sellerDistributorItem).toList();
            default -> throw unsupportedType(type);
        };
        return response(query, results, limit);
    }

    public SearchSuggestionsResponse adminSuggestions(String requestedType, String requestedQuery, Integer requestedLimit) {
        String query = validatedQuery(requestedQuery);
        if (query == null) return empty(requestedQuery);
        int limit = safeLimit(requestedLimit);
        Pageable page = PageRequest.of(0, limit);
        String type = normalizedType(requestedType, "ALL");
        List<SearchSuggestionItem> results = new ArrayList<>();

        switch (type) {
            case "ALL" -> {
                productRepository.searchAdminSuggestions(query, page).forEach(product -> results.add(productItem(product, "PRODUCT", null)));
                userRepository.searchByRoleForSuggestions(Role.SELLER, query, page).forEach(user -> results.add(sellerItem(user)));
                customerProfileRepository.searchAdminSuggestions(query, page).forEach(profile -> results.add(adminCustomerItem(profile)));
                distributorRepository.searchSuggestions(query, page).forEach(distributor -> results.add(distributorItem(distributor)));
                categoryRepository.findByNameContainingIgnoreCaseOrderByNameAsc(query, page).forEach(category -> results.add(categoryItem(category, null)));
            }
            case "PRODUCT" -> productRepository.searchAdminSuggestions(query, page).forEach(product -> results.add(productItem(product, "PRODUCT", null)));
            case "SELLER" -> userRepository.searchByRoleForSuggestions(Role.SELLER, query, page).forEach(user -> results.add(sellerItem(user)));
            case "CUSTOMER" -> customerProfileRepository.searchAdminSuggestions(query, page).forEach(profile -> results.add(adminCustomerItem(profile)));
            case "DISTRIBUTOR" -> distributorRepository.searchSuggestions(query, page).forEach(distributor -> results.add(distributorItem(distributor)));
            case "CATEGORY" -> {
                categoryRepository.findByNameContainingIgnoreCaseOrderByNameAsc(query, page).forEach(category -> results.add(categoryItem(category, null)));
                subCategoryRepository.searchAdminSuggestions(query, page).forEach(subCategory -> results.add(subCategoryItem(subCategory)));
            }
            default -> throw unsupportedType(type);
        }
        return response(query, results, limit);
    }

    private SearchSuggestionsResponse response(String query, List<SearchSuggestionItem> candidates, int limit) {
        List<SearchSuggestionItem> results = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        candidates.forEach(candidate -> add(results, seen, candidate, limit));
        return new SearchSuggestionsResponse(query, results);
    }

    private SearchSuggestionItem productItem(Product product, String type, String route) {
        String category = product.getCategoryEntity() != null ? product.getCategoryEntity().getName() : product.getCategory();
        String subCategory = product.getSubCategory() == null ? null : product.getSubCategory().getName();
        String variant = joinNonBlank(" · ", product.getBrand(), product.getModel(), product.getRam(), product.getStorage(), product.getColorOptions());
        Map<String, String> metadata = new LinkedHashMap<>();
        put(metadata, "brand", product.getBrand());
        put(metadata, "model", product.getModel());
        put(metadata, "sku", product.getSellerSku());
        put(metadata, "barcode", product.getBarcode());
        put(metadata, "imeiTracked", String.valueOf(product.isSerialTrackingRequired()));
        put(metadata, "active", String.valueOf(product.isActive()));
        return new SearchSuggestionItem(product.getId(), type, product.getName(), variant,
                productImage(product), product.getPrice(), category, subCategory, route, metadata);
    }

    private SearchSuggestionItem sellerCustomerItem(CustomerProfile profile) {
        return new SearchSuggestionItem(profile.getId(), "CUSTOMER", profile.getName(),
                joinNonBlank(" · ", maskedMobile(profile.getMobile()), profile.getEmail(), profile.getCity()),
                null, null, null, null, null, Map.of(
                        "mobile", nullToEmpty(profile.getMobile()),
                        "email", nullToEmpty(profile.getEmail()),
                        "city", nullToEmpty(profile.getCity())));
    }

    private SearchSuggestionItem adminCustomerItem(CustomerProfile profile) {
        return new SearchSuggestionItem(profile.getId(), "CUSTOMER", profile.getName(),
                joinNonBlank(" · ", profile.getMobile(), profile.getEmail(), profile.getCity()),
                null, null, null, null, null, Map.of("email", nullToEmpty(profile.getEmail())));
    }

    private SearchSuggestionItem sellerDistributorItem(SellerDistributor link) {
        Distributor distributor = link.getDistributor();
        return new SearchSuggestionItem(distributor.getId(), "DISTRIBUTOR", distributor.getBusinessName(),
                joinNonBlank(" · ", link.getBrand(), distributor.getMobile(), distributor.getEmail()),
                null, null, null, null, null, Map.of("brand", nullToEmpty(link.getBrand()), "gstin", nullToEmpty(distributor.getGstin())));
    }

    private SearchSuggestionItem distributorItem(Distributor distributor) {
        return new SearchSuggestionItem(distributor.getId(), "DISTRIBUTOR", distributor.getBusinessName(),
                joinNonBlank(" · ", distributor.getMobile(), distributor.getEmail(), distributor.getGstin()),
                null, null, null, null, null, Map.of("gstin", nullToEmpty(distributor.getGstin())));
    }

    private SearchSuggestionItem sellerItem(User seller) {
        return new SearchSuggestionItem(seller.getId(), "SELLER", seller.getName(),
                joinNonBlank(" · ", seller.getBusinessName(), seller.getEmail(), seller.getMobile()),
                null, null, null, null, null, Map.of("email", nullToEmpty(seller.getEmail())));
    }

    private SearchSuggestionItem categoryItem(Category category, String route) {
        return new SearchSuggestionItem(category.getId(), "CATEGORY", category.getName(), "Category", null,
                null, category.getName(), null, route, Map.of("categoryId", String.valueOf(category.getId())));
    }

    private SearchSuggestionItem subCategoryItem(SubCategory subCategory) {
        return new SearchSuggestionItem(subCategory.getId(), "SUBCATEGORY", subCategory.getName(),
                subCategory.getCategory().getName(), null, null, subCategory.getCategory().getName(),
                subCategory.getName(), "/customer/dashboard?subcategory=" + subCategory.getId(),
                Map.of("categoryId", String.valueOf(subCategory.getCategory().getId())));
    }

    private String productImage(Product product) {
        // Do not traverse Product.images here: it is intentionally lazy and would turn an
        // eight-result autocomplete response into an N+1 query. The legacy primary image
        // URL is already customer-safe; pages retain their richer image loading as before.
        return hasText(product.getImageUrl()) ? product.getImageUrl() : null;
    }

    private void add(List<SearchSuggestionItem> results, Set<String> seen, SearchSuggestionItem item, int limit) {
        if (item == null || results.size() >= limit) return;
        String key = item.type() + ":" + (item.id() == null ? item.label().toLowerCase(Locale.ROOT) : item.id());
        if (seen.add(key)) results.add(item);
    }

    private String validatedQuery(String raw) {
        String query = raw == null ? "" : raw.trim().replaceAll("\\s+", " ");
        if (query.length() < 2) return null;
        if (query.length() > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search text must be 100 characters or fewer.");
        return query;
    }

    private int safeLimit(Integer requestedLimit) { return Math.min(MAX_LIMIT, Math.max(1, requestedLimit == null ? MAX_LIMIT : requestedLimit)); }
    private SearchSuggestionsResponse empty(String query) { return new SearchSuggestionsResponse(query == null ? "" : query.trim(), List.of()); }
    private String normalizedType(String value, String defaultValue) { return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : defaultValue; }
    private ResponseStatusException unsupportedType(String type) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported suggestion type: " + type); }
    private boolean hasText(String value) { return value != null && !value.isBlank(); }
    private String nullToEmpty(String value) { return value == null ? "" : value; }
    private void put(Map<String, String> metadata, String key, String value) { if (hasText(value)) metadata.put(key, value); }
    private String joinNonBlank(String delimiter, String... values) { return java.util.Arrays.stream(values).filter(this::hasText).map(String::trim).filter(Objects::nonNull).reduce((a, b) -> a + delimiter + b).orElse(null); }
    private String maskedMobile(String mobile) { return !hasText(mobile) || mobile.length() < 5 ? mobile : "••••••" + mobile.substring(mobile.length() - 4); }
    private String urlValue(String value) { return value.trim().replace(" ", "%20"); }
}
