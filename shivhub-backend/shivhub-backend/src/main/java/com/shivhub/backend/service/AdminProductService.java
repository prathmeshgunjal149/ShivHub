package com.shivhub.backend.service;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.shivhub.backend.dto.CreateProductRequest;
import com.shivhub.backend.entity.Category;
import com.shivhub.backend.entity.Product;
import com.shivhub.backend.entity.ProductImage;
import com.shivhub.backend.entity.SubCategory;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.ImageType;
import com.shivhub.backend.enums.ProductStatus;
import com.shivhub.backend.repository.CategoryRepository;
import com.shivhub.backend.repository.ProductRepository;
import com.shivhub.backend.repository.PurchaseRepository;
import com.shivhub.backend.repository.SubCategoryRepository;
import com.shivhub.backend.repository.UserRepository;

@Service
public class AdminProductService {

    private final ProductRepository productRepository;
    private final PurchaseRepository purchaseRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final ProductImageService productImageService;
    private final EmailService emailService;

    public AdminProductService(
            ProductRepository productRepository,
            PurchaseRepository purchaseRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            SubCategoryRepository subCategoryRepository,
            ProductImageService productImageService,
            EmailService emailService) {

        this.productRepository = productRepository;
        this.purchaseRepository = purchaseRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
        this.productImageService = productImageService;
        this.emailService = emailService;
    }


    /*
     * =====================================================
     * CREATE ADMIN PRODUCT
     * =====================================================
     */

    @Transactional
    public Product createAdminProduct(

            CreateProductRequest request,

            MultipartFile[] files,

            List<String> imageUrls,

            String adminEmail) {


        /*
         * VALIDATE REQUEST
         */

        if (request == null) {
            throw new RuntimeException(
                    "Product data is required"
            );
        }


        if (request.getName() == null ||
                request.getName().trim().isEmpty()) {

            throw new RuntimeException(
                    "Product name is required"
            );
        }


        if (request.getPrice() == null ||
                request.getPrice().signum() < 0) {

            throw new RuntimeException(
                    "Valid price is required"
            );
        }

        if (request.getOfferPercentage() != null &&
                (request.getOfferPercentage().signum() < 0 ||
                        request.getOfferPercentage().compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new RuntimeException("Offer discount must be between 0 and 100%");
        }


        if (request.getStock() == null ||
                request.getStock() < 0) {

            throw new RuntimeException(
                    "Valid stock is required"
            );
        }


        /*
         * =================================================
         * ADMIN
         * =================================================
         */

        User admin =
                userRepository
                        .findByEmail(adminEmail)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Admin user not found"
                                )
                        );


        /*
         * =================================================
         * CATEGORY
         * =================================================
         */

        Category category = null;

        if (request.getCategoryId() != null) {

            category =
                    categoryRepository
                            .findById(
                                    request.getCategoryId()
                            )
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Category not found"
                                    )
                            );

            if (!category.isActive()) {

                throw new RuntimeException(
                        "Selected category is inactive"
                );
            }
        }


        /*
         * =================================================
         * SUBCATEGORY
         * =================================================
         */

        SubCategory subCategory = null;

        if (request.getSubCategoryId() != null) {

            subCategory =
                    subCategoryRepository
                            .findById(
                                    request.getSubCategoryId()
                            )
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Subcategory not found"
                                    )
                            );

            if (!subCategory.isActive()) {

                throw new RuntimeException(
                        "Selected subcategory is inactive"
                );
            }


            if (category != null &&
                    subCategory.getCategory() != null &&
                    !subCategory
                            .getCategory()
                            .getId()
                            .equals(
                                    category.getId()
                            )) {

                throw new RuntimeException(
                        "Subcategory does not belong to selected category"
                );
            }
        }


        /*
         * =================================================
         * IMAGE COUNT
         * =================================================
         *
         * Admin can use:
         *
         * Local files
         * +
         * External URLs
         *
         * Total must be between 5 and 15.
         *
         * =================================================
         */

        int fileCount =
                files == null
                        ? 0
                        : files.length;

        int urlCount =
                imageUrls == null
                        ? 0
                        : imageUrls.size();

        int totalImages = fileCount + urlCount;

        if (totalImages < 5 || totalImages > 15) {

            throw new RuntimeException(
                    "Provide between 5 and 15 product images"
            );
        }


        /*
         * =================================================
         * CREATE PRODUCT
         * =================================================
         */

        Product product =
                new Product();


        product.setName(
                request.getName().trim()
        );


        product.setDescription(
                request.getDescription()
        );


        product.setPrice(
                request.getPrice()
        );

        product.setOfferPercentage(
                request.getOfferPercentage() != null && request.getOfferPercentage().signum() > 0
                        ? request.getOfferPercentage()
                        : null
        );


        product.setStock(
                request.getStock()
        );


        /*
         * CATEGORY
         */

        if (category != null) {

            product.setCategoryEntity(
                    category
            );

            /*
             * Old category field kept for compatibility.
             */

            product.setCategory(
                    category.getName()
            );
        }


        /*
         * SUBCATEGORY
         */

        if (subCategory != null) {

            product.setSubCategory(
                    subCategory
            );
        }


        /*
         * ADMIN IS OWNER
         *
         * Product.seller is nullable=false.
         */

        product.setSeller(
                admin
        );


        /*
         * =================================================
         * ADMIN PRODUCT IS DIRECTLY APPROVED
         * =================================================
         */

        product.setApprovalStatus(
                ProductStatus.APPROVED
        );


        product.setActive(
                true
        );


        product.setAdminReview(
                "Product added and approved by ShivHub Admin."
        );


        product.setReviewedAt(
                LocalDateTime.now()
        );


        /*
         * =================================================
         * SAVE PRODUCT FIRST
         * =================================================
         *
         * Product ID is required before storing
         * uploaded files.
         *
         * =================================================
         */

        Product savedProduct =
                productRepository.save(
                        product
                );


        /*
         * =================================================
         * SAVE 5 PRODUCT IMAGES
         * =================================================
         *
         * Existing ProductImageService supports:
         *
         * local computer files
         * +
         * external URLs
         *
         * 5 to 15 total.
         *
         * =================================================
         */

        List<ProductImage> savedImages =
                productImageService.createProductImages(

                        savedProduct.getId(),

                        files,

                        imageUrls
                );


        /*
         * =================================================
         * SET OLD IMAGE URL
         * =================================================
         *
         * Existing frontend/database compatibility.
         *
         * =================================================
         */

        if (savedImages != null &&
                !savedImages.isEmpty()) {

            savedProduct.setImageUrl(
                    savedImages
                            .get(0)
                            .getImageUrl()
            );

            savedProduct =
                    productRepository.save(
                            savedProduct
                    );
        }


        return savedProduct;
    }


    /*
     * =====================================================
     * GET ALL PRODUCTS
     * =====================================================
     */

    @Transactional(readOnly = true)
    public List<Product> getProducts() {

        return productRepository.findAll();
    }


    /*
     * =====================================================
     * GET PENDING PRODUCTS
     * =====================================================
     */

    @Transactional(readOnly = true)
    public List<Product> getPendingProducts() {

        return productRepository
                .findByApprovalStatus(ProductStatus.PENDING)
                .stream()
                .filter(product -> !purchaseRepository.hasCompletedPurchaseForProduct(product.getId()))
                .toList();
    }


    /*
     * =====================================================
     * GET PRODUCT
     * =====================================================
     */

    @Transactional(readOnly = true)
    public Product getProduct(
            Long productId) {

        return productRepository
                .findDetailedById(productId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Product not found"
                        )
                );
    }

    /** Admin-only product offer. Null or zero removes the current discount. */
    @Transactional
    public Product updateOfferPercentage(Long productId, BigDecimal offerPercentage) {
        if (offerPercentage != null &&
                (offerPercentage.signum() < 0 || offerPercentage.compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new RuntimeException("Offer discount must be between 0 and 100%");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setOfferPercentage(offerPercentage == null || offerPercentage.signum() == 0 ? null : offerPercentage);
        return productRepository.save(product);
    }


    /*
     * =====================================================
     * APPROVE SELLER PRODUCT
     * =====================================================
     */

    @Transactional
    public Product approveProduct(

            Long productId,

            String adminReview) {


        Product product =
                productRepository
                        .findById(productId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Product not found"
                                )
                        );


        if (product.getApprovalStatus()
                != ProductStatus.PENDING) {

            throw new RuntimeException(
                    "Only pending products can be approved"
            );
        }


        String reviewMessage =
                adminReview;


        if (reviewMessage == null ||
                reviewMessage.trim().isEmpty()) {

            reviewMessage =
                    "Product approved by ShivHub Admin.";
        }


        product.setApprovalStatus(
                ProductStatus.APPROVED
        );


        product.setActive(
                true
        );


        product.setAdminReview(
                reviewMessage.trim()
        );


        product.setReviewedAt(
                LocalDateTime.now()
        );


        Product savedProduct =
                productRepository.save(
                        product
                );


        /*
         * EMAIL SELLER
         */

        User seller =
                product.getSeller();


        if (seller != null &&
                seller.getEmail() != null &&
                !seller.getEmail()
                        .trim()
                        .isEmpty()) {

            emailService.sendProductApprovedEmail(

                    seller.getEmail(),

                    product.getName(),

                    reviewMessage.trim()
            );
        }


        return savedProduct;
    }


    /*
     * =====================================================
     * REJECT SELLER PRODUCT
     * =====================================================
     */

    @Transactional
    public Product rejectProduct(

            Long productId,

            String rejectionReason) {


        if (rejectionReason == null ||
                rejectionReason.trim().isEmpty()) {

            throw new RuntimeException(
                    "Rejection reason is required"
            );
        }


        Product product =
                productRepository
                        .findById(productId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Product not found"
                                )
                        );


        if (product.getApprovalStatus()
                != ProductStatus.PENDING) {

            throw new RuntimeException(
                    "Only pending products can be rejected"
            );
        }


        String reason =
                rejectionReason.trim();


        product.setApprovalStatus(
                ProductStatus.REJECTED
        );


        product.setActive(
                false
        );


        product.setAdminReview(
                reason
        );


        product.setReviewedAt(
                LocalDateTime.now()
        );


        Product savedProduct =
                productRepository.save(
                        product
                );


        /*
         * EMAIL SELLER
         */

        User seller =
                product.getSeller();


        if (seller != null &&
                seller.getEmail() != null &&
                !seller.getEmail()
                        .trim()
                        .isEmpty()) {

            emailService.sendProductRejectedEmail(

                    seller.getEmail(),

                    product.getName(),

                    reason
            );
        }


        return savedProduct;
    }
}
