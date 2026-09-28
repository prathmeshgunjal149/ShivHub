package com.shivhub.backend.service;

import java.util.Collections;
import java.util.List;
import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.shivhub.backend.dto.CreateProductRequest;
import com.shivhub.backend.entity.Category;
import com.shivhub.backend.entity.Product;
import com.shivhub.backend.entity.ProductImage;
import com.shivhub.backend.entity.SubCategory;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.ProductSource;
import com.shivhub.backend.enums.ProductStatus;
import com.shivhub.backend.repository.CategoryRepository;
import com.shivhub.backend.repository.ProductRepository;
import com.shivhub.backend.repository.SubCategoryRepository;
import com.shivhub.backend.repository.UserRepository;


/*
 * =========================================================
 * ProductService
 * =========================================================
 *
 * Handles:
 *
 * 1. Seller Product Creation
 * 2. Admin Product Creation
 * 3. Product Listing
 * 4. Product Update
 * 5. Product Delete
 *
 * =========================================================
 */

@Service
public class ProductService {


    private final ProductRepository productRepository;

    private final UserRepository userRepository;

    private final CategoryRepository categoryRepository;

    private final SubCategoryRepository subCategoryRepository;

    private final ProductImageService productImageService;

    private final GstCalculator gstCalculator;
    private final ProductConfigurationService productConfigurationService;


    /*
     * =====================================================
     * CONSTRUCTOR
     * =====================================================
     */

    public ProductService(
            ProductRepository productRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            SubCategoryRepository subCategoryRepository,
            ProductImageService productImageService,
            GstCalculator gstCalculator, ProductConfigurationService productConfigurationService) {

        this.productRepository =
                productRepository;

        this.userRepository =
                userRepository;

        this.categoryRepository =
                categoryRepository;

        this.subCategoryRepository =
                subCategoryRepository;

        this.productImageService =
                productImageService;

        this.gstCalculator =
                gstCalculator;
        this.productConfigurationService = productConfigurationService;
    }


    /*
     * =====================================================
     * CREATE SELLER PRODUCT
     * =====================================================
     *
     * Seller can provide 5 to 15 product images using:
     *
     * 1. Images uploaded from computer
     * 2. Image URLs
     *
     * Computer images + URL images = 5 to 15 images.
     *
     * Seller product starts as:
     *
     * PENDING
     *
     * and:
     *
     * ACTIVE = false
     *
     * =====================================================
     */

    @Transactional
    public Product createProduct(
            CreateProductRequest request,
            Long sellerId,
            MultipartFile[] images,
            List<String> imageUrls) {


        /*
         * =================================================
         * BASIC VALIDATION
         * =================================================
         */

        validateBasicProductRequest(
                request
        );


        /*
         * =================================================
         * IMAGE COUNT
         * =================================================
         *
         * Seller can mix computer-uploaded images and image URLs.
         * Total must be between 5 and 15.
         *
         * =================================================
         */

        int uploadedCount =
                images == null
                        ? 0
                        : images.length;


        int urlCount =
                imageUrls == null
                        ? 0
                        : imageUrls.size();


        int totalImages = uploadedCount + urlCount;

        if (totalImages < 5 || totalImages > 15) {

            throw new RuntimeException(
                    "Provide between 5 and 15 product images. Use computer upload or image URL."
            );
        }


        /*
         * =================================================
         * FIND SELLER
         * =================================================
         */

        User seller =
                userRepository
                        .findById(sellerId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Seller not found"
                                )
                        );


        /*
         * =================================================
         * CHECK SELLER ROLE
         * =================================================
         */

        if (seller.getRole() == null ||
                !"SELLER".equals(
                        seller.getRole().name()
                )) {

            throw new RuntimeException(
                    "Only sellers can create products"
            );
        }


        /*
         * =================================================
         * CHECK SELLER ACTIVE
         * =================================================
         */

        if (!seller.isEnabled()) {

            throw new RuntimeException(
                    "Seller account is not active"
            );
        }


        /*
         * =================================================
         * GET CATEGORY
         * =================================================
         */

        Category category =
                getCategory(
                        request.getCategoryId()
                );


        /*
         * =================================================
         * GET SUBCATEGORY
         * =================================================
         */

        SubCategory subCategory =
                getSubCategory(
                        request.getSubCategoryId()
                );


        /*
         * =================================================
         * VALIDATE CATEGORY RELATION
         * =================================================
         */

        validateCategoryRelation(
                category,
                subCategory
        );


        /*
         * =================================================
         * CREATE PRODUCT
         * =================================================
         */

        Product product =
                new Product();


        /*
         * =================================================
         * SET COMMON PRODUCT FIELDS
         * =================================================
         */

        setCommonProductFields(
                product,
                request,
                category,
                subCategory
        );


        /*
         * =================================================
         * SET SELLER
         * =================================================
         */

        product.setSeller(
                seller
        );


        /*
         * =================================================
         * SELLER PRODUCT STATUS
         * =================================================
         */

        product.setSource(
                ProductSource.SELLER
        );


        product.setApprovalStatus(
                ProductStatus.PENDING
        );


        /*
         * Product should not be visible to customers
         * before Admin approval.
         */

        product.setActive(
                false
        );


        /*
         * =================================================
         * SAVE PRODUCT
         * =================================================
         */

        Product savedProduct =
                productRepository.save(
                        product
                );


        /*
         * =================================================
         * SAVE PRODUCT IMAGES
         * =================================================
         *
         * Computer uploaded images + external image URLs
         * are saved together.
         *
         * Total image count has already been validated as 5.
         *
         * =================================================
         */

        List<ProductImage> savedImages = productImageService.createProductImages(
                savedProduct.getId(),
                images,
                imageUrls == null
                        ? Collections.emptyList()
                        : imageUrls
        );


        // Populate the legacy primary field as well so existing seller/admin
        // review screens can always show the first submitted image.
        if (!savedImages.isEmpty()) {
            savedProduct.setImageUrl(savedImages.get(0).getImageUrl());
            savedProduct = productRepository.save(savedProduct);
        }

        return savedProduct;
    }


    /*
     * =====================================================
     * CREATE ADMIN PRODUCT
     * =====================================================
     *
     * Admin can provide:
     *
     * 1. Computer uploaded images
     * 2. Image URLs
     *
     * Total must be between 5 and 15.
     *
     * =====================================================
     */

    @Transactional
    public Product createAdminProduct(
            CreateProductRequest request,
            Long adminId,
            MultipartFile[] images,
            List<String> imageUrls) {


        /*
         * =================================================
         * BASIC VALIDATION
         * =================================================
         */

        validateBasicProductRequest(
                request
        );


        /*
         * =================================================
         * IMAGE COUNT
         * =================================================
         */

        int uploadedCount =
                images == null
                        ? 0
                        : images.length;


        int urlCount =
                imageUrls == null
                        ? 0
                        : imageUrls.size();


        /*
         * =================================================
         * PRODUCT IMAGE VALIDATION
         * =================================================
         */

        int totalImages = uploadedCount + urlCount;

        if (totalImages < 5 || totalImages > 15) {

            throw new RuntimeException(
                    "Provide between 5 and 15 product images. Use computer upload or image URL."
            );
        }


        /*
         * =================================================
         * FIND ADMIN
         * =================================================
         */

        User admin =
                userRepository
                        .findById(adminId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Admin user not found"
                                )
                        );


        /*
         * =================================================
         * CHECK ADMIN ROLE
         * =================================================
         */

        if (admin.getRole() == null ||
                !"ADMIN".equals(
                        admin.getRole().name()
                )) {

            throw new RuntimeException(
                    "Only Admin can create ShivHub products"
            );
        }


        /*
         * =================================================
         * CHECK ADMIN ACTIVE
         * =================================================
         */

        if (!admin.isEnabled()) {

            throw new RuntimeException(
                    "Admin account is not active"
            );
        }


        /*
         * =================================================
         * CATEGORY
         * =================================================
         */

        Category category =
                getCategory(
                        request.getCategoryId()
                );


        /*
         * =================================================
         * SUBCATEGORY
         * =================================================
         */

        SubCategory subCategory =
                getSubCategory(
                        request.getSubCategoryId()
                );


        /*
         * =================================================
         * CATEGORY RELATION
         * =================================================
         */

        validateCategoryRelation(
                category,
                subCategory
        );


        /*
         * =================================================
         * CREATE PRODUCT
         * =================================================
         */

        Product product =
                new Product();


        /*
         * =================================================
         * COMMON FIELDS
         * =================================================
         */

        setCommonProductFields(
                product,
                request,
                category,
                subCategory
        );


        /*
         * =================================================
         * ADMIN AS OWNER
         * =================================================
         *
         * Existing products table requires seller_id.
         *
         * Therefore Admin is stored as owner.
         *
         * =================================================
         */

        product.setSeller(
                admin
        );


        /*
         * =================================================
         * ADMIN PRODUCT STATUS
         * =================================================
         */

        product.setSource(
                ProductSource.ADMIN
        );


        product.setApprovalStatus(
                ProductStatus.PENDING
        );


        product.setActive(
                false
        );


        product.setAdminReview(
                null
        );


        product.setReviewedAt(
                null
        );


        /*
         * =================================================
         * SAVE PRODUCT
         * =================================================
         */

        Product savedProduct =
                productRepository.save(
                        product
                );


        /*
         * =================================================
         * SAVE IMAGES
         * =================================================
         *
         * Computer uploads +
         * external URLs
         *
         * Total between 5 and 15.
         *
         * =================================================
         */

        productImageService.createProductImages(
                savedProduct.getId(),
                images,
                imageUrls == null
                        ? Collections.emptyList()
                        : imageUrls
        );


        return savedProduct;
    }


    /*
     * =====================================================
     * COMMON PRODUCT VALIDATION
     * =====================================================
     */

    private void validateBasicProductRequest(
            CreateProductRequest request) {


        if (request == null) {

            throw new RuntimeException(
                    "Product request is required"
            );
        }


        /*
         * PRODUCT NAME
         */

        if (
                request.getName() == null ||
                request.getName()
                        .trim()
                        .isEmpty()
        ) {

            throw new RuntimeException(
                    "Product name is required"
            );
        }


        /*
         * DESCRIPTION
         */

        if (
                request.getDescription() == null ||
                request.getDescription()
                        .trim()
                        .isEmpty()
        ) {

            throw new RuntimeException(
                    "Product description is required"
            );
        }


        /*
         * PRICE
         */

        if (
                request.getPrice() == null ||
                request.getPrice().signum() <= 0
        ) {

            throw new RuntimeException(
                    "Product price must be greater than zero"
            );
        }


        /*
         * STOCK
         */

        if (
                request.getStock() == null ||
                request.getStock() < 0
        ) {

            throw new RuntimeException(
                    "Product stock cannot be negative"
            );
        }


        /*
         * CATEGORY
         */

        if (
                request.getCategoryId() == null
        ) {

            throw new RuntimeException(
                    "Category is required"
            );
        }


        /*
         * SUBCATEGORY
         */

        if (
                request.getSubCategoryId() == null
        ) {

            throw new RuntimeException(
                    "Subcategory is required"
            );
        }
    }


    /*
     * =====================================================
     * GET CATEGORY
     * =====================================================
     */

    private Category getCategory(
            Long categoryId) {
        Category category = categoryRepository
                .findById(categoryId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Category not found"
                        )
                );
        if (!category.isActive()) throw new RuntimeException("Selected category is inactive");
        return category;
    }


    /*
     * =====================================================
     * GET SUBCATEGORY
     * =====================================================
     */

    private SubCategory getSubCategory(
            Long subCategoryId) {
        SubCategory subCategory = subCategoryRepository
                .findById(subCategoryId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Subcategory not found"
                        )
                );
        if (!subCategory.isActive()) throw new RuntimeException("Selected subcategory is inactive");
        return subCategory;
    }


    /*
     * =====================================================
     * VALIDATE CATEGORY RELATION
     * =====================================================
     */

    private void validateCategoryRelation(
            Category category,
            SubCategory subCategory) {


        if (
                subCategory.getCategory() == null ||
                !subCategory.getCategory()
                        .getId()
                        .equals(
                                category.getId()
                        )
        ) {

            throw new RuntimeException(
                    "Selected subcategory does not belong to selected category"
            );
        }
    }


    /*
     * =====================================================
     * SET COMMON PRODUCT FIELDS
     * =====================================================
     */

    private void setCommonProductFields(
            Product product,
            CreateProductRequest request,
            Category category,
            SubCategory subCategory) {


        product.setName(
                request.getName()
                        .trim()
        );


        product.setDescription(
                request.getDescription()
                        .trim()
        );


        product.setPrice(
                gstCalculator.money(request.getPrice())
        );

        product.setOfferPercentage(
                normalizeOfferPercentage(request.getOfferPercentage())
        );


        product.setStock(
                request.getStock()
        );

        String brand = request.getBrand();
        if (brand == null || brand.isBlank()) brand = inferBrand(product.getName());
        product.setBrand(brand == null ? null : brand.trim().toUpperCase());
        product.setModel(clean(request.getModel()));
        product.setModelNumber(clean(request.getModelNumber()));
        product.setRam(clean(request.getRam()));
        product.setStorage(clean(request.getStorage()));
        product.setColorOptions(clean(request.getColorOptions()));
        product.setHsnCode(clean(request.getHsnCode()));
        product.setSpecificationDetails(clean(request.getSpecificationDetails()));
        product.setProductSpecifications(clean(request.getProductSpecifications()));
        product.setShortHighlights(clean(request.getShortHighlights()));
        product.setGstRate(normalizeGstRate(request.getGstRate()));
        GstBreakup sellingBreakup = gstCalculator.inclusive(product.getPrice(), product.getGstRate());
        product.setSellingTaxablePrice(sellingBreakup.taxableAmount());
        product.setSellingCgst(sellingBreakup.cgst());
        product.setSellingSgst(sellingBreakup.sgst());
        product.setSellingIgst(sellingBreakup.igst());
        product.setSellerSku(clean(request.getSellerSku()));
        product.setBarcode(validateBarcode(request.getBarcode()));
        product.setSellingPriceIncludesGst(true);
        product.setProductType(normalizeChoice(request.getProductType(), "NEW_MOBILE"));
        product.setPhysicalCondition(normalizeChoice(request.getPhysicalCondition(), "NEW"));
        product.setPurchaseTaxTreatment(normalizeChoice(request.getPurchaseTaxTreatment(), "REGULAR_GST"));
        product.setSaleTaxTreatment(normalizeChoice(request.getSaleTaxTreatment(), "REGULAR_GST"));
        product.setAccessoryType(clean(request.getAccessoryType()));
        product.setCompatibility(clean(request.getCompatibility()));
        product.setWarrantyDetails(clean(request.getWarrantyDetails()));
        product.setPackageContents(clean(request.getPackageContents()));
        product.setTaxTreatmentBasis(clean(request.getTaxTreatmentBasis()));
        product.setReorderThreshold(normalizeReorderThreshold(request.getReorderThreshold()));
        product.setSerialTrackingRequired(Boolean.TRUE.equals(request.getSerialTrackingRequired()));


        /*
         * Keep old category field for compatibility.
         */

        product.setCategory(
                category.getName()
        );


        /*
         * New category relationship.
         */

        product.setCategoryEntity(
                category
        );


        /*
         * Subcategory relationship.
         */

        product.setSubCategory(
                subCategory
        );

        validateMobileConfiguration(product, request);
        productConfigurationService.configureNew(product, request);
        if (product.isVariantsEnabled()) {
            GstBreakup variantBase = gstCalculator.inclusive(product.getPrice(), product.getGstRate());
            product.setSellingTaxablePrice(variantBase.taxableAmount());product.setSellingCgst(variantBase.cgst());product.setSellingSgst(variantBase.sgst());product.setSellingIgst(variantBase.igst());
        }
    }

    /**
     * Mobile inventory is owned by the individual listing and its IMEI/serial
     * records. Each colour-RAM-storage configuration must therefore remain a
     * separate product rather than becoming a generic product variant.
     */
    private void validateMobileConfiguration(
            Product product,
            CreateProductRequest request) {

        if (!ProductConfigurationService.isMobile(product)) {
            return;
        }

        if (request.getVariants() != null && !request.getVariants().isEmpty()) {
            throw new RuntimeException(
                    "Mobile variants are managed as separate listings. " +
                    "Create one listing per colour, RAM and storage configuration."
            );
        }

        validateSingleMobileOption("Colour", product.getColorOptions());
        validateSingleMobileOption("RAM", product.getRam());
        validateSingleMobileOption("Storage", product.getStorage());
    }

    private void validateSingleMobileOption(
            String label,
            String value) {

        if (value != null && value.matches(".*[,|/].*")) {
            throw new RuntimeException(
                    label + " must contain one value for a mobile listing. " +
                    "Create a separate listing for each configuration so its price, stock and IMEI stay correct."
            );
        }
    }

    private BigDecimal normalizeOfferPercentage(BigDecimal offerPercentage) {
        if (offerPercentage == null) {
            return null;
        }
        if (offerPercentage.signum() < 0 || offerPercentage.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new RuntimeException("Offer percentage must be between 0 and 100");
        }
        return offerPercentage.signum() == 0 ? null : offerPercentage;
    }

    private String validateBarcode(String barcode) {
        String normalized = clean(barcode);
        if (normalized == null) return null;
        if (normalized.length() > 80) throw new RuntimeException("Product barcode cannot exceed 80 characters");
        if (productRepository.findByBarcodeIgnoreCase(normalized).isPresent()) {
            throw new RuntimeException("This product barcode is already in use");
        }
        return normalized;
    }

    private BigDecimal normalizeGstRate(BigDecimal gstRate) {
        if (gstRate == null) {
            return BigDecimal.ZERO;
        }
        if (gstRate.signum() < 0 || gstRate.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new RuntimeException("GST rate must be between 0 and 100");
        }
        return gstRate;
    }

    private Integer normalizeReorderThreshold(Integer reorderThreshold) {
        if (reorderThreshold == null) {
            return null;
        }
        if (reorderThreshold < 0) {
            throw new RuntimeException("Reorder threshold cannot be negative");
        }
        return reorderThreshold;
    }

    private String normalizeChoice(String value, String fallback) {
        String cleaned = clean(value);
        if (cleaned == null) {
            return fallback;
        }
        return cleaned.trim().toUpperCase().replace(' ', '_').replace('-', '_');
    }


    /*
     * =====================================================
     * GET SELLER PRODUCTS
     * =====================================================
     */

    @Transactional(readOnly = true)
    public List<Product> getSellerProducts(
            Long sellerId) {


        User seller =
                userRepository
                        .findById(sellerId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Seller not found"
                                )
                        );


        return productRepository
                .findBySeller(
                        seller
                );
    }


    /*
     * =====================================================
     * GET ACTIVE PRODUCTS
     * =====================================================
     *
     * Customer sees only:
     *
     * APPROVED
     * +
     * ACTIVE
     *
     * =====================================================
     */

    @Transactional(readOnly = true)
    public List<Product> getActiveProducts() {

        return productRepository
                .findByActiveTrueAndApprovalStatus(
                        ProductStatus.APPROVED
                );
    }

    @Transactional(readOnly = true)
    public List<Product> browseApprovedCatalogue(String query) {
        String search = query == null ? "" : query.trim();
        if (search.isBlank()) {
            return productRepository.findTop30ByApprovalStatusAndActiveTrueOrderByUpdatedAtDesc(
                    ProductStatus.APPROVED
            );
        }

        List<Product> byName =
                productRepository.findTop30ByApprovalStatusAndActiveTrueAndNameContainingIgnoreCaseOrderByUpdatedAtDesc(
                        ProductStatus.APPROVED,
                        search
                );

        List<Product> byBrand =
                productRepository.findTop30ByApprovalStatusAndActiveTrueAndBrandContainingIgnoreCaseOrderByUpdatedAtDesc(
                        ProductStatus.APPROVED,
                        search
                );

        java.util.LinkedHashMap<Long, Product> merged = new java.util.LinkedHashMap<>();
        byName.forEach(product -> merged.put(product.getId(), product));
        byBrand.forEach(product -> merged.put(product.getId(), product));
        return merged.values().stream().limit(30).toList();
    }

    @Transactional
    public Product createSellerListingFromCatalogue(
            CreateProductRequest request,
            Long sellerId) {

        if (request == null || request.getCatalogueProductId() == null) {
            throw new RuntimeException("Approved catalogue product is required");
        }
        if (request.getPrice() == null || request.getPrice().signum() <= 0) {
            throw new RuntimeException("Selling Price (Including GST) must be greater than zero");
        }

        User seller =
                userRepository.findById(sellerId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Seller not found"
                                )
                        );

        if (seller.getRole() == null || !"SELLER".equals(seller.getRole().name())) {
            throw new RuntimeException("Only sellers can create listings");
        }
        if (!seller.isEnabled()) {
            throw new RuntimeException("Seller account is not active");
        }

        Product catalogueProduct =
                productRepository.findById(request.getCatalogueProductId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Catalogue product not found"
                                )
                        );

        if (catalogueProduct.getApprovalStatus() != ProductStatus.APPROVED || !catalogueProduct.isActive()) {
            throw new RuntimeException("Only approved active catalogue products can be listed");
        }

        java.util.Optional<Product> existing =
                productRepository.findBySellerAndCatalogueParent(
                        seller,
                        catalogueProduct
                );

        if (existing.isPresent()) {
            return existing.get();
        }

        Product listing = new Product();
        listing.setName(catalogueProduct.getName());
        listing.setDescription(catalogueProduct.getDescription());
        listing.setBrand(catalogueProduct.getBrand());
        listing.setModel(catalogueProduct.getModel());
        listing.setModelNumber(catalogueProduct.getModelNumber());
        listing.setRam(catalogueProduct.getRam());
        listing.setStorage(catalogueProduct.getStorage());
        listing.setColorOptions(catalogueProduct.getColorOptions());
        listing.setHsnCode(catalogueProduct.getHsnCode());
        listing.setSpecificationDetails(catalogueProduct.getSpecificationDetails());
        listing.setProductSpecifications(catalogueProduct.getProductSpecifications());
        listing.setShortHighlights(catalogueProduct.getShortHighlights());
        listing.setGstRate(catalogueProduct.getGstRate() == null ? normalizeGstRate(request.getGstRate()) : catalogueProduct.getGstRate());
        listing.setCategory(catalogueProduct.getCategory());
        listing.setCategoryEntity(catalogueProduct.getCategoryEntity());
        listing.setSubCategory(catalogueProduct.getSubCategory());
        listing.setImageUrl(catalogueProduct.getImageUrl());
        listing.setSeller(seller);
        listing.setCatalogueParent(catalogueProduct);
        listing.setSellerSku(clean(request.getSellerSku()));
        listing.setPrice(gstCalculator.money(request.getPrice()));
        listing.setSellingPriceIncludesGst(true);
        GstBreakup listingBreakup = gstCalculator.inclusive(listing.getPrice(), listing.getGstRate());
        listing.setSellingTaxablePrice(listingBreakup.taxableAmount());
        listing.setSellingCgst(listingBreakup.cgst());
        listing.setSellingSgst(listingBreakup.sgst());
        listing.setSellingIgst(listingBreakup.igst());
        listing.setOfferPercentage(null);
        listing.setStock(0);
        listing.setReservedStock(0);
        listing.setSource(ProductSource.SELLER);
        listing.setApprovalStatus(ProductStatus.APPROVED);
        listing.setActive(true);
        listing.setAdminReview("Seller listing created from approved catalogue product #" + catalogueProduct.getId());
        listing.setReviewedAt(java.time.LocalDateTime.now());

        Product saved = productRepository.save(listing);
        productImageService.copyProductImages(catalogueProduct.getId(), saved.getId());
        return saved;
    }


    /*
     * =====================================================
     * GET PRODUCT BY ID
     * =====================================================
     */

    @Transactional(readOnly = true)
    public Product getProductById(
            Long productId) {

        // The customer product page renders gallery/category/seller panels from
        // the same approved listing. Load those relations together so the
        // public detail response remains stable outside an open persistence
        // session (and does not issue one query per image/seller field).
        return productRepository
                .findDetailedById(productId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Product not found"
                        )
                );
    }


    /*
     * =====================================================
     * UPDATE PRODUCT
     * =====================================================
     */

    @Transactional
    public Product updateProduct(
            Long productId,
            Product updatedProduct,
            Long sellerId) {


        Product existingProduct =
                productRepository
                        .findById(productId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Product not found"
                                )
                        );


        /*
         * =================================================
         * OWNERSHIP CHECK
         * =================================================
         */

        if (
                existingProduct.getSeller() == null ||
                !existingProduct.getSeller()
                        .getId()
                        .equals(
                                sellerId
                        )
        ) {

            throw new RuntimeException(
                    "You are not allowed to update this product"
            );
        }


        /*
         * =================================================
         * UPDATE BASIC FIELDS
         * =================================================
         */

        existingProduct.setName(
                updatedProduct.getName()
        );


        existingProduct.setDescription(
                updatedProduct.getDescription()
        );


        if (!existingProduct.isVariantsEnabled()) existingProduct.setPrice(
                gstCalculator.money(updatedProduct.getPrice())
        );

        if (updatedProduct.getGstRate() != null) {
            existingProduct.setGstRate(normalizeGstRate(updatedProduct.getGstRate()));
        }

        GstBreakup updateBreakup = gstCalculator.inclusive(existingProduct.getPrice(), existingProduct.getGstRate());
        existingProduct.setSellingTaxablePrice(updateBreakup.taxableAmount());
        existingProduct.setSellingCgst(updateBreakup.cgst());
        existingProduct.setSellingSgst(updateBreakup.sgst());
        existingProduct.setSellingIgst(updateBreakup.igst());

        if (!existingProduct.isVariantsEnabled()) existingProduct.setStock(
                updatedProduct.getStock()
        );


        existingProduct.setCategory(
                updatedProduct.getCategory()
        );

        String brand = updatedProduct.getBrand();
        if (brand == null || brand.isBlank()) brand = inferBrand(updatedProduct.getName());
        existingProduct.setBrand(brand == null ? null : brand.trim().toUpperCase());


        existingProduct.setImageUrl(
                updatedProduct.getImageUrl()
        );


        /*
         * =================================================
         * REQUIRE NEW APPROVAL
         * =================================================
         */

        existingProduct.setApprovalStatus(
                ProductStatus.PENDING
        );


        existingProduct.setActive(
                false
        );


        existingProduct.setAdminReview(
                null
        );


        existingProduct.setReviewedAt(
                null
        );


        return productRepository.save(
                existingProduct
        );
    }


    /*
     * =====================================================
     * DELETE PRODUCT
     * =====================================================
     */

    @Transactional
    public void deleteProduct(
            Long productId,
            Long sellerId) {


        Product product =
                productRepository
                        .findById(productId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Product not found"
                                )
                        );


        /*
         * =================================================
         * OWNERSHIP CHECK
         * =================================================
         */

        if (
                product.getSeller() == null ||
                !product.getSeller()
                        .getId()
                        .equals(
                                sellerId
                        )
        ) {

            throw new RuntimeException(
                    "You are not allowed to delete this product"
            );
        }


        productRepository.delete(
                product
        );
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** Covers common mobile naming, including iPhone written as "i Phone". */
    private String inferBrand(String productName) {
        if (productName == null) return null;
        String value = productName.trim().toUpperCase();
        if (value.startsWith("IPHONE") || value.startsWith("I PHONE") || value.startsWith("IPAD") || value.startsWith("MACBOOK")) return "APPLE";
        if (value.startsWith("REDMI") || value.startsWith("POCO") || value.startsWith("XIAOMI")) return "XIAOMI";
        if (value.startsWith("GOOGLE") || value.startsWith("PIXEL")) return "GOOGLE PIXEL";
        for (String brand : new String[]{"SAMSUNG", "APPLE", "VIVO", "OPPO", "REALME", "ONEPLUS", "MOTOROLA", "NOTHING"}) {
            if (value.startsWith(brand)) return brand;
        }
        return null;
    }
}
