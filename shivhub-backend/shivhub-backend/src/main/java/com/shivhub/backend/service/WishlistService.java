package com.shivhub.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shivhub.backend.entity.Product;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.entity.Wishlist;
import com.shivhub.backend.enums.ProductStatus;
import com.shivhub.backend.repository.ProductRepository;
import com.shivhub.backend.repository.UserRepository;
import com.shivhub.backend.repository.WishlistRepository;


/*
 * =========================================================
 * WishlistService
 * =========================================================
 *
 * Handles all Wishlist business logic.
 *
 * Features:
 *
 * 1. Add product to wishlist
 * 2. Get customer's wishlist
 * 3. Check wishlist item
 * 4. Remove product
 * 5. Get wishlist count
 *
 * =========================================================
 */

@Service
public class WishlistService {


    /*
     * =====================================================
     * REPOSITORIES
     * =====================================================
     */

    private final WishlistRepository wishlistRepository;

    private final UserRepository userRepository;

    private final ProductRepository productRepository;


    /*
     * =====================================================
     * CONSTRUCTOR
     * =====================================================
     */

    public WishlistService(
            WishlistRepository wishlistRepository,
            UserRepository userRepository,
            ProductRepository productRepository) {

        this.wishlistRepository =
                wishlistRepository;

        this.userRepository =
                userRepository;

        this.productRepository =
                productRepository;
    }


    /*
     * =====================================================
     * ADD TO WISHLIST
     * =====================================================
     */

    @Transactional
    public Wishlist addToWishlist(
            Long customerId,
            Long productId) {


        /*
         * =================================================
         * VERIFY CUSTOMER
         * =================================================
         */

        User customer =
                userRepository.findById(
                        customerId
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Customer not found"
                        )
                );


        /*
         * =================================================
         * VERIFY CUSTOMER ROLE
         * =================================================
         */

        if (customer.getRole() == null
                || !"CUSTOMER".equals(
                        customer.getRole().name())) {

            throw new RuntimeException(
                    "Only customers can use wishlist"
            );
        }


        /*
         * =================================================
         * VERIFY ACCOUNT
         * =================================================
         */

        if (!customer.isEnabled()) {

            throw new RuntimeException(
                    "Customer account is disabled"
            );
        }


        /*
         * =================================================
         * FIND PRODUCT
         * =================================================
         */

        Product product =
                productRepository.findById(
                        productId
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Product not found"
                        )
                );


        /*
         * =================================================
         * PRODUCT MUST BE APPROVED
         * =================================================
         */

        if (product.getApprovalStatus()
                != ProductStatus.APPROVED) {

            throw new RuntimeException(
                    "Product is not available"
            );
        }


        /*
         * =================================================
         * PRODUCT MUST BE ACTIVE
         * =================================================
         */

        if (!product.isActive()) {

            throw new RuntimeException(
                    "Product is inactive"
            );
        }


        /*
         * =================================================
         * CHECK DUPLICATE
         * =================================================
         */

        boolean alreadyExists =
                wishlistRepository
                        .existsByCustomerIdAndProductId(
                                customerId,
                                productId
                        );


        if (alreadyExists) {

            throw new RuntimeException(
                    "Product is already in wishlist"
            );
        }


        /*
         * =================================================
         * CREATE WISHLIST ITEM
         * =================================================
         */

        Wishlist wishlist =
                new Wishlist();


        wishlist.setCustomerId(
                customerId
        );


        wishlist.setProductId(
                productId
        );


        /*
         * =================================================
         * SAVE
         * =================================================
         */

        return wishlistRepository.save(
                wishlist
        );
    }


    /*
     * =====================================================
     * GET CUSTOMER WISHLIST
     * =====================================================
     */

    @Transactional(readOnly = true)
    public List<Wishlist> getCustomerWishlist(
            Long customerId) {


        /*
         * Verify customer.
         */

        userRepository.findById(
                customerId
        )
        .orElseThrow(() ->
                new RuntimeException(
                        "Customer not found"
                )
        );


        /*
         * Return wishlist.
         */

        return wishlistRepository
                .findByCustomerIdOrderByCreatedAtDesc(
                        customerId
                );
    }


    /*
     * =====================================================
     * CHECK PRODUCT IN WISHLIST
     * =====================================================
     */

    @Transactional(readOnly = true)
    public boolean isInWishlist(
            Long customerId,
            Long productId) {


        return wishlistRepository
                .existsByCustomerIdAndProductId(
                        customerId,
                        productId
                );
    }


    /*
     * =====================================================
     * REMOVE FROM WISHLIST
     * =====================================================
     */

    @Transactional
    public void removeFromWishlist(
            Long customerId,
            Long productId) {


        /*
         * =================================================
         * CHECK ITEM
         * =================================================
         */

        Wishlist wishlist =
                wishlistRepository
                        .findByCustomerIdAndProductId(
                                customerId,
                                productId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Product is not in wishlist"
                                )
                        );


        /*
         * =================================================
         * DELETE
         * =================================================
         */

        wishlistRepository.delete(
                wishlist
        );
    }


    /*
     * =====================================================
     * GET WISHLIST COUNT
     * =====================================================
     */

    @Transactional(readOnly = true)
    public long getWishlistCount(
            Long customerId) {

        return wishlistRepository
                .countByCustomerId(
                        customerId
                );
    }
}