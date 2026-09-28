package com.shivhub.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.shivhub.backend.entity.ProductImage;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.repository.UserRepository;
import com.shivhub.backend.service.ProductImageService;


/*
 * =========================================================
 * ProductImageController
 * =========================================================
 *
 * Handles Product Image APIs.
 *
 *
 * PRODUCT IMAGES:
 *
 * Seller can upload normal product images within the configured product image limit.
 *
 *
 * DESCRIPTION IMAGE:
 *
 * Seller can additionally upload description images.
 *
 *
 * GET IMAGES:
 *
 * Customer can fetch product images.
 *
 * =========================================================
 */

@RestController
@RequestMapping("/api/product-images")
@CrossOrigin(origins = "http://localhost:5173")
public class ProductImageController {


    /*
     * =====================================================
     * SERVICES
     * =====================================================
     */

    private final ProductImageService productImageService;

    private final UserRepository userRepository;


    /*
     * =====================================================
     * CONSTRUCTOR
     * =====================================================
     */

    public ProductImageController(
            ProductImageService productImageService,
            UserRepository userRepository) {

        this.productImageService =
                productImageService;

        this.userRepository =
                userRepository;
    }


    /*
     * =====================================================
     * UPLOAD PRODUCT IMAGES
     * =====================================================
     *
     * POST:
     *
     * /api/product-images/product/{productId}
     *
     *
     * Content-Type:
     *
     * multipart/form-data
     *
     *
     * Request:
     *
     * images = 5 image files
     *
     *
     * IMPORTANT:
     *
     * Exactly 5 PRODUCT images are required when
     * creating a product.
     *
     * =====================================================
     */

    @PostMapping(
            value = "/product/{productId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<List<ProductImage>> uploadProductImages(

            @PathVariable Long productId,

            @RequestPart("images")
            MultipartFile[] images,

            Authentication authentication) {


        /*
         * =================================================
         * CHECK LOGIN
         * =================================================
         */

        if (authentication == null ||
                authentication.getName() == null) {

            throw new RuntimeException(
                    "Authentication required"
            );
        }


        /*
         * =================================================
         * CHECK SELLER
         * =================================================
         *
         * ProductImageController is used by sellers.
         *
         * We make sure the logged-in user exists.
         *
         * Product ownership validation should be handled
         * in the ProductService / ProductImageService layer
         * as the project grows.
         *
         * =================================================
         */

        String email =
                authentication.getName();


        userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found"
                        )
                );


        /*
         * =================================================
         * UPLOAD IMAGES
         * =================================================
         */

        List<ProductImage> savedImages =
                productImageService.uploadProductImages(
                        productId,
                        images
                );


        /*
         * =================================================
         * RETURN 201 CREATED
         * =================================================
         */

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedImages);
    }


    /*
     * =====================================================
     * UPLOAD DESCRIPTION IMAGE
     * =====================================================
     *
     * POST:
     *
     * /api/product-images/product/{productId}/description
     *
     *
     * Content-Type:
     *
     * multipart/form-data
     *
     *
     * Request:
     *
     * image = one description image
     *
     *
     * These images are separate from the normal 5
     * PRODUCT images.
     *
     * =====================================================
     */

    @PostMapping(
            value = "/product/{productId}/description",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ProductImage> uploadDescriptionImage(

            @PathVariable Long productId,

            @RequestPart("image")
            MultipartFile image,

            Authentication authentication) {


        /*
         * =================================================
         * CHECK LOGIN
         * =================================================
         */

        if (authentication == null ||
                authentication.getName() == null) {

            throw new RuntimeException(
                    "Authentication required"
            );
        }


        /*
         * =================================================
         * CHECK USER
         * =================================================
         */

        String email =
                authentication.getName();


        userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found"
                        )
                );


        /*
         * =================================================
         * UPLOAD DESCRIPTION IMAGE
         * =================================================
         */

        ProductImage savedImage =
                productImageService.uploadDescriptionImage(
                        productId,
                        image
                );


        /*
         * =================================================
         * RETURN CREATED IMAGE
         * =================================================
         */

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedImage);
    }


    /*
     * =====================================================
     * GET ALL PRODUCT IMAGES
     * =====================================================
     *
     * GET:
     *
     * /api/product-images/product/{productId}
     *
     *
     * Used by:
     *
     * Customer Product Details Page
     *
     * Seller Product Details Page
     *
     * =====================================================
     */

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ProductImage>> getProductImages(

            @PathVariable Long productId) {


        /*
         * Get all images belonging to product.
         */

        List<ProductImage> images =
                productImageService.getProductImages(
                        productId
                );


        return ResponseEntity.ok(
                images
        );
    }


    /*
     * =====================================================
     * GET NORMAL PRODUCT IMAGES
     * =====================================================
     *
     * GET:
     *
     * /api/product-images/product/{productId}/normal
     *
     *
     * Returns only:
     *
     * ImageType.PRODUCT
     *
     * So DESCRIPTION images are not included.
     *
     * =====================================================
     */

    @GetMapping("/product/{productId}/normal")
    public ResponseEntity<List<ProductImage>>
    getNormalProductImages(

            @PathVariable Long productId) {


        /*
         * Get only normal PRODUCT images.
         */

        List<ProductImage> images =
                productImageService.getNormalProductImages(
                        productId
                );


        return ResponseEntity.ok(
                images
        );
    }
}
