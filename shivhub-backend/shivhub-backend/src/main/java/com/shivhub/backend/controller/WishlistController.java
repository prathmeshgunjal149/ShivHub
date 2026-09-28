package com.shivhub.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shivhub.backend.entity.User;
import com.shivhub.backend.entity.Wishlist;
import com.shivhub.backend.repository.UserRepository;
import com.shivhub.backend.service.WishlistService;


/*
 * =========================================================
 * WishlistController
 * =========================================================
 *
 * Customer Wishlist APIs.
 *
 * APIs:
 *
 * POST   /api/wishlist/{productId}
 * GET    /api/wishlist
 * GET    /api/wishlist/check/{productId}
 * DELETE /api/wishlist/{productId}
 * GET    /api/wishlist/count
 *
 * Customer is identified from JWT authentication.
 *
 * =========================================================
 */

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {


    /*
     * =====================================================
     * SERVICES
     * =====================================================
     */

    private final WishlistService wishlistService;

    private final UserRepository userRepository;


    /*
     * =====================================================
     * CONSTRUCTOR
     * =====================================================
     */

    public WishlistController(
            WishlistService wishlistService,
            UserRepository userRepository) {

        this.wishlistService =
                wishlistService;

        this.userRepository =
                userRepository;
    }


    /*
     * =====================================================
     * ADD PRODUCT TO WISHLIST
     * =====================================================
     *
     * POST:
     *
     * /api/wishlist/{productId}
     *
     * =====================================================
     */

    @PostMapping("/{productId}")
    public ResponseEntity<Wishlist> addToWishlist(
            @PathVariable Long productId,
            Authentication authentication) {


        Long customerId =
                getAuthenticatedCustomerId(
                        authentication
                );


        Wishlist wishlist =
                wishlistService.addToWishlist(
                        customerId,
                        productId
                );


        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(wishlist);
    }


    /*
     * =====================================================
     * GET CUSTOMER WISHLIST
     * =====================================================
     *
     * GET:
     *
     * /api/wishlist
     *
     * =====================================================
     */

    @GetMapping
    public ResponseEntity<List<Wishlist>>
            getWishlist(
                    Authentication authentication) {


        Long customerId =
                getAuthenticatedCustomerId(
                        authentication
                );


        List<Wishlist> wishlist =
                wishlistService.getCustomerWishlist(
                        customerId
                );


        return ResponseEntity.ok(
                wishlist
        );
    }


    /*
     * =====================================================
     * CHECK PRODUCT
     * =====================================================
     *
     * GET:
     *
     * /api/wishlist/check/{productId}
     *
     * Response:
     *
     * true
     * false
     *
     * =====================================================
     */

    @GetMapping("/check/{productId}")
    public ResponseEntity<Boolean>
            checkWishlist(
                    @PathVariable Long productId,
                    Authentication authentication) {


        Long customerId =
                getAuthenticatedCustomerId(
                        authentication
                );


        boolean exists =
                wishlistService.isInWishlist(
                        customerId,
                        productId
                );


        return ResponseEntity.ok(
                exists
        );
    }


    /*
     * =====================================================
     * REMOVE FROM WISHLIST
     * =====================================================
     *
     * DELETE:
     *
     * /api/wishlist/{productId}
     *
     * =====================================================
     */

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void>
            removeFromWishlist(
                    @PathVariable Long productId,
                    Authentication authentication) {


        Long customerId =
                getAuthenticatedCustomerId(
                        authentication
                );


        wishlistService.removeFromWishlist(
                customerId,
                productId
        );


        return ResponseEntity.noContent()
                .build();
    }


    /*
     * =====================================================
     * GET WISHLIST COUNT
     * =====================================================
     *
     * GET:
     *
     * /api/wishlist/count
     *
     * =====================================================
     */

    @GetMapping("/count")
    public ResponseEntity<Long>
            getWishlistCount(
                    Authentication authentication) {


        Long customerId =
                getAuthenticatedCustomerId(
                        authentication
                );


        long count =
                wishlistService.getWishlistCount(
                        customerId
                );


        return ResponseEntity.ok(
                count
        );
    }


    /*
     * =====================================================
     * GET AUTHENTICATED CUSTOMER ID
     * =====================================================
     *
     * JWT subject contains customer email.
     *
     * Example:
     *
     * omkarkarpe91@gmail.com
     *
     * We find the User using that email.
     *
     * =====================================================
     */

    private Long getAuthenticatedCustomerId(
            Authentication authentication) {


        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new RuntimeException(
                    "Customer is not authenticated"
            );
        }


        String email =
                authentication.getName();


        if (email == null
                || email.isBlank()) {

            throw new RuntimeException(
                    "Authenticated email not found"
            );
        }


        User user =
                userRepository.findByEmail(
                        email
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Authenticated user not found"
                        )
                );


        /*
         * Only CUSTOMER can use wishlist.
         */

        if (user.getRole() == null
                || !"CUSTOMER".equals(
                        user.getRole().name())) {

            throw new RuntimeException(
                    "Only customers can use wishlist"
            );
        }


        return user.getId();
    }
}