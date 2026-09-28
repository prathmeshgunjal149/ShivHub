package com.shivhub.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shivhub.backend.dto.AddToCartRequest;
import com.shivhub.backend.dto.BulkAddToCartRequest;
import com.shivhub.backend.dto.BulkAddToCartResponse;
import com.shivhub.backend.entity.CartItem;
import com.shivhub.backend.dto.CartLineResponse;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.repository.UserRepository;
import com.shivhub.backend.service.CartService;

import jakarta.validation.Valid;


/*
 * =========================================================
 * CartController
 * =========================================================
 *
 * Customer Cart APIs.
 *
 * =========================================================
 *
 * GET
 *     /api/cart
 *
 * POST
 *     /api/cart
 *
 * PUT
 *     /api/cart/{productId}
 *
 * POST
 *     /api/cart/{productId}/increase
 *
 * POST
 *     /api/cart/{productId}/decrease
 *
 * DELETE
 *     /api/cart/{productId}
 *
 * DELETE
 *     /api/cart
 *
 * GET
 *     /api/cart/count
 *
 * =========================================================
 */

@RestController
@RequestMapping("/api/cart")
public class CartController {
    @PostMapping("/lines/{lineId}/increase")
    public ResponseEntity<CartLineResponse> increaseLine(@PathVariable Long lineId, Authentication authentication) {
        return ResponseEntity.ok(CartLineResponse.from(cartService.changeLine(getAuthenticatedCustomerId(authentication), lineId, 1)));
    }
    @PostMapping("/lines/{lineId}/decrease")
    public ResponseEntity<CartLineResponse> decreaseLine(@PathVariable Long lineId, Authentication authentication) {
        CartItem line = cartService.changeLine(getAuthenticatedCustomerId(authentication), lineId, -1);
        return line == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(CartLineResponse.from(line));
    }
    @DeleteMapping("/lines/{lineId}")
    public ResponseEntity<Void> removeLine(@PathVariable Long lineId, Authentication authentication) {
        cartService.removeLine(getAuthenticatedCustomerId(authentication), lineId); return ResponseEntity.noContent().build();
    }


    private final CartService cartService;

    private final UserRepository userRepository;


    /*
     * =====================================================
     * CONSTRUCTOR
     * =====================================================
     */

    public CartController(
            CartService cartService,
            UserRepository userRepository) {

        this.cartService =
                cartService;

        this.userRepository =
                userRepository;
    }


    /*
     * =====================================================
     * GET CART
     * =====================================================
     *
     * GET /api/cart
     *
     * =====================================================
     */

    @GetMapping
    public ResponseEntity<List<CartLineResponse>>
            getCart(
                    Authentication authentication) {


        Long customerId =
                getAuthenticatedCustomerId(
                        authentication
                );


        List<CartItem> cart =
                cartService.getCart(
                        customerId
                );


        return ResponseEntity.ok(cart.stream().map(CartLineResponse::from).toList());
    }


    /*
     * =====================================================
     * ADD TO CART
     * =====================================================
     *
     * POST /api/cart
     *
     * Body:
     *
     * {
     *     "productId": 1,
     *     "quantity": 2
     * }
     *
     * =====================================================
     */

    @PostMapping
    public ResponseEntity<CartLineResponse>
            addToCart(
                    @Valid @RequestBody
                    AddToCartRequest request,
                    Authentication authentication) {


        Long customerId =
                getAuthenticatedCustomerId(
                        authentication
                );


        CartItem cartItem =
                cartService.addToCart(
                        customerId,
                        request.getProductId(),
                        request.getQuantity(),
                        request.getVariantId()
                );


        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CartLineResponse.from(cartItem));
    }

    @PostMapping("/bulk")
    public ResponseEntity<BulkAddToCartResponse>
            addSelectedToCart(
                    @Valid @RequestBody
                    BulkAddToCartRequest request,
                    Authentication authentication) {

        Long customerId =
                getAuthenticatedCustomerId(
                        authentication
                );

        cartService.addSelectedToCart(
                customerId,
                request.items()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new BulkAddToCartResponse(true, List.of()));
    }


    /*
     * =====================================================
     * UPDATE QUANTITY
     * =====================================================
     *
     * PUT /api/cart/{productId}
     *
     * Body:
     *
     * {
     *     "quantity": 3
     * }
     *
     * =====================================================
     */

    @PutMapping("/{productId}")
    public ResponseEntity<CartLineResponse>
            updateQuantity(
                    @PathVariable Long productId,
                    @RequestBody
                    QuantityRequest request,
                    Authentication authentication) {


        Long customerId =
                getAuthenticatedCustomerId(
                        authentication
                );


        CartItem cartItem =
                cartService.updateQuantity(
                        customerId,
                        productId,
                        request.getQuantity()
                );


        return ResponseEntity.ok(CartLineResponse.from(cartItem));
    }


    /*
     * =====================================================
     * INCREASE QUANTITY
     * =====================================================
     *
     * POST /api/cart/{productId}/increase
     *
     * =====================================================
     */

    @PostMapping("/{productId}/increase")
    public ResponseEntity<CartLineResponse>
            increaseQuantity(
                    @PathVariable Long productId,
                    Authentication authentication) {


        Long customerId =
                getAuthenticatedCustomerId(
                        authentication
                );


        CartItem cartItem =
                cartService.increaseQuantity(
                        customerId,
                        productId
                );


        return ResponseEntity.ok(CartLineResponse.from(cartItem));
    }


    /*
     * =====================================================
     * DECREASE QUANTITY
     * =====================================================
     *
     * POST /api/cart/{productId}/decrease
     *
     * =====================================================
     */

    @PostMapping("/{productId}/decrease")
    public ResponseEntity<CartLineResponse>
            decreaseQuantity(
                    @PathVariable Long productId,
                    Authentication authentication) {


        Long customerId =
                getAuthenticatedCustomerId(
                        authentication
                );


        CartItem cartItem =
                cartService.decreaseQuantity(
                        customerId,
                        productId
                );


        /*
         * If quantity became zero,
         * service already removed the item.
         */

        if (cartItem == null) {

            return ResponseEntity.noContent()
                    .build();
        }


        return ResponseEntity.ok(CartLineResponse.from(cartItem));
    }


    /*
     * =====================================================
     * REMOVE PRODUCT
     * =====================================================
     *
     * DELETE /api/cart/{productId}
     *
     * =====================================================
     */

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void>
            removeFromCart(
                    @PathVariable Long productId,
                    Authentication authentication) {


        Long customerId =
                getAuthenticatedCustomerId(
                        authentication
                );


        cartService.removeFromCart(
                customerId,
                productId
        );


        return ResponseEntity.noContent()
                .build();
    }


    /*
     * =====================================================
     * CLEAR CART
     * =====================================================
     *
     * DELETE /api/cart
     *
     * =====================================================
     */

    @DeleteMapping
    public ResponseEntity<Void>
            clearCart(
                    Authentication authentication) {


        Long customerId =
                getAuthenticatedCustomerId(
                        authentication
                );


        cartService.clearCart(
                customerId
        );


        return ResponseEntity.noContent()
                .build();
    }


    /*
     * =====================================================
     * CART COUNT
     * =====================================================
     *
     * GET /api/cart/count
     *
     * =====================================================
     */

    @GetMapping("/count")
    public ResponseEntity<Long>
            getCartCount(
                    Authentication authentication) {


        Long customerId =
                getAuthenticatedCustomerId(
                        authentication
                );


        long count =
                cartService.getCartCount(
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
     * JWT subject = email
     *
     * Example:
     *
     * omkarkarpe91@gmail.com
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


        if (user.getRole() == null
                || !"CUSTOMER".equals(
                        user.getRole().name())) {

            throw new RuntimeException(
                    "Only customers can use cart"
            );
        }


        return user.getId();
    }


    /*
     * =====================================================
     * QUANTITY REQUEST
     * =====================================================
     *
     * Used by:
     *
     * PUT /api/cart/{productId}
     *
     * =====================================================
     */

    public static class QuantityRequest {

        private Integer quantity;


        public Integer getQuantity() {

            return quantity;
        }


        public void setQuantity(
                Integer quantity) {

            this.quantity =
                    quantity;
        }
    }
}
