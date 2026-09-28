package com.shivhub.backend.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.shivhub.backend.entity.Product;
import com.shivhub.backend.entity.ProductImage;
import com.shivhub.backend.enums.ImageType;
import com.shivhub.backend.repository.ProductImageRepository;
import com.shivhub.backend.repository.ProductRepository;


/*
 * =========================================================
 * ProductImageService
 * =========================================================
 *
 * Handles:
 *
 * 1. Uploading normal product images
 * 2. External image URLs
 * 3. Computer + URL combination
 * 4. Description images
 * 5. Image validation
 * 6. Getting product images
 *
 * =========================================================
 */

@Service
public class ProductImageService {


    private static final int MIN_PRODUCT_IMAGES = 5;
    private static final int MAX_PRODUCT_IMAGES = 15;


    private final ProductImageRepository productImageRepository;

    private final ProductRepository productRepository;

    private final FileStorageService fileStorageService;


    public ProductImageService(
            ProductImageRepository productImageRepository,
            ProductRepository productRepository,
            FileStorageService fileStorageService) {

        this.productImageRepository =
                productImageRepository;

        this.productRepository =
                productRepository;

        this.fileStorageService =
                fileStorageService;
    }


    /*
     * =====================================================
     * EXISTING SELLER IMAGE UPLOAD
     * =====================================================
     *
     * IMPORTANT:
     *
     * Existing Seller flow is preserved.
     *
     * At least 5 local files.
     *
     * =====================================================
     */

    @Transactional
    public List<ProductImage> uploadProductImages(
            Long productId,
            MultipartFile[] files) {

        if (productId == null) {

            throw new RuntimeException(
                    "Product ID is required"
            );
        }


        if (files == null) {

            throw new RuntimeException(
                    "Product images are required"
            );
        }


        if (files.length < MIN_PRODUCT_IMAGES || files.length > MAX_PRODUCT_IMAGES) {

            throw new RuntimeException(
                    "Provide between 5 and 15 product images"
            );
        }


        Product product =
                productRepository.findById(productId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Product not found"
                                )
                        );


        long existingImageCount =
                productImageRepository
                        .countByProductIdAndImageType(
                                productId,
                                ImageType.PRODUCT
                        );


        if (existingImageCount + files.length
                > MAX_PRODUCT_IMAGES) {

            throw new RuntimeException(
                    "A product can have maximum 15 product images"
            );
        }


        for (MultipartFile file : files) {

            validateImage(file);
        }


        List<ProductImage> savedImages =
                new ArrayList<>();


        int displayOrder =
                (int) existingImageCount + 1;


        for (MultipartFile file : files) {

            String imagePath =
                    fileStorageService.storeProductImage(
                            productId,
                            file
                    );


            ProductImage productImage =
                    saveProductImage(
                            product,
                            displayOrder++,
                            imagePath
                    );


            savedImages.add(productImage);
        }


        return savedImages;
    }


    /*
     * =====================================================
     * CREATE PRODUCT IMAGES
     * =====================================================
     *
     * Supports:
     *
     * Computer files
     * +
     * External URLs
     *
     * Total must be between 5 and 15.
     *
     * Existing Seller implementation uses this method.
     *
     * =====================================================
     */

    @Transactional
    public List<ProductImage> createProductImages(
            Long productId,
            MultipartFile[] files,
            List<String> externalImageUrls) {

        MultipartFile[] uploadedFiles =
                files == null
                        ? new MultipartFile[0]
                        : files;


        List<String> urls =
                externalImageUrls == null
                        ? List.of()
                        : externalImageUrls;


        int totalImages = uploadedFiles.length + urls.size();

        if (totalImages < MIN_PRODUCT_IMAGES || totalImages > MAX_PRODUCT_IMAGES) {

            throw new RuntimeException(
                    "Provide between 5 and 15 product images"
            );
        }


        Product product =
                productRepository.findById(productId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Product not found"
                                )
                        );


        for (MultipartFile file : uploadedFiles) {

            validateImage(file);
        }


        for (String imageUrl : urls) {

            validateExternalImageUrl(imageUrl);
        }


        List<ProductImage> savedImages =
                new ArrayList<>();


        int displayOrder = 1;


        /*
         * Local images.
         */

        for (MultipartFile file : uploadedFiles) {

            String imagePath =
                    fileStorageService.storeProductImage(
                            productId,
                            file
                    );


            savedImages.add(
                    saveProductImage(
                            product,
                            displayOrder++,
                            imagePath
                    )
            );
        }


        /*
         * External URLs.
         */

        for (String imageUrl : urls) {

            savedImages.add(
                    saveProductImage(
                            product,
                            displayOrder++,
                            imageUrl.trim()
                    )
            );
        }


        return savedImages;
    }

    @Transactional
    public List<ProductImage> copyProductImages(
            Long sourceProductId,
            Long targetProductId) {

        Product target =
                productRepository.findById(targetProductId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Target product not found"
                                )
                        );

        List<ProductImage> sourceImages =
                productImageRepository.findByProductIdOrderByDisplayOrderAsc(
                        sourceProductId
                );

        List<ProductImage> copiedImages =
                new ArrayList<>();

        int displayOrder = 1;

        for (ProductImage sourceImage : sourceImages) {
            copiedImages.add(
                    saveProductImage(
                            target,
                            displayOrder++,
                            sourceImage.getImageUrl()
                    )
            );
        }

        return copiedImages;
    }


    /*
     * =====================================================
     * SAVE PRODUCT IMAGE
     * =====================================================
     */

    private ProductImage saveProductImage(
            Product product,
            int displayOrder,
            String imageUrl) {

        ProductImage productImage =
                new ProductImage();

        productImage.setImageUrl(
                imageUrl
        );

        productImage.setImageType(
                ImageType.PRODUCT
        );

        productImage.setDisplayOrder(
                displayOrder
        );

        productImage.setProduct(
                product
        );

        return productImageRepository.save(
                productImage
        );
    }


    /*
     * =====================================================
     * EXTERNAL URL VALIDATION
     * =====================================================
     */

    private void validateExternalImageUrl(
            String imageUrl) {

        if (imageUrl == null ||
                imageUrl.isBlank() ||
                imageUrl.length() > 2000) {

            throw new RuntimeException(
                    "Enter a valid image URL"
            );
        }


        try {

            URI uri =
                    new URI(
                            imageUrl.trim()
                    );


            if (!uri.isAbsolute() ||
                    uri.getHost() == null ||
                    !(
                            "http".equalsIgnoreCase(
                                    uri.getScheme()
                            )
                            ||
                            "https".equalsIgnoreCase(
                                    uri.getScheme()
                            )
                    )) {

                throw new RuntimeException(
                        "Image URL must start with http:// or https://"
                );
            }

        } catch (URISyntaxException exception) {

            throw new RuntimeException(
                    "Enter a valid image URL"
            );
        }
    }


    /*
     * =====================================================
     * DESCRIPTION IMAGE
     * =====================================================
     */

    @Transactional
    public ProductImage uploadDescriptionImage(
            Long productId,
            MultipartFile image) {

        Product product =
                productRepository.findById(productId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Product not found"
                                )
                        );


        validateImage(image);


        String imagePath =
                fileStorageService.storeProductImage(
                        productId,
                        image
                );


        ProductImage productImage =
                new ProductImage();


        productImage.setImageUrl(
                imagePath
        );

        productImage.setImageType(
                ImageType.DESCRIPTION
        );

        productImage.setDisplayOrder(
                null
        );

        productImage.setProduct(
                product
        );


        return productImageRepository.save(
                productImage
        );
    }


    /*
     * =====================================================
     * VALIDATE IMAGE
     * =====================================================
     */

    private void validateImage(
            MultipartFile file) {

        if (file == null ||
                file.isEmpty()) {

            throw new RuntimeException(
                    "Uploaded image cannot be empty"
            );
        }


        String contentType =
                file.getContentType();


        if (contentType == null ||
                !contentType.startsWith("image/")) {

            throw new RuntimeException(
                    "Only image files are allowed"
            );
        }


        if (
                !contentType.equalsIgnoreCase(
                        "image/jpeg"
                )
                &&
                !contentType.equalsIgnoreCase(
                        "image/png"
                )
                &&
                !contentType.equalsIgnoreCase(
                        "image/webp"
                )
        ) {

            throw new RuntimeException(
                    "Only JPG, PNG and WEBP images are allowed"
            );
        }
    }


    /*
     * =====================================================
     * GET ALL PRODUCT IMAGES
     * =====================================================
     */

    @Transactional(readOnly = true)
    public List<ProductImage> getProductImages(
            Long productId) {

        productRepository.findById(
                productId
        )
        .orElseThrow(() ->
                new RuntimeException(
                        "Product not found"
                )
        );


        return productImageRepository
                .findByProductIdOrderByDisplayOrderAsc(
                        productId
                );
    }


    /*
     * =====================================================
     * GET NORMAL PRODUCT IMAGES
     * =====================================================
     */

    @Transactional(readOnly = true)
    public List<ProductImage> getNormalProductImages(
            Long productId) {

        productRepository.findById(
                productId
        )
        .orElseThrow(() ->
                new RuntimeException(
                        "Product not found"
                )
        );


        return productImageRepository
                .findByProductIdAndImageType(
                        productId,
                        ImageType.PRODUCT
                );
    }
}
