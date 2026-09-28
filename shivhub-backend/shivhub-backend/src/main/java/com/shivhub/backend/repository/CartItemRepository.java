package com.shivhub.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.shivhub.backend.entity.CartItem;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.entity.Product;


/*
 * =========================================================
 * CartItemRepository
 * =========================================================
 *
 * Handles database operations for cart_items.
 *
 * =========================================================
 */

@Repository
public interface CartItemRepository
        extends JpaRepository<CartItem, Long> {


    /*
     * =====================================================
     * GET CUSTOMER CART
     * =====================================================
     */

    List<CartItem> findByCustomerOrderByCreatedAtDesc(
            User customer
    );


    /*
     * =====================================================
     * FIND PRODUCT IN CUSTOMER CART
     * =====================================================
     */

    @org.springframework.data.jpa.repository.Query("select c from CartItem c where c.customer = :customer and c.product = :product and c.productVariant is null")
    Optional<CartItem> findByCustomerAndProduct(
            User customer,
            Product product
    );

    Optional<CartItem> findByCustomerAndProductAndProductVariant(User customer, Product product, com.shivhub.backend.entity.ProductVariant productVariant);
    Optional<CartItem> findByIdAndCustomer(Long id, User customer);


    /*
     * =====================================================
     * CHECK PRODUCT ALREADY IN CART
     * =====================================================
     */

    @org.springframework.data.jpa.repository.Query("select (count(c)>0) from CartItem c where c.customer=:customer and c.product=:product and c.productVariant is null")
    boolean existsByCustomerAndProduct(
            User customer,
            Product product
    );


    /*
     * =====================================================
     * REMOVE PRODUCT FROM CART
     * =====================================================
     */

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("delete from CartItem c where c.customer=:customer and c.product=:product and c.productVariant is null")
    void deleteByCustomerAndProduct(
            User customer,
            Product product
    );


    /*
     * =====================================================
     * COUNT CART ITEMS
     * =====================================================
     */

    long countByCustomer(
            User customer
    );


    /*
     * =====================================================
     * DELETE CUSTOMER CART
     * =====================================================
     */

    void deleteByCustomer(
            User customer
    );
}
