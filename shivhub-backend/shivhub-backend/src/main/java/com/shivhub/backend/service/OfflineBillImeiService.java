package com.shivhub.backend.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shivhub.backend.dto.AvailableImeiResponse;
import com.shivhub.backend.entity.OfflineBillItem;
import com.shivhub.backend.entity.Product;
import com.shivhub.backend.entity.PurchaseItemSerial;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.repository.ProductRepository;
import com.shivhub.backend.repository.PurchaseItemSerialRepository;
import com.shivhub.backend.repository.UserRepository;


/*
 * =========================================================
 * OfflineBillImeiService
 * =========================================================
 *
 * Controls physical IMEI / Serial units used by POS billing.
 *
 * FLOW:
 *
 * Purchase
 *      ↓
 * PurchaseItem
 *      ↓
 * PurchaseItemSerial
 *      ↓
 * IMEI / Serial
 *      ↓
 * OfflineBillItem
 *      ↓
 * OfflineBill
 *      ↓
 * Customer
 *
 * =========================================================
 */

@Service
public class OfflineBillImeiService {

    private final PurchaseItemSerialRepository serialRepository;

    private final UserRepository userRepository;

    private final ProductRepository productRepository;


    /*
     * =========================================================
     * CONSTRUCTOR
     * =========================================================
     */

    public OfflineBillImeiService(
            PurchaseItemSerialRepository serialRepository,
            UserRepository userRepository,
            ProductRepository productRepository) {

        this.serialRepository = serialRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }


    /*
     * =========================================================
     * GET AVAILABLE IMEI
     * =========================================================
     *
     * Seller POS:
     *
     * Product select
     *      ↓
     * Available IMEI list
     *
     * =========================================================
     */

    @Transactional(readOnly = true)
    public List<AvailableImeiResponse> available(
            String email,
            Long productId) {

        User seller = seller(email);

        if (productId == null) {

            throw new RuntimeException(
                    "Product ID is required"
            );
        }

        return serialRepository
                .findAvailableBySellerAndProduct(
                        seller.getId(),
                        productId
                )
                .stream()
                .map(serial ->
                        new AvailableImeiResponse(
                                serial.getId(),
                                serial.getImei1(),
                                serial.getImei2()
                        )
                )
                .toList();
    }


    /*
     * =========================================================
     * MARK IMEI AS SOLD
     * =========================================================
     *
     * Called from OfflineBillingService.
     *
     * Important:
     *
     * The backend NEVER trusts browser selected IDs.
     *
     * It verifies:
     *
     * Seller
     * Product
     * Purchase
     * Purchase status
     * IMEI
     * Available status
     *
     * =========================================================
     */

    @Transactional
    public void markSold(
            User billingUser,
            Long productId,
            int quantity,
            List<Long> ids,
            OfflineBillItem billItem) {

        /*
         * =====================================================
         * BASIC VALIDATION
         * =====================================================
         */

        if (billingUser == null) {

            throw new RuntimeException(
                    "Billing user is required"
            );
        }

        if (productId == null) {

            throw new RuntimeException(
                    "Product ID is required"
            );
        }

        if (quantity <= 0) {

            throw new RuntimeException(
                    "Invalid quantity"
            );
        }

        if (billItem == null) {

            throw new RuntimeException(
                    "Bill item is required"
            );
        }


        /*
         * =====================================================
         * FIND PRODUCT
         * =====================================================
         */

        Product product =
                productRepository.findById(productId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Product not found: "
                                                + productId
                                )
                        );


        /*
         * =====================================================
         * FIND ACTUAL INVENTORY SELLER
         * =====================================================
         *
         * SELLER:
         *
         * billingUser = actual seller
         *
         *
         * ADMIN:
         *
         * billingUser = admin
         *
         * Product seller = actual shop seller
         *
         * Therefore for IMEI verification we must use
         * product.getSeller().
         *
         * =====================================================
         */

        User inventorySeller;

        if (billingUser.getRole() == Role.ADMIN) {

            inventorySeller =
                    product.getSeller();

            if (inventorySeller == null) {

                throw new RuntimeException(
                        "Product is not assigned to a seller"
                );
            }

        } else {

            inventorySeller =
                    billingUser;
        }


        /*
         * =====================================================
         * SELLER PRODUCT OWNERSHIP
         * =====================================================
         */

        if (billingUser.getRole() == Role.SELLER) {

            if (product.getSeller() == null
                    || !billingUser.getId()
                            .equals(product.getSeller().getId())) {

                throw new RuntimeException(
                        "You can sell only your own product stock"
                );
            }
        }


        /*
         * =====================================================
         * GET AVAILABLE SERIALS
         * =====================================================
         */

        List<PurchaseItemSerial> available =
                serialRepository
                        .findAvailableBySellerAndProduct(
                                inventorySeller.getId(),
                                productId
                        );


        /*
         * =====================================================
         * NON-IMEI PRODUCT
         * =====================================================
         *
         * No serial records means normal stock product.
         *
         * Example:
         *
         * Cover
         * Charger
         * Cable
         * Speaker
         *
         * =====================================================
         */

        if (available.isEmpty()) {

            if (ids != null && !ids.isEmpty()) {

                throw new RuntimeException(
                        "Selected IMEI does not belong to this product"
                );
            }

            return;
        }


        /*
         * =====================================================
         * IMEI REQUIRED
         * =====================================================
         */

        if (ids == null || ids.isEmpty()) {

            throw new RuntimeException(
                    "Select the IMEI for every tracked mobile unit being billed"
            );
        }


        /*
         * =====================================================
         * DUPLICATE CHECK
         * =====================================================
         */

        Set<Long> uniqueIds =
                new HashSet<>(ids);

        if (uniqueIds.size() != ids.size()) {

            throw new RuntimeException(
                    "Duplicate IMEI selection is not allowed"
            );
        }


        /*
         * =====================================================
         * QUANTITY MUST MATCH IMEI COUNT
         * =====================================================
         */

        if (ids.size() != quantity) {

            throw new RuntimeException(
                    "Select one unique IMEI for every mobile unit being billed"
            );
        }


        /*
         * =====================================================
         * VERIFY IMEI
         * =====================================================
         */

        List<PurchaseItemSerial> serials =
                serialRepository
                        .findAvailableForSale(
                                inventorySeller.getId(),
                                productId,
                                ids
                        );


        /*
         * =====================================================
         * ALL IMEI MUST BE AVAILABLE
         * =====================================================
         */

        if (serials.size() != ids.size()) {

            throw new RuntimeException(
                    "One or more selected IMEIs are unavailable or already sold"
            );
        }

        validateVariantOwnership(serials, billItem);


        /*
         * =====================================================
         * MARK SOLD
         * =====================================================
         */

        LocalDateTime soldAt =
                LocalDateTime.now();


        for (PurchaseItemSerial serial : serials) {

            /*
             * Double safety check
             */

            if (serial.getSoldOfflineBillItem() != null) {

                throw new RuntimeException(
                        "IMEI "
                                + displayImei(serial)
                                + " is already sold"
                );
            }


            if (!serial.isAvailable()) {

                throw new RuntimeException(
                        "IMEI "
                                + displayImei(serial)
                                + " is not available"
                );
            }


            /*
             * Link physical unit to bill item
             */

            serial.setSoldOfflineBillItem(
                    billItem
            );


            /*
             * IMPORTANT:
             *
             * Update status.
             */

            serial.setStatus(
                    "SOLD"
            );


            /*
             * IMPORTANT:
             *
             * Save exact sale time.
             */

            serial.setSoldAt(
                    soldAt
            );
        }


        /*
         * =====================================================
         * SAVE
         * =====================================================
         */

        serialRepository.saveAll(serials);
    }


    /**
     * Holds selected physical units for a Razorpay POS bill. A reservation
     * keeps the same IMEI from appearing in another seller bill, but it does
     * not mark the unit sold until the gateway payment has been verified.
     */
    @Transactional
    public void reserveForOfflineBill(
            User billingUser,
            Long productId,
            int quantity,
            List<Long> ids,
            OfflineBillItem billItem) {

        if (billingUser == null || productId == null || quantity <= 0 || billItem == null) {
            throw new RuntimeException("Valid seller, product, quantity and bill item are required");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        User inventorySeller = billingUser.getRole() == Role.ADMIN ? product.getSeller() : billingUser;
        if (inventorySeller == null) {
            throw new RuntimeException("Product is not assigned to a seller");
        }
        if (billingUser.getRole() == Role.SELLER
                && (product.getSeller() == null || !billingUser.getId().equals(product.getSeller().getId()))) {
            throw new RuntimeException("You can reserve only your own product stock");
        }

        List<PurchaseItemSerial> available = serialRepository
                .findAvailableBySellerAndProduct(inventorySeller.getId(), productId);
        if (available.isEmpty()) {
            if (ids != null && !ids.isEmpty()) {
                throw new RuntimeException("Selected IMEI does not belong to this product");
            }
            return;
        }
        if (ids == null || ids.size() != quantity || new HashSet<>(ids).size() != ids.size()) {
            throw new RuntimeException("Select one unique available IMEI for every tracked unit");
        }

        List<PurchaseItemSerial> serials = serialRepository
                .findAvailableForSale(inventorySeller.getId(), productId, ids);
        if (serials.size() != ids.size()) {
            throw new RuntimeException("One or more selected IMEIs are unavailable or already sold");
        }
        validateVariantOwnership(serials, billItem);
        for (PurchaseItemSerial serial : serials) {
            serial.setReservedOfflineBillItem(billItem);
            serial.setStatus("RESERVED");
        }
        serialRepository.saveAll(serials);
    }

    /** Converts only this POS bill's prior reservations into final sold units. */
    @Transactional
    public void finalizeOfflineBillReservation(OfflineBillItem billItem) {
        if (billItem == null || billItem.getId() == null) return;
        List<PurchaseItemSerial> serials = serialRepository.findReservedForOfflineBillItem(billItem.getId());
        LocalDateTime soldAt = LocalDateTime.now();
        for (PurchaseItemSerial serial : serials) {
            if (!serial.isReserved() || serial.getSoldOfflineBillItem() != null) {
                throw new RuntimeException("A reserved IMEI cannot be finalized safely");
            }
            serial.setReservedOfflineBillItem(null);
            serial.setSoldOfflineBillItem(billItem);
            serial.setStatus("SOLD");
            serial.setSoldAt(soldAt);
        }
        serialRepository.saveAll(serials);
    }

    /** A serial inherits its variant from its purchase line. This prevents an
     * IMEI received for (say) Black/256GB from being billed as Black/512GB. */
    private void validateVariantOwnership(List<PurchaseItemSerial> serials, OfflineBillItem billItem) {
        if (billItem == null || billItem.getProductVariantId() == null) return;
        Long selected = billItem.getProductVariantId();
        boolean mismatch = serials.stream().anyMatch(serial -> serial.getPurchaseItem() == null
                || !selected.equals(serial.getPurchaseItem().getProductVariantId()));
        if (mismatch) throw new RuntimeException("Selected IMEI belongs to a different product variant");
    }


    /*
     * =========================================================
     * GET SERIAL BY ID
     * =========================================================
     */

    @Transactional(readOnly = true)
    public PurchaseItemSerial getSerialById(
            Long serialId) {

        if (serialId == null) {

            throw new RuntimeException(
                    "Serial ID is required"
            );
        }

        return serialRepository
                .findById(serialId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "IMEI / Serial record not found"
                        )
                );
    }


    /*
     * =========================================================
     * GET IMEI HISTORY
     * =========================================================
     */

    @Transactional(readOnly = true)
    public PurchaseItemSerial getImeiHistory(
            String imei) {

        if (imei == null
                || imei.isBlank()) {

            throw new RuntimeException(
                    "IMEI is required"
            );
        }

        String normalizedImei =
                imei.trim();


        return serialRepository
                .findByImei1OrImei2(
                        normalizedImei,
                        normalizedImei
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "IMEI not found: "
                                        + normalizedImei
                        )
                );
    }


    /*
     * =========================================================
     * CHECK IMEI SOLD STATUS
     * =========================================================
     */

    @Transactional(readOnly = true)
    public boolean isSold(
            String imei) {

        PurchaseItemSerial serial =
                getImeiHistory(imei);

        return serial.isSold();
    }


    /*
     * =========================================================
     * DISPLAY IMEI
     * =========================================================
     */

    private String displayImei(
            PurchaseItemSerial serial) {

        if (serial.getImei1() != null
                && !serial.getImei1().isBlank()) {

            return serial.getImei1();
        }

        if (serial.getImei2() != null
                && !serial.getImei2().isBlank()) {

            return serial.getImei2();
        }

        if (serial.getSerialNumber() != null
                && !serial.getSerialNumber().isBlank()) {

            return serial.getSerialNumber();
        }

        return "Unknown";
    }


    /*
     * =========================================================
     * SELLER VALIDATION
     * =========================================================
     */

    private User seller(
            String email) {

        if (email == null
                || email.isBlank()) {

            throw new RuntimeException(
                    "Seller email is required"
            );
        }


        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Seller not found"
                                )
                        );


        if (user.getRole() != Role.SELLER) {

            throw new RuntimeException(
                    "Only sellers can access IMEI inventory"
            );
        }


        if (!user.isEnabled()) {

            throw new RuntimeException(
                    "Only active sellers can access IMEI inventory"
            );
        }


        return user;
    }
}
