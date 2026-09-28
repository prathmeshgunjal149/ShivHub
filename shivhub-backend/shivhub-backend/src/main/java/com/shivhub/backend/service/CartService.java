package com.shivhub.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shivhub.backend.dto.AddToCartRequest;
import com.shivhub.backend.entity.CartItem;
import com.shivhub.backend.entity.Product;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.entity.ProductVariant;
import com.shivhub.backend.repository.CartItemRepository;
import com.shivhub.backend.repository.ProductRepository;
import com.shivhub.backend.repository.UserRepository;
import com.shivhub.backend.repository.ProductVariantRepository;


/*
 * =========================================================
 * CartService
 * =========================================================
 *
 * Handles all customer cart business logic.
 *
 * Features:
 *
 * 1. Get cart
 * 2. Add product
 * 3. Increase quantity
 * 4. Decrease quantity
 * 5. Update quantity
 * 6. Remove product
 * 7. Clear cart
 * 8. Cart count
 *
 * =========================================================
 */

@Service
public class CartService {
    @Transactional
    public CartItem changeLine(Long customerId, Long lineId, int delta) {
        CartItem line = cartItemRepository.findByIdAndCustomer(lineId, getCustomer(customerId))
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Cart line not found"));
        int quantity = line.getQuantity() + delta;
        if (quantity <= 0) { cartItemRepository.delete(line); return null; }
        if (!line.getProduct().isActive() || line.getProduct().getApprovalStatus() != com.shivhub.backend.enums.ProductStatus.APPROVED || (line.getProductVariant() != null && !line.getProductVariant().isActive())) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Product option is unavailable");
        if (quantity > line.getAvailableStock()) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Insufficient stock for selected option");
        line.setQuantity(quantity); return cartItemRepository.save(line);
    }
    @Transactional
    public void removeLine(Long customerId, Long lineId) {
        CartItem line = cartItemRepository.findByIdAndCustomer(lineId, getCustomer(customerId))
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Cart line not found"));
        cartItemRepository.delete(line);
    }


    private final CartItemRepository cartItemRepository;

    private final UserRepository userRepository;

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;


    /*
     * =====================================================
     * CONSTRUCTOR
     * =====================================================
     */

    public CartService(
            CartItemRepository cartItemRepository,
            UserRepository userRepository,
            ProductRepository productRepository, ProductVariantRepository productVariantRepository) {

        this.cartItemRepository =
                cartItemRepository;

        this.userRepository =
                userRepository;

        this.productRepository =
                productRepository;
        this.productVariantRepository = productVariantRepository;
    }


    /*
     * =====================================================
     * GET CUSTOMER
     * =====================================================
     */

    private User getCustomer(
            Long customerId) {

        User customer =
                userRepository.findById(
                        customerId
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Customer not found"
                        )
                );


        if (customer.getRole() == null
                || !"CUSTOMER".equals(
                        customer.getRole().name())) {

            throw new RuntimeException(
                    "Only customers can use cart"
            );
        }


        if (!customer.isEnabled()) {

            throw new RuntimeException(
                    "Customer account is disabled"
            );
        }


        return customer;
    }


    /*
     * =====================================================
     * GET CART
     * =====================================================
     */

    @Transactional(readOnly = true)
    public List<CartItem> getCart(
            Long customerId) {

        User customer =
                getCustomer(
                        customerId
                );


        return cartItemRepository
                .findByCustomerOrderByCreatedAtDesc(
                        customer
                );
    }


    /*
     * =====================================================
     * ADD TO CART
     * =====================================================
     *
     * If product already exists:
     *
     * Existing quantity + requested quantity
     *
     * Example:
     *
     * Existing = 2
     * Request  = 1
     *
     * Result = 3
     *
     * =====================================================
     */

    @Transactional
    public CartItem addToCart(
            Long customerId,
            Long productId,
            Integer quantity) { return addToCart(customerId, productId, quantity, null); }

    @Transactional
    public CartItem addToCart(Long customerId, Long productId, Integer quantity, Long variantId) {


        if (quantity == null
                || quantity < 1) {

            throw new RuntimeException(
                    "Quantity must be at least 1"
            );
        }


        User customer =
                getCustomer(
                        customerId
                );


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
         * Product must be active.
         */

        if (!product.isActive() || product.getApprovalStatus() != com.shivhub.backend.enums.ProductStatus.APPROVED) {

            throw new RuntimeException(
                    "Product is not available"
            );
        }


        /*
         * Product must have stock.
         */

        ProductVariant variant = null;
        if (product.isVariantsEnabled() && variantId == null) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Please select all product options");
        if (variantId != null) {
            variant = productVariantRepository.findByIdAndProduct(variantId, product)
                    .orElseThrow(() -> new RuntimeException("Selected variant does not belong to this product"));
            if (!product.isVariantsEnabled() || !variant.isActive() || variant.getAvailableStock() <= 0) throw new RuntimeException("Selected variant is out of stock");
        }
        int available = variant == null ? product.getAvailableStock() : variant.getAvailableStock();
        if (available <= 0) {

            throw new RuntimeException(
                    "Product is out of stock"
            );
        }


        /*
         * Requested quantity cannot exceed stock.
         */

        CartItem existingItem = variant == null
                ? cartItemRepository.findByCustomerAndProduct(customer, product).orElse(null)
                : cartItemRepository.findByCustomerAndProductAndProductVariant(customer, product, variant).orElse(null);


        int finalQuantity;


        if (existingItem != null) {

            finalQuantity =
                    existingItem.getQuantity()
                    + quantity;

        } else {

            finalQuantity =
                    quantity;
        }


        /*
         * Check stock.
         */

        if (finalQuantity > available) {

            throw new RuntimeException(
                    "Only "
                    + available
                    + " item(s) available in stock"
            );
        }


        /*
         * Existing item.
         */

        if (existingItem != null) {

            existingItem.setQuantity(
                    finalQuantity
            );


            return cartItemRepository.save(
                    existingItem
            );
        }


        /*
         * New cart item.
         */

        CartItem cartItem =
                new CartItem();


        cartItem.setCustomer(
                customer
        );


        cartItem.setProduct(
                product
        );
        cartItem.setProductVariant(variant);
        cartItem.setSelectionKey(variant == null ? 0L : variant.getId());


        cartItem.setQuantity(
                quantity
        );


        return cartItemRepository.save(
                cartItem
        );
    }

    @Transactional
    public void addSelectedToCart(
            Long customerId,
            List<AddToCartRequest> items) {

        if (items == null || items.isEmpty()) {
            throw new RuntimeException("Select at least one product");
        }

        for (AddToCartRequest item : items) {
            if (item == null) {
                throw new RuntimeException("Invalid cart item");
            }
            addToCart(customerId, item.getProductId(), item.getQuantity(), item.getVariantId());
        }
    }


    /*
     * =====================================================
     * UPDATE QUANTITY
     * =====================================================
     */

    @Transactional
    public CartItem updateQuantity(
            Long customerId,
            Long productId,
            Integer quantity) {


        if (quantity == null
                || quantity < 1) {

            throw new RuntimeException(
                    "Quantity must be at least 1"
            );
        }


        User customer =
                getCustomer(
                        customerId
                );


        Product product =
                productRepository.findById(
                        productId
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Product not found"
                        )
                );


        CartItem cartItem =
                cartItemRepository
                        .findByCustomerAndProduct(
                                customer,
                                product
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Product is not in cart"
                                )
                        );


        /*
         * Check stock.
         */

        if (quantity > product.getAvailableStock()) {

            throw new RuntimeException(
                    "Only "
                    + product.getAvailableStock()
                    + " item(s) available in stock"
            );
        }


        cartItem.setQuantity(
                quantity
        );


        return cartItemRepository.save(
                cartItem
        );
    }


    /*
     * =====================================================
     * INCREASE QUANTITY
     * =====================================================
     */

    @Transactional
    public CartItem increaseQuantity(
            Long customerId,
            Long productId) {


        User customer =
                getCustomer(
                        customerId
                );


        Product product =
                productRepository.findById(
                        productId
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Product not found"
                        )
                );


        CartItem cartItem =
                cartItemRepository
                        .findByCustomerAndProduct(
                                customer,
                                product
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Product is not in cart"
                                )
                        );


        int newQuantity =
                cartItem.getQuantity() + 1;


        if (newQuantity > product.getAvailableStock()) {

            throw new RuntimeException(
                    "Only "
                    + product.getAvailableStock()
                    + " item(s) available in stock"
            );
        }


        cartItem.setQuantity(
                newQuantity
        );


        return cartItemRepository.save(
                cartItem
        );
    }


    /*
     * =====================================================
     * DECREASE QUANTITY
     * =====================================================
     */

    @Transactional
    public CartItem decreaseQuantity(
            Long customerId,
            Long productId) {


        User customer =
                getCustomer(
                        customerId
                );


        Product product =
                productRepository.findById(
                        productId
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Product not found"
                        )
                );


        CartItem cartItem =
                cartItemRepository
                        .findByCustomerAndProduct(
                                customer,
                                product
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Product is not in cart"
                                )
                        );


        int newQuantity =
                cartItem.getQuantity() - 1;


        /*
         * If quantity becomes zero,
         * remove the item.
         */

        if (newQuantity <= 0) {

            cartItemRepository.delete(
                    cartItem
            );

            return null;
        }


        cartItem.setQuantity(
                newQuantity
        );


        return cartItemRepository.save(
                cartItem
        );
    }


    /*
     * =====================================================
     * REMOVE PRODUCT
     * =====================================================
     */

    @Transactional
    public void removeFromCart(
            Long customerId,
            Long productId) {


        User customer =
                getCustomer(
                        customerId
                );


        Product product =
                productRepository.findById(
                        productId
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Product not found"
                        )
                );


        boolean exists =
                cartItemRepository
                        .existsByCustomerAndProduct(
                                customer,
                                product
                        );


        if (!exists) {

            throw new RuntimeException(
                    "Product is not in cart"
            );
        }


        cartItemRepository
                .deleteByCustomerAndProduct(
                        customer,
                        product
                );
    }


    /*
     * =====================================================
     * CLEAR CART
     * =====================================================
     */

    @Transactional
    public void clearCart(
            Long customerId) {


        User customer =
                getCustomer(
                        customerId
                );


        cartItemRepository
                .deleteByCustomer(
                        customer
                );
    }


    /*
     * =====================================================
     * CART COUNT
     * =====================================================
     */

    @Transactional(readOnly = true)
    public long getCartCount(
            Long customerId) {


        User customer =
                getCustomer(
                        customerId
                );


        return cartItemRepository
                .countByCustomer(
                        customer
                );
    }
}
