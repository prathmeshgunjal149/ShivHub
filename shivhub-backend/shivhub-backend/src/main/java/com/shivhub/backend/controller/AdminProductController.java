package com.shivhub.backend.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.shivhub.backend.dto.CreateProductRequest;
import com.shivhub.backend.entity.Product;
import com.shivhub.backend.service.AdminProductService;

@RestController
@RequestMapping("/api/admin/products")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminProductController {

    private final AdminProductService adminProductService;

    public AdminProductController(
            AdminProductService adminProductService) {

        this.adminProductService = adminProductService;
    }


    // =========================================================
    // CREATE PRODUCT BY ADMIN
    // =========================================================

    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Product> createProduct(

            @RequestPart("product")
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


        // -----------------------------------------------------
        // CHECK AUTHENTICATION
        // -----------------------------------------------------

        if (authentication == null ||
                authentication.getName() == null ||
                authentication.getName().trim().isEmpty()) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }


        // -----------------------------------------------------
        // NULL SAFETY
        // -----------------------------------------------------

        if (images == null) {
            images = new MultipartFile[0];
        }

        if (imageUrls == null) {
            imageUrls = Collections.emptyList();
        }


        // -----------------------------------------------------
        // REMOVE EMPTY URL VALUES
        // -----------------------------------------------------

        imageUrls = imageUrls.stream()
                .filter(url ->
                        url != null &&
                        !url.trim().isEmpty())
                .map(String::trim)
                .toList();


        // -----------------------------------------------------
        // DEBUG
        // -----------------------------------------------------

        System.out.println(
                "=============================================="
        );

        System.out.println(
                "ADMIN CREATE PRODUCT"
        );

        System.out.println(
                "ADMIN EMAIL : "
                        + authentication.getName()
        );

        System.out.println(
                "LOCAL IMAGE COUNT : "
                        + images.length
        );

        System.out.println(
                "URL IMAGE COUNT : "
                        + imageUrls.size()
        );

        System.out.println(
                "TOTAL IMAGE COUNT : "
                        + (images.length + imageUrls.size())
        );

        System.out.println(
                "=============================================="
        );


        // -----------------------------------------------------
        // CREATE PRODUCT
        // -----------------------------------------------------

        Product product =
                adminProductService.createAdminProduct(
                        request,
                        images,
                        imageUrls,
                        authentication.getName()
                );


        // -----------------------------------------------------
        // RESPONSE
        // -----------------------------------------------------

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(product);
    }


    // =========================================================
    // GET ALL PRODUCTS
    // =========================================================

    @GetMapping
    public ResponseEntity<List<Product>> getProducts() {

        return ResponseEntity.ok(
                adminProductService.getProducts()
        );
    }


    // =========================================================
    // GET PENDING PRODUCTS
    // =========================================================

    @GetMapping("/pending")
    public ResponseEntity<List<Product>>
    getPendingProducts() {

        return ResponseEntity.ok(
                adminProductService.getPendingProducts()
        );
    }


    // =========================================================
    // GET SINGLE PRODUCT
    // =========================================================

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                adminProductService.getProduct(id)
        );
    }


    // =========================================================
    // APPROVE PRODUCT
    // =========================================================

    @PutMapping("/{id}/approve")
    public ResponseEntity<Product> approveProduct(

            @PathVariable Long id,

            @RequestBody(
                    required = false
            )
            ApprovalRequest request) {


        String review = null;

        if (request != null) {
            review = request.getReview();
        }


        Product product =
                adminProductService.approveProduct(
                        id,
                        review
                );


        return ResponseEntity.ok(product);
    }


    // =========================================================
    // REJECT PRODUCT
    // =========================================================

    @PutMapping("/{id}/reject")
    public ResponseEntity<Product> rejectProduct(

            @PathVariable Long id,

            @RequestBody
            ApprovalRequest request) {


        if (request == null ||
                request.getReason() == null ||
                request.getReason()
                        .trim()
                        .isEmpty()) {

            return ResponseEntity
                    .badRequest()
                    .build();
        }


        Product product =
                adminProductService.rejectProduct(
                        id,
                        request.getReason()
                );


        return ResponseEntity.ok(product);
    }

    @PutMapping("/{id}/offer")
    public ResponseEntity<Product> updateOffer(
            @PathVariable Long id,
            @RequestBody OfferRequest request) {

        if (request == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(
                adminProductService.updateOfferPercentage(id, request.getOfferPercentage())
        );
    }


    // =========================================================
    // APPROVAL REQUEST
    // =========================================================

    public static class ApprovalRequest {

        private String review;

        private String reason;


        public ApprovalRequest() {
        }


        public String getReview() {
            return review;
        }


        public void setReview(String review) {
            this.review = review;
        }


        public String getReason() {
            return reason;
        }


        public void setReason(String reason) {
            this.reason = reason;
        }
    }

}
