package com.shivhub.backend.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.shivhub.backend.dto.CreateProductRequest;
import com.shivhub.backend.entity.Product;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.repository.UserRepository;
import com.shivhub.backend.service.ProductService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "http://localhost:5173")
public class ProductController {

    private final ProductService productService;

    private final UserRepository userRepository;


    public ProductController(
            ProductService productService,
            UserRepository userRepository) {

        this.productService = productService;
        this.userRepository = userRepository;
    }


    /*
     * =========================================================
     * CREATE SELLER PRODUCT
     * =========================================================
     *
     * POST:
     *
     * /api/products/seller
     *
     * Content-Type:
     *
     * multipart/form-data
     *
     *
     * Parts:
     *
     * product
     *     -> CreateProductRequest JSON
     *
     * images
     *     -> local computer images
     *
     * imageUrls
     *     -> JSON array of external image URLs
     *
     *
     * Total:
     *
     * local images + URL images = at least 5 images
     *
     *
     * Example:
     *
     * 3 computer images
     * +
     * 2 image URLs
     * =
     * 5 images
     *
     *
     * Seller product starts:
     *
     * PENDING
     *
     * active = false
     *
     * Admin must approve it.
     *
     * =========================================================
     */

    @PostMapping(
            value = "/seller",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Product> createSellerProduct(

            @RequestPart("product")
            @Valid
            CreateProductRequest request,

            @RequestPart(
                    value = "images",
                    required = false
            )
            MultipartFile[] images,

            @RequestPart(
                    value = "imageUrls",
                    required = false
            )
            List<String> imageUrls,

            Authentication authentication) {


        /*
         * =====================================================
         * CHECK AUTHENTICATION
         * =====================================================
         */

        if (authentication == null ||
                authentication.getName() == null) {

            throw new RuntimeException(
                    "Authentication required"
            );
        }


        /*
         * =====================================================
         * GET LOGGED-IN SELLER
         * =====================================================
         */

        String email =
                authentication.getName();


        User seller =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Seller not found"
                                )
                        );


        /*
         * =====================================================
         * CHECK SELLER ROLE
         * =====================================================
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
         * =====================================================
         * SAFE DEFAULT FOR IMAGE URLS
         * =====================================================
         */

        if (imageUrls == null) {

            imageUrls =
                    Collections.emptyList();
        }


        /*
         * =====================================================
         * SAFE DEFAULT FOR FILES
         * =====================================================
         */

        if (images == null) {

            images =
                    new MultipartFile[0];
        }


        /*
         * =====================================================
         * PRODUCT IMAGE VALIDATION
         * =====================================================
         */

        int totalImages =
                images.length +
                imageUrls.size();


        if (totalImages < 5 || totalImages > 15) {

            throw new RuntimeException(
                    "Provide between 5 and 15 product images. Use computer upload or image URL."
            );
        }


        /*
         * =====================================================
         * CREATE PRODUCT
         * =====================================================
         */

        Product savedProduct =
                productService.createProduct(
                        request,
                        seller.getId(),
                        images,
                        imageUrls
                );


        /*
         * =====================================================
         * RESPONSE
         * =====================================================
         */

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedProduct);
    }


    /*
     * =========================================================
     * GET ALL ACTIVE PRODUCTS
     * =========================================================
     *
     * GET:
     *
     * /api/products
     *
     * =========================================================
     */

    @GetMapping
    public ResponseEntity<List<Product>>
    getActiveProducts() {

        List<Product> products =
                productService.getActiveProducts();

        return ResponseEntity.ok(products);
    }

    @GetMapping("/catalogue")
    public ResponseEntity<List<Product>> browseApprovedCatalogue(
            @RequestParam(value = "q", required = false) String query) {

        return ResponseEntity.ok(
                productService.browseApprovedCatalogue(query)
        );
    }

    @PostMapping("/seller/list-from-catalogue")
    public ResponseEntity<Product> createSellerListingFromCatalogue(
            @RequestBody CreateProductRequest request,
            Authentication authentication) {

        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Authentication required");
        }

        User seller =
                userRepository
                        .findByEmail(authentication.getName())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Seller not found"
                                )
                        );

        if (seller.getRole() == null || !"SELLER".equals(seller.getRole().name())) {
            throw new RuntimeException("Only sellers can create listings");
        }

        Product saved =
                productService.createSellerListingFromCatalogue(
                        request,
                        seller.getId()
                );

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }


    /*
     * =========================================================
     * GET PRODUCT BY ID
     * =========================================================
     *
     * GET:
     *
     * /api/products/{id}
     *
     * =========================================================
     */

    @GetMapping("/{id}")
    public ResponseEntity<Product>
    getProductById(
            @PathVariable Long id) {

        Product product =
                productService.getProductById(id);

        return ResponseEntity.ok(product);
    }


    /*
     * =========================================================
     * GET SELLER PRODUCTS
     * =========================================================
     *
     * GET:
     *
     * /api/products/seller
     *
     * =========================================================
     */

    @GetMapping("/seller")
    public ResponseEntity<List<Product>>
    getSellerProducts(
            Authentication authentication) {


        if (authentication == null ||
                authentication.getName() == null) {

            throw new RuntimeException(
                    "Authentication required"
            );
        }


        String email =
                authentication.getName();


        User seller =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Seller not found"
                                )
                        );


        if (seller.getRole() == null ||
                !"SELLER".equals(
                        seller.getRole().name()
                )) {

            throw new RuntimeException(
                    "Only sellers can access seller products"
            );
        }


        List<Product> products =
                productService.getSellerProducts(
                        seller.getId()
                );


        return ResponseEntity.ok(products);
    }


    /*
     * =========================================================
     * UPDATE PRODUCT
     * =========================================================
     *
     * PUT:
     *
     * /api/products/{id}
     *
     * =========================================================
     */

    @PutMapping("/{id}")
    public ResponseEntity<Product>
    updateProduct(

            @PathVariable Long id,

            @Valid
            @RequestBody Product updatedProduct,

            Authentication authentication) {


        if (authentication == null ||
                authentication.getName() == null) {

            throw new RuntimeException(
                    "Authentication required"
            );
        }


        String email =
                authentication.getName();


        User seller =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Seller not found"
                                )
                        );


        if (seller.getRole() == null ||
                !"SELLER".equals(
                        seller.getRole().name()
                )) {

            throw new RuntimeException(
                    "Only sellers can update products"
            );
        }


        Product updated =
                productService.updateProduct(
                        id,
                        updatedProduct,
                        seller.getId()
                );


        return ResponseEntity.ok(updated);
    }


    /*
     * =========================================================
     * DELETE PRODUCT
     * =========================================================
     *
     * DELETE:
     *
     * /api/products/{id}
     *
     * =========================================================
     */

    @DeleteMapping("/{id}")
    public ResponseEntity<Void>
    deleteProduct(

            @PathVariable Long id,

            Authentication authentication) {


        if (authentication == null ||
                authentication.getName() == null) {

            throw new RuntimeException(
                    "Authentication required"
            );
        }


        String email =
                authentication.getName();


        User seller =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Seller not found"
                                )
                        );


        if (seller.getRole() == null ||
                !"SELLER".equals(
                        seller.getRole().name()
                )) {

            throw new RuntimeException(
                    "Only sellers can delete products"
            );
        }


        productService.deleteProduct(
                id,
                seller.getId()
        );


        return ResponseEntity
                .noContent()
                .build();
    }
}
