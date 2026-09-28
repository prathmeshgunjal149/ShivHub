package com.shivhub.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shivhub.backend.dto.PurchasePaymentRequest;
import com.shivhub.backend.dto.PurchasePaymentSummaryResponse;
import com.shivhub.backend.dto.DistributorPayableBillResponse;
import com.shivhub.backend.entity.Purchase;
import com.shivhub.backend.entity.PurchasePayment;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.PurchaseStatus;
import com.shivhub.backend.repository.PurchasePaymentRepository;
import com.shivhub.backend.repository.PurchaseRepository;
import com.shivhub.backend.repository.DistributorCreditNoteRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PurchasePaymentService {

    private final PurchasePaymentRepository purchasePaymentRepository;

    private final PurchaseRepository purchaseRepository;

    /* Credit notes are purchase-cost adjustments, therefore reduce payable before payment validation. */
    private final DistributorCreditNoteRepository distributorCreditNoteRepository;

    private final EmailService emailService;

    @Transactional(readOnly = true)
    public List<DistributorPayableBillResponse> getPayableBills(User seller) {
        if (seller == null || seller.getId() == null) throw new RuntimeException("Valid seller is required");
        return purchaseRepository.findBySellerIdOrderByPurchaseDateDesc(seller.getId()).stream()
                .map(purchase -> new DistributorPayableBillResponse(purchase.getId(), purchase.getInvoiceNumber(),
                        purchase.getPurchaseDate(), purchase.getGrandTotal(),
                        purchase.getDistributor() != null && purchase.getDistributor().getDistributor() != null
                                ? purchase.getDistributor().getDistributor().getBusinessName() : "Distributor"))
                .toList();
    }


    /*
     * =========================================================
     * ADD PAYMENT
     * =========================================================
     *
     * Seller adds payment made towards distributor purchase.
     *
     * Flow:
     *
     * Seller
     *    ↓
     * Purchase
     *    ↓
     * Validate
     *    ↓
     * Calculate remaining
     *    ↓
     * Save payment
     *    ↓
     * Calculate payment status
     *    ↓
     * Send email to seller
     *
     * =========================================================
     */

    @Transactional
    public PurchasePayment addPayment(
            PurchasePaymentRequest request,
            User seller) {

        /*
         * =====================================================
         * 1. VALIDATE REQUEST
         * =====================================================
         */

        if (request == null) {

            throw new RuntimeException(
                    "Payment request cannot be null"
            );
        }


        /*
         * =====================================================
         * 2. VALIDATE PURCHASE ID
         * =====================================================
         */

        if (request.getPurchaseId() == null) {

            throw new RuntimeException(
                    "Purchase ID is required"
            );
        }


        /*
         * =====================================================
         * 3. VALIDATE PAYMENT AMOUNT
         * =====================================================
         */

        if (request.getAmount() == null) {

            throw new RuntimeException(
                    "Payment amount is required"
            );
        }


        if (request.getAmount()
                .compareTo(BigDecimal.ZERO) <= 0) {

            throw new RuntimeException(
                    "Payment amount must be greater than zero"
            );
        }


        /*
         * =====================================================
         * 4. VALIDATE PAYMENT METHOD
         * =====================================================
         */
if (request.getPaymentMethod() == null) {

    throw new RuntimeException(
            "Payment method is required"
    );
}


        /*
         * =====================================================
         * 5. VALIDATE SELLER
         * =====================================================
         */

        if (seller == null
                || seller.getId() == null) {

            throw new RuntimeException(
                    "Valid seller is required"
            );
        }


        /*
         * =====================================================
         * 6. FIND PURCHASE
         * =====================================================
         */

        Purchase purchase =
                purchaseRepository
                        .findById(
                                request.getPurchaseId()
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Purchase not found"
                                )
                        );


        /*
         * =====================================================
         * 7. CHECK SELLER OWNERSHIP
         * =====================================================
         */

        if (purchase.getSeller() == null
                || purchase.getSeller().getId() == null
                || !purchase.getSeller()
                        .getId()
                        .equals(seller.getId())) {

            throw new RuntimeException(
                    "You are not authorized to add payment for this purchase"
            );
        }


        /*
         * =====================================================
         * 8. CHECK PURCHASE STATUS
         * =====================================================
         *
         * CANCELLED purchase cannot receive payment.
         *
         * =====================================================
         */

        if (purchase.getStatus() == null) {

            throw new RuntimeException(
                    "Purchase status is not available"
            );
        }


        if (purchase.getStatus()
                == PurchaseStatus.CANCELLED) {

            throw new RuntimeException(
                    "Payment cannot be added to a cancelled purchase"
            );
        }


        /*
         * =====================================================
         * 9. GET TOTAL ALREADY PAID
         * =====================================================
         */

        BigDecimal totalPaid =
                getTotalPaid(purchase);


        /*
         * =====================================================
         * 10. GET PURCHASE TOTAL
         * =====================================================
         */

        BigDecimal purchaseTotal =
                purchase.getGrandTotal() == null
                        ? BigDecimal.ZERO
                        : purchase.getGrandTotal();


        purchaseTotal =
                purchaseTotal.setScale(
                        2,
                        RoundingMode.HALF_UP
                );

        purchaseTotal = purchaseTotal.subtract(
                distributorCreditNoteRepository.totalForPurchase(purchase)
        ).max(BigDecimal.ZERO);


        /*
         * =====================================================
         * 11. CALCULATE REMAINING
         * =====================================================
         */

        BigDecimal remaining =
                purchaseTotal.subtract(
                        totalPaid
                );


        if (remaining.compareTo(BigDecimal.ZERO) < 0) {

            remaining = BigDecimal.ZERO;
        }


        /*
         * =====================================================
         * 12. CHECK FULLY PAID
         * =====================================================
         */

        if (remaining.compareTo(BigDecimal.ZERO) == 0) {

            throw new RuntimeException(
                    "This purchase is already fully paid"
            );
        }


        /*
         * =====================================================
         * 13. CHECK PAYMENT AMOUNT
         * =====================================================
         */

        BigDecimal paymentAmount =
                request.getAmount()
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );


        if (paymentAmount.compareTo(remaining) > 0) {

            throw new RuntimeException(
                    "Payment amount cannot be greater than remaining due amount. "
                    + "Remaining due: ₹"
                    + remaining
            );
        }


        /*
         * =====================================================
         * 14. CREATE PAYMENT
         * =====================================================
         */

        PurchasePayment payment =
                new PurchasePayment();


        payment.setPurchase(
                purchase
        );


        payment.setAmount(
                paymentAmount
        );


        /*
         * =====================================================
         * PAYMENT METHOD
         * =====================================================
         *
         * PurchasePayment paymentMethod is enum.
         *
         * PurchasePaymentRequest paymentMethod is String.
         *
         * =====================================================
         */

        payment.setPaymentMethod(
                request.getPaymentMethod()
        );


        /*
         * =====================================================
         * 15. PAYMENT DATE
         * =====================================================
         */

        if (request.getPaymentDate() != null) {

            payment.setPaymentDate(
                    request.getPaymentDate()
            );

        } else {

            payment.setPaymentDate(
                    LocalDateTime.now()
            );
        }


        /*
         * =====================================================
         * 16. TRANSACTION REFERENCE
         * =====================================================
         */

        payment.setTransactionReference(
                request.getTransactionReference()
        );


        /*
         * =====================================================
         * 17. NOTES
         * =====================================================
         */

        payment.setNotes(
                request.getNotes()
        );


        /*
         * =====================================================
         * 18. SAVE PAYMENT
         * =====================================================
         */

        PurchasePayment savedPayment =
                purchasePaymentRepository.save(
                        payment
                );


        /*
         * =====================================================
         * 19. CALCULATE NEW PAYMENT STATUS
         * =====================================================
         */

        BigDecimal newTotalPaid =
                totalPaid.add(
                        paymentAmount
                );


        newTotalPaid =
                newTotalPaid.setScale(
                        2,
                        RoundingMode.HALF_UP
                );


        BigDecimal newRemaining =
                purchaseTotal.subtract(
                        newTotalPaid
                );


        if (newRemaining.compareTo(BigDecimal.ZERO) < 0) {

            newRemaining = BigDecimal.ZERO;
        }


        String paymentStatus;


        if (newRemaining.compareTo(BigDecimal.ZERO) == 0) {

            paymentStatus = "PAID";

        } else {

            paymentStatus = "PARTIALLY_PAID";
        }


        /*
         * =====================================================
         * 20. SEND PAYMENT EMAIL TO SELLER
         * =====================================================
         *
         * Payment is already saved successfully.
         *
         * Email failure should NOT undo payment.
         *
         * IMPORTANT:
         *
         * payment.getPaymentMethod() is enum.
         *
         * EmailService expects String.
         *
         * Therefore:
         *
         * payment.getPaymentMethod().name()
         *
         * =====================================================
         */

        try {

            if (seller.getEmail() != null
                    && !seller.getEmail()
                            .trim()
                            .isEmpty()) {


                /*
                 * =================================================
                 * SELLER NAME
                 * =================================================
                 */

                String sellerName =
                        seller.getName() != null
                                ? seller.getName()
                                : "Seller";


                /*
                 * =================================================
                 * DISTRIBUTOR NAME
                 * =================================================
                 */

                String distributorName =
                        "Distributor";


                if (purchase.getDistributor() != null
                        && purchase.getDistributor()
                                .getDistributor() != null) {


                    if (purchase.getDistributor()
                            .getDistributor()
                            .getBusinessName() != null) {


                        distributorName =
                                purchase.getDistributor()
                                        .getDistributor()
                                        .getBusinessName();
                    }
                }


                /*
                 * =================================================
                 * SEND EMAIL
                 * =================================================
                 */

                emailService.sendPurchasePaymentEmail(

                        seller.getEmail(),

                        sellerName,

                        purchase.getInvoiceNumber(),

                        distributorName,

                        paymentAmount,

                        payment.getPaymentMethod().name(),

                        payment.getPaymentDate(),

                        payment.getTransactionReference(),

                        purchaseTotal,

                        newTotalPaid,

                        newRemaining,

                        paymentStatus
                );

                if (purchase.getDistributor() != null
                        && purchase.getDistributor().getDistributor() != null
                        && purchase.getDistributor().getDistributor().getEmail() != null
                        && !purchase.getDistributor().getDistributor().getEmail().isBlank()) {
                    emailService.sendDistributorPaymentEmail(
                            purchase.getDistributor().getDistributor().getEmail(), purchase.getInvoiceNumber(),
                            sellerName, paymentAmount, newTotalPaid, newRemaining);
                }
            }


        } catch (Exception emailException) {

            /*
             * =================================================
             * EMAIL FAILURE SHOULD NOT ROLLBACK PAYMENT
             * =================================================
             */

            System.err.println(
                    "PURCHASE PAYMENT EMAIL FAILED : "
                    + emailException.getMessage()
            );
        }


        /*
         * =====================================================
         * 21. RETURN SAVED PAYMENT
         * =====================================================
         */

        return savedPayment;
    }


    /*
     * =========================================================
     * GET PAYMENTS
     * =========================================================
     *
     * GET all payments for one purchase.
     *
     * =========================================================
     */

    @Transactional(readOnly = true)
    public List<PurchasePayment> getPayments(
            Long purchaseId,
            User seller) {


        Purchase purchase =
                getSellerPurchase(
                        purchaseId,
                        seller
                );


        return purchasePaymentRepository
                .findByPurchaseOrderByPaymentDateDesc(
                        purchase
                );
    }


    /*
     * =========================================================
     * GET PAYMENT SUMMARY
     * =========================================================
     *
     * Returns:
     *
     * Purchase Total
     * Total Paid
     * Remaining Amount
     * Payment Status
     *
     * =========================================================
     */

    @Transactional(readOnly = true)
    public PurchasePaymentSummaryResponse getSummary(
            Long purchaseId,
            User seller) {


        Purchase purchase =
                getSellerPurchase(
                        purchaseId,
                        seller
                );


        /*
         * =====================================================
         * PURCHASE TOTAL
         * =====================================================
         */

        BigDecimal purchaseTotal =
                purchase.getGrandTotal() == null
                        ? BigDecimal.ZERO
                        : purchase.getGrandTotal();


        purchaseTotal =
                purchaseTotal.setScale(
                        2,
                        RoundingMode.HALF_UP
                );

        purchaseTotal = purchaseTotal.subtract(
                distributorCreditNoteRepository.totalForPurchase(purchase)
        ).max(BigDecimal.ZERO);


        /*
         * =====================================================
         * TOTAL PAID
         * =====================================================
         */

        BigDecimal totalPaid =
                getTotalPaid(purchase);


        /*
         * =====================================================
         * REMAINING
         * =====================================================
         */

        BigDecimal remaining =
                purchaseTotal.subtract(
                        totalPaid
                );


        if (remaining.compareTo(BigDecimal.ZERO) < 0) {

            remaining = BigDecimal.ZERO;
        }


        /*
         * =====================================================
         * PAYMENT STATUS
         * =====================================================
         */

        String status;


        if (totalPaid.compareTo(BigDecimal.ZERO) == 0) {

            status = "UNPAID";

        } else if (totalPaid.compareTo(purchaseTotal) >= 0) {

            status = "PAID";

        } else {

            status = "PARTIALLY_PAID";
        }


        /*
         * =====================================================
         * RETURN SUMMARY
         * =====================================================
         */

        return new PurchasePaymentSummaryResponse(

                purchaseTotal,

                totalPaid,

                remaining,

                status
        );
    }


    /*
     * =========================================================
     * TOTAL PAID
     * =========================================================
     */

    private BigDecimal getTotalPaid(
            Purchase purchase) {


        List<PurchasePayment> payments =
                purchasePaymentRepository
                        .findByPurchaseOrderByPaymentDateDesc(
                                purchase
                        );


        BigDecimal total =
                BigDecimal.ZERO;


        for (PurchasePayment payment : payments) {

            if (payment.getAmount() != null) {

                total =
                        total.add(
                                payment.getAmount()
                        );
            }
        }


        return total.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }


    /*
     * =========================================================
     * GET SELLER PURCHASE
     * =========================================================
     *
     * Makes sure that seller can access only
     * his own purchase.
     *
     * =========================================================
     */

    private Purchase getSellerPurchase(
            Long purchaseId,
            User seller) {


        /*
         * =====================================================
         * VALIDATE PURCHASE ID
         * =====================================================
         */

        if (purchaseId == null) {

            throw new RuntimeException(
                    "Purchase ID is required"
            );
        }


        /*
         * =====================================================
         * VALIDATE SELLER
         * =====================================================
         */

        if (seller == null
                || seller.getId() == null) {

            throw new RuntimeException(
                    "Valid seller is required"
            );
        }


        /*
         * =====================================================
         * FIND PURCHASE
         * =====================================================
         */

        Purchase purchase =
                purchaseRepository
                        .findById(
                                purchaseId
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Purchase not found"
                                )
                        );


        /*
         * =====================================================
         * CHECK OWNERSHIP
         * =====================================================
         */

        if (purchase.getSeller() == null
                || purchase.getSeller().getId() == null
                || !purchase.getSeller()
                        .getId()
                        .equals(seller.getId())) {


            throw new RuntimeException(
                    "You are not authorized to access this purchase"
            );
        }


        return purchase;
    }
}
