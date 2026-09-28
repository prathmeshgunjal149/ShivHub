package com.shivhub.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shivhub.backend.entity.Product;
import com.shivhub.backend.entity.StockMovement;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.repository.StockMovementRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockMovementService {

    private final StockMovementRepository stockMovementRepository;


    /*
     * =========================================================
     * RECORD STOCK MOVEMENT
     * =========================================================
     *
     * This method records every stock change.
     *
     * Example:
     *
     * Before = 10
     * Quantity = +5
     * After = 15
     *
     * =========================================================
     */

    @Transactional
    public StockMovement recordMovement(
            Product product,
            User seller,
            String movementType,
            Integer quantity,
            Integer stockBefore,
            Integer stockAfter,
            String referenceType,
            Long referenceId,
            String notes) {


        /*
         * -----------------------------------------------------
         * BASIC VALIDATION
         * -----------------------------------------------------
         */

        if (product == null) {
            throw new RuntimeException(
                    "Product is required"
            );
        }

        if (seller == null) {
            throw new RuntimeException(
                    "Seller is required"
            );
        }

        if (movementType == null
                || movementType.isBlank()) {

            throw new RuntimeException(
                    "Movement type is required"
            );
        }

        if (quantity == null
                || quantity == 0) {

            throw new RuntimeException(
                    "Movement quantity cannot be zero"
            );
        }

        if (stockBefore == null
                || stockBefore < 0) {

            throw new RuntimeException(
                    "Invalid stock before value"
            );
        }

        if (stockAfter == null
                || stockAfter < 0) {

            throw new RuntimeException(
                    "Invalid stock after value"
            );
        }


        /*
         * -----------------------------------------------------
         * VERIFY STOCK CALCULATION
         * -----------------------------------------------------
         *
         * Example:
         *
         * 10 + 5 = 15
         *
         * OR
         *
         * 15 - 5 = 10
         *
         * -----------------------------------------------------
         */

        if (stockBefore + quantity != stockAfter) {

            throw new RuntimeException(
                    "Invalid stock movement calculation"
            );
        }


        /*
         * -----------------------------------------------------
         * DUPLICATE PROTECTION
         * -----------------------------------------------------
         *
         * For reference-based movements such as PURCHASE,
         * prevent the same product/reference from being
         * recorded twice.
         *
         * -----------------------------------------------------
         */

        if (referenceType != null
                && referenceId != null) {

            boolean alreadyExists =
                    stockMovementRepository
                            .existsByReferenceTypeAndReferenceIdAndProductId(
                                    referenceType,
                                    referenceId,
                                    product.getId()
                            );

            /*
             * Duplicate PURCHASE movement should not happen.
             */

            if (alreadyExists) {

                throw new RuntimeException(
                        "Stock movement already exists for "
                                + referenceType
                                + " ID "
                                + referenceId
                                + " and product "
                                + product.getId()
                );
            }
        }


        /*
         * -----------------------------------------------------
         * CREATE MOVEMENT
         * -----------------------------------------------------
         */

        StockMovement movement =
                new StockMovement();

        movement.setProduct(product);

        movement.setSeller(seller);

        movement.setMovementType(
                movementType.trim().toUpperCase()
        );

        movement.setQuantity(quantity);

        movement.setStockBefore(stockBefore);

        movement.setStockAfter(stockAfter);

        movement.setReferenceType(
                referenceType
        );

        movement.setReferenceId(
                referenceId
        );

        movement.setNotes(
                notes
        );


        /*
         * -----------------------------------------------------
         * SAVE
         * -----------------------------------------------------
         */

        return stockMovementRepository.save(
                movement
        );
    }


    /*
     * =========================================================
     * GET PRODUCT STOCK HISTORY
     * =========================================================
     */

    @Transactional(readOnly = true)
    public List<StockMovement> getProductHistory(
            Long productId) {

        if (productId == null) {

            throw new RuntimeException(
                    "Product ID is required"
            );
        }

        return stockMovementRepository
                .findByProductIdOrderByCreatedAtDesc(
                        productId
                );
    }


    /*
     * =========================================================
     * GET SELLER STOCK HISTORY
     * =========================================================
     */

    @Transactional(readOnly = true)
    public List<StockMovement> getSellerHistory(
            User seller) {

        if (seller == null
                || seller.getId() == null) {

            throw new RuntimeException(
                    "Valid seller is required"
            );
        }

        return stockMovementRepository
                .findBySellerOrderByCreatedAtDesc(
                        seller
                );
    }


    /*
     * =========================================================
     * GET SELLER + PRODUCT HISTORY
     * =========================================================
     */

    @Transactional(readOnly = true)
    public List<StockMovement> getSellerProductHistory(
            User seller,
            Long productId) {

        if (seller == null
                || seller.getId() == null) {

            throw new RuntimeException(
                    "Valid seller is required"
            );
        }

        if (productId == null) {

            throw new RuntimeException(
                    "Product ID is required"
            );
        }

        Product product =
                new Product();

        product.setId(productId);

        return stockMovementRepository
                .findBySellerAndProductOrderByCreatedAtDesc(
                        seller,
                        product
                );
    }


    /*
     * =========================================================
     * GET PURCHASE STOCK MOVEMENTS
     * =========================================================
     */

    @Transactional(readOnly = true)
    public List<StockMovement> getPurchaseMovements(
            Long purchaseId) {

        if (purchaseId == null) {

            throw new RuntimeException(
                    "Purchase ID is required"
            );
        }

        return stockMovementRepository
                .findByReferenceTypeAndReferenceId(
                        "PURCHASE",
                        purchaseId
                );
    }
}