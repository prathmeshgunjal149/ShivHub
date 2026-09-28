package com.shivhub.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shivhub.backend.dto.CreateOfflineBillRequest;
import com.shivhub.backend.dto.OfflineBillItemRequest;
import com.shivhub.backend.dto.OfflineBillItemResponse;
import com.shivhub.backend.dto.OfflineBillResponse;
import com.shivhub.backend.dto.CustomerReceivableRequest;
import com.shivhub.backend.dto.CustomerPaymentRequest;
import com.shivhub.backend.dto.OfflineCustomerLookupResponse;
import com.shivhub.backend.dto.SalespersonSalesSummaryResponse;
import com.shivhub.backend.entity.OfflineBill;
import com.shivhub.backend.entity.OfflineBillItem;
import com.shivhub.backend.entity.CustomerProfile;
import com.shivhub.backend.entity.Product;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.PaymentMethod;
import com.shivhub.backend.enums.PaymentStatus;
import com.shivhub.backend.repository.OfflineBillRepository;
import com.shivhub.backend.repository.ProductRepository;
import com.shivhub.backend.repository.UserRepository;
import com.shivhub.backend.repository.ShopStaffAssignmentRepository;


/*
 * =========================================================
 * OfflineBillingService
 * =========================================================
 *
 * ShivHub Offline / POS Billing
 *
 * Handles:
 *
 * 1. Seller/Admin verification
 * 2. Customer verification
 * 3. Walk-in customer billing
 * 4. Product validation
 * 5. Stock validation
 * 6. Product offer
 * 7. Item discount
 * 8. Bill discount
 * 9. GST calculation
 * 10. Payment validation
 * 11. Stock reduction
 * 12. Bill saving
 * 13. Invoice PDF generation
 * 14. Customer email
 * 15. Bill retrieval
 *
 * =========================================================
 */

@Service
public class OfflineBillingService {
    @org.springframework.beans.factory.annotation.Autowired
    private FinanceService financeService;


    /*
     * =========================================================
     * REPOSITORIES
     * =========================================================
     */

    private final OfflineBillRepository offlineBillRepository;

    private final ProductRepository productRepository;

    private final UserRepository userRepository;


    /*
     * =========================================================
     * SERVICES
     * =========================================================
     */

    private final OfflineBillInvoiceService
            offlineBillInvoiceService;

    private final EmailService emailService;

    /** Central ShivHub WhatsApp sender; credentials remain server-side. */
    private final WhatsAppNotificationService whatsappNotificationService;

    /* Links a POS sale to the exact purchased IMEI units. */
    private final OfflineBillImeiService offlineBillImeiService;

    private final CustomerReceivableService customerReceivableService;

    private final ShopStaffAssignmentRepository shopStaffAssignmentRepository;

    private final GstCalculator gstCalculator;
    private final GstinValidator gstinValidator;

    private final RazorpayPaymentService razorpayPaymentService;

    private final LoyaltyService loyaltyService;

    private final SellerCustomerService sellerCustomerService;
    @org.springframework.beans.factory.annotation.Autowired private VariantInventoryService variantInventory;


    /*
     * =========================================================
     * CONSTRUCTOR
     * =========================================================
     */

    public OfflineBillingService(
            OfflineBillRepository offlineBillRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            OfflineBillInvoiceService offlineBillInvoiceService,
            EmailService emailService,
            WhatsAppNotificationService whatsappNotificationService,
            OfflineBillImeiService offlineBillImeiService,
            CustomerReceivableService customerReceivableService,
            ShopStaffAssignmentRepository shopStaffAssignmentRepository,
            GstCalculator gstCalculator,
            GstinValidator gstinValidator,
            RazorpayPaymentService razorpayPaymentService,
            LoyaltyService loyaltyService,
            SellerCustomerService sellerCustomerService) {

        this.offlineBillRepository =
                offlineBillRepository;

        this.productRepository =
                productRepository;

        this.userRepository =
                userRepository;

        this.offlineBillInvoiceService =
                offlineBillInvoiceService;

        this.emailService =
                emailService;

        this.whatsappNotificationService = whatsappNotificationService;

        this.offlineBillImeiService =
                offlineBillImeiService;

        this.customerReceivableService = customerReceivableService;
        this.shopStaffAssignmentRepository = shopStaffAssignmentRepository;
        this.gstCalculator = gstCalculator;
        this.gstinValidator = gstinValidator;
        this.razorpayPaymentService = razorpayPaymentService;
        this.loyaltyService = loyaltyService;
        this.sellerCustomerService = sellerCustomerService;
    }


    /*
     * =========================================================
     * CREATE OFFLINE BILL
     * =========================================================
     */

    @Transactional
    public OfflineBillResponse createOfflineBill(
            CreateOfflineBillRequest request,
            String sellerEmail) {


        /*
         * =====================================================
         * REQUEST VALIDATION
         * =====================================================
         */

        if (request == null) {

            throw new RuntimeException(
                    "Billing request cannot be null"
            );
        }


        if (request.getItems() == null
                || request.getItems().isEmpty()) {

            throw new RuntimeException(
                    "At least one product is required"
            );
        }


        /*
         * =====================================================
         * FIND LOGGED-IN SELLER
         * =====================================================
         */

        if (sellerEmail == null
                || sellerEmail.isBlank()) {

            throw new RuntimeException(
                    "Seller email is required"
            );
        }


        User seller =
                userRepository.findByEmail(
                        sellerEmail
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Seller not found"
                        )
                );


        /*
         * =====================================================
         * SELLER ACCOUNT CHECK
         * =====================================================
         */

        if (!seller.isEnabled()) {

            throw new RuntimeException(
                    "Seller account is disabled"
            );
        }


        /*
         * =====================================================
         * ROLE CHECK
         * =====================================================
         */

        if (seller.getRole() == null) {

            throw new RuntimeException(
                    "User role not found"
            );
        }


        String role =
                seller.getRole().name();


        if (!"SELLER".equals(role)
                && !"ADMIN".equals(role)) {

            throw new RuntimeException(
                    "Only Seller or Admin can create offline bills"
            );
        }


        /*
         * =====================================================
         * CUSTOMER
         * =====================================================
         *
         * Registered customer:
         *
         * customerId != null
         *
         * Walk-in customer:
         *
         * customerId == null
         *
         * =====================================================
         */

        User registeredCustomer = null;


        if (request.getCustomerId() != null) {

            registeredCustomer =
                    userRepository.findById(
                            request.getCustomerId()
                    )
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Customer not found"
                            )
                    );


            /*
             * Customer role check
             */

            if (registeredCustomer.getRole() == null
                    || !"CUSTOMER".equals(
                            registeredCustomer
                                    .getRole()
                                    .name())) {

                throw new RuntimeException(
                        "Selected user is not a customer"
                );
            }


            /*
             * Customer enabled check
             */

            if (!registeredCustomer.isEnabled()) {

                throw new RuntimeException(
                        "Customer account is disabled"
                );
            }

            if (request.getCustomerMobile() != null && !request.getCustomerMobile().isBlank()
                    && !CustomerMobileNormalizer.normalizeIndianMobile(request.getCustomerMobile())
                    .equals(CustomerMobileNormalizer.normalizeIndianMobile(registeredCustomer.getMobile()))) {
                throw new IllegalArgumentException("Selected customer does not match the supplied mobile number");
            }
        }

        CustomerProfile customerProfile = sellerCustomerService.resolveCustomerProfileForBilling(
                seller, request.getCustomerProfileId(), request.getCustomerMobile());
        if (customerProfile != null && registeredCustomer != null
                && (customerProfile.getOnlineUser() == null || !registeredCustomer.getId().equals(customerProfile.getOnlineUser().getId()))) {
            throw new IllegalArgumentException("Customer account does not match the selected customer profile");
        }


        /*
         * =====================================================
         * CREATE BILL
         * =====================================================
         */

        OfflineBill bill =
                new OfflineBill();


        bill.setBillNumber(
                generateBillNumber(seller.getInvoicePrefix())
        );


        /*
         * =====================================================
         * SELLER DETAILS
         * =====================================================
         */

        bill.setSellerId(
                seller.getId()
        );


        bill.setSellerName(
                seller.getName()
        );

        /* Owner can select an assigned staff salesperson. Save a name snapshot for reports/invoices. */
        if (request.getSalesPersonId() != null) {
            User salesperson = userRepository.findById(request.getSalesPersonId())
                    .orElseThrow(() -> new RuntimeException("Selected salesperson was not found"));
            boolean assignedToSeller = shopStaffAssignmentRepository.findByStaffAndActiveTrue(salesperson).stream()
                    .anyMatch(assignment -> assignment.getShop() != null
                            && assignment.getShop().getOwner() != null
                            && assignment.getShop().getOwner().getId().equals(seller.getId()));
            if (!assignedToSeller) throw new RuntimeException("Selected salesperson is not assigned to your shop");
            bill.setSalesPersonId(salesperson.getId());
            bill.setSalesPersonName(salesperson.getName());
        } else {
            bill.setSalesPersonId(seller.getId());
            bill.setSalesPersonName(seller.getName());
        }

        bill.setSellerBusinessName(seller.getBusinessName());
        bill.setSellerGstin(seller.getGstin());
        bill.setSellerBusinessAddress(seller.getBusinessAddress());
        bill.setSellerMobile(seller.getMobile());
        bill.setSellerWebsiteUrl(seller.getWebsiteUrl());
        bill.setSellerInstagramUrl(seller.getInstagramUrl());
        bill.setSellerFacebookUrl(seller.getFacebookUrl());
        bill.setSellerWhatsappUrl(seller.getWhatsappUrl());
        bill.setSellerYoutubeUrl(seller.getYoutubeUrl());
        bill.setSellerInvoiceTerms(seller.getInvoiceTerms());
        bill.setWarrantyPeriod(seller.getWarrantyPeriod()); bill.setWarrantyType(seller.getWarrantyType()); bill.setReturnPolicy(seller.getReturnPolicy()); bill.setReplacementPolicy(seller.getReplacementPolicy()); bill.setExchangePolicy(seller.getExchangePolicy()); bill.setRefundPolicy(seller.getRefundPolicy()); bill.setWarrantyTerms(seller.getWarrantyTerms());
        bill.setInvoiceShowAddress(seller.isInvoiceShowAddress()); bill.setInvoiceShowMobile(seller.isInvoiceShowMobile());
        bill.setInvoiceShowGstin(seller.isInvoiceShowGstin()); bill.setInvoiceShowCustomerDetails(seller.isInvoiceShowCustomerDetails()); bill.setInvoiceShowNotes(seller.isInvoiceShowNotes());
        bill.setInvoiceShowWebsite(seller.isInvoiceShowWebsite());
        bill.setInvoiceShowSocialLinks(seller.isInvoiceShowSocialLinks());


        /*
         * =====================================================
         * CUSTOMER DETAILS
         * =====================================================
         */

        if (customerProfile != null) {

            bill.setCustomerProfileId(customerProfile.getId());
            bill.setCustomerId(customerProfile.getOnlineUser() == null ? null : customerProfile.getOnlineUser().getId());
            bill.setCustomerName(customerProfile.getName());
            bill.setCustomerMobile(CustomerMobileNormalizer.normalizeIndianMobile(customerProfile.getMobile()));
            bill.setCustomerEmail(customerProfile.getEmail());
            bill.setCustomerAddress(customerProfile.getAddress());
            bill.setCustomerGstin(request.getCustomerGstin());

        } else if (registeredCustomer != null) {

            bill.setCustomerProfileId(null);

            bill.setCustomerId(
                    registeredCustomer.getId()
            );


            bill.setCustomerName(
                    registeredCustomer.getName()
            );


            bill.setCustomerMobile(
                    registeredCustomer.getMobile()
            );


            bill.setCustomerEmail(
                    registeredCustomer.getEmail()
            );

            bill.setCustomerAddress(request.getCustomerAddress());


            bill.setCustomerGstin(
                    blankToNull(request.getCustomerGstin()) != null
                            ? request.getCustomerGstin()
                            : registeredCustomer.getGstin()
            );

        } else {

            /*
             * =================================================
             * WALK-IN CUSTOMER
             * =================================================
             */

            bill.setCustomerId(null);

            bill.setCustomerProfileId(null);


            bill.setCustomerName(
                    request.getCustomerName()
            );


            bill.setCustomerMobile(
                    request.getCustomerMobile()
            );


            bill.setCustomerEmail(
                    request.getCustomerEmail()
            );

            bill.setCustomerAddress(request.getCustomerAddress());


            bill.setCustomerGstin(
                    request.getCustomerGstin()
            );
        }

        /* Store the exact counter choice on the invoice. An existing online
         * account that has opted out is never overridden by a POS checkbox. */
        bill.setWhatsappConsent(request.isWhatsappConsent()
                && hasBillWhatsappConsent(bill, registeredCustomer, customerProfile));


        /*
         * =====================================================
         * INITIAL AMOUNTS
         * =====================================================
         */

        BigDecimal subtotal =
                BigDecimal.ZERO;


        BigDecimal itemDiscountTotal =
                BigDecimal.ZERO;


        BigDecimal billDiscount =
                safeAmount(
                        request.getDiscount()
                );


        BigDecimal taxableAmount =
                BigDecimal.ZERO;


        BigDecimal cgst =
                BigDecimal.ZERO;


        BigDecimal sgst =
                BigDecimal.ZERO;


        BigDecimal igst =
                BigDecimal.ZERO;


        BigDecimal deliveryCharge =
                safeAmount(
                        request.getDeliveryCharge()
                );


        String normalizedCustomerGstin = gstinValidator.normalizeAndValidate(bill.getCustomerGstin());
        bill.setCustomerGstin(normalizedCustomerGstin);
        bill.setCustomerLegalName(blankToNull(request.getCustomerLegalName()));
        bill.setCustomerTradeName(blankToNull(request.getCustomerTradeName()));
        bill.setPlaceOfSupply(blankToNull(request.getPlaceOfSupply()));
        bill.setCustomerTaxType(normalizedCustomerGstin == null ? "B2C" : "B2B");
        boolean interstateSupply = isInterstateSupply(seller.getGstin(), normalizedCustomerGstin, bill.getPlaceOfSupply());

        /*
         * The method is needed during item processing: Razorpay bills reserve
         * inventory while cash/manual bills deduct it immediately.
         */
        PaymentMethod paymentMethod =
                request.getPaymentMethod();

        if (paymentMethod == null) {
            throw new RuntimeException(
                    "Payment method is required"
            );
        }


        /*
         * =====================================================
         * PROCESS EACH PRODUCT
         * =====================================================
         */

        List<List<Long>> imeiSelections = new ArrayList<>();

        for (OfflineBillItemRequest itemRequest :
                request.getItems()) {


            /*
             * =================================================
             * ITEM VALIDATION
             * =================================================
             */

            if (itemRequest == null) {

                throw new RuntimeException(
                        "Invalid bill item"
                );
            }


            if (itemRequest.getProductId() == null) {

                throw new RuntimeException(
                        "Product ID is required"
                );
            }


            if (itemRequest.getQuantity() == null
                    || itemRequest.getQuantity() <= 0) {

                throw new RuntimeException(
                        "Invalid product quantity"
                );
            }


            /*
             * =================================================
             * FIND PRODUCT
             * =================================================
             */

            Product product =
                    productRepository.findById(
                            itemRequest.getProductId()
                    )
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Product not found: "
                                    + itemRequest.getProductId()
                            )
                    );


            /*
             * A POS seller may bill only inventory owned by that seller.
             * Without this check, one shop could accidentally reduce another
             * shop's stock simply by submitting its product ID.
             * Admin remains allowed to create a bill for any shop product.
             */
            if ("SELLER".equals(role)
                    && (product.getSeller() == null
                    || !seller.getId().equals(product.getSeller().getId()))) {

                throw new RuntimeException(
                        "You can create an offline bill only for your own products"
                );
            }


            /*
             * =================================================
             * PRODUCT APPROVAL
             * =================================================
             */

            if (product.getApprovalStatus() == null
                    || !"APPROVED".equals(
                            product
                                    .getApprovalStatus()
                                    .name())) {

                throw new RuntimeException(
                        "Product is not approved: "
                        + product.getName()
                );
            }


            /*
             * =================================================
             * PRODUCT ACTIVE
             * =================================================
             */

            if (!product.isActive()) {

                throw new RuntimeException(
                        "Product is inactive: "
                        + product.getName()
                );
            }


            /*
             * =================================================
             * STOCK
             * =================================================
             */

            if (product.getStock() == null) {

                throw new RuntimeException(
                        "Stock information not available for "
                        + product.getName()
                );
            }


            if (product.getAvailableStock()
                    < itemRequest.getQuantity()) {

                throw new RuntimeException(
                        "Insufficient stock for "
                        + product.getName()
                        + ". Available stock: "
                        + product.getAvailableStock()
                );
            }


            /*
             * =================================================
             * FINAL SELLING PRICE
             * =================================================
             *
             * Product offer is already handled by:
             *
             * getFinalSellingPrice()
             *
             * =================================================
             */

            com.shivhub.backend.entity.ProductVariant selectedVariant = product.isVariantsEnabled()
                    ? variantInventory.selected(product, itemRequest.getVariantId()) : null;
            if (!product.isVariantsEnabled() && itemRequest.getVariantId() != null) throw new IllegalArgumentException("Select a variant belonging to this product");
            if (selectedVariant != null && selectedVariant.getAvailableStock() < itemRequest.getQuantity()) throw new IllegalArgumentException("Selected variant has insufficient stock");
            BigDecimal unitPrice = selectedVariant == null ? product.getFinalSellingPrice() : selectedVariant.getSellingPriceIncludingGst();

            /*
             * A seller may set a POS sale price for this one invoice. The
             * product, seller ownership, approval and stock are already
             * verified above; absent input continues to use the product price.
             */
            if (selectedVariant == null && itemRequest.getUnitPrice() != null) {
                unitPrice = itemRequest.getUnitPrice();
            }


            if (unitPrice == null) {

                throw new RuntimeException(
                        "Product price is not available for "
                        + product.getName()
                );
            }


            if (unitPrice.compareTo(
                    BigDecimal.ZERO
            ) < 0) {

                throw new RuntimeException(
                        "Invalid product price for "
                        + product.getName()
                );
            }


            unitPrice =
                    unitPrice.setScale(
                            2,
                            RoundingMode.HALF_UP
                    );


            /*
             * =================================================
             * ITEM GROSS
             * =================================================
             */

            BigDecimal itemGross =
                    unitPrice.multiply(
                            BigDecimal.valueOf(
                                    itemRequest.getQuantity()
                            )
                    )
                    .setScale(
                            2,
                            RoundingMode.HALF_UP
                    );


            /*
             * =================================================
             * ITEM DISCOUNT
             * =================================================
             */

            BigDecimal itemDiscount =
                    safeAmount(
                            itemRequest.getDiscount()
                    );


            if (itemDiscount.compareTo(
                    BigDecimal.ZERO
            ) < 0) {

                throw new RuntimeException(
                        "Item discount cannot be negative"
                );
            }


            if (itemDiscount.compareTo(
                    itemGross
            ) > 0) {

                throw new RuntimeException(
                        "Item discount cannot exceed "
                        + "product amount for "
                        + product.getName()
                );
            }


            /*
             * =================================================
             * ITEM FINAL INCLUSIVE AMOUNT
             * =================================================
             *
             * Seller listing price is already GST-inclusive.
             * Discounts entered here reduce the final invoice value.
             * Taxable/GST are reverse-calculated from this inclusive
             * amount so GST is never added a second time.
             * =================================================
             */

            BigDecimal itemInclusiveAmount =
                    itemGross.subtract(
                            itemDiscount
                    )
                    .setScale(
                            2,
                            RoundingMode.HALF_UP
                    );


            /*
             * =================================================
             * GST RATE
             * =================================================
             */

            BigDecimal gstRate =
                    safeAmount(
                            itemRequest.getGstRate()
                    );


            if (gstRate.compareTo(
                    BigDecimal.ZERO
            ) < 0
                    || gstRate.compareTo(
                            new BigDecimal("100")
                    ) > 0) {

                throw new RuntimeException(
                        "GST rate must be between 0 and 100"
                );
            }


            /*
             * =================================================
             * GST CALCULATION FROM INCLUSIVE PRICE
             * =================================================
             */

            GstBreakup gstBreakup =
                    gstCalculator.inclusive(itemInclusiveAmount, gstRate, interstateSupply);

            BigDecimal itemTaxableAmount = gstBreakup.taxableAmount();
            BigDecimal itemCgst = gstBreakup.cgst();
            BigDecimal itemSgst = gstBreakup.sgst();
            BigDecimal itemIgst = gstBreakup.igst();


            /*
             * =================================================
             * ITEM TOTAL
             * =================================================
             */

            BigDecimal itemTotal =
                    itemInclusiveAmount;


            /*
             * =================================================
             * CREATE BILL ITEM
             * =================================================
             */

            OfflineBillItem billItem =
                    new OfflineBillItem();
            if (selectedVariant != null) { billItem.setProductVariantId(selectedVariant.getId()); billItem.setSelectedAttributes(selectedVariant.getAttributesJson()); }


            billItem.setProductId(
                    product.getId()
            );


            billItem.setProductName(
                    product.getName()
            );


            billItem.setSku(null);
            billItem.setMrp(product.getPrice());


            billItem.setQuantity(
                    itemRequest.getQuantity()
            );


            billItem.setUnitPrice(
                    unitPrice
            );


            billItem.setDiscount(
                    itemDiscount
            );


            billItem.setTaxableAmount(
                    itemTaxableAmount
            );


            billItem.setGstRate(
                    gstRate
            );


            billItem.setCgst(
                    itemCgst
            );


            billItem.setSgst(
                    itemSgst
            );


            billItem.setIgst(
                    itemIgst
            );


            billItem.setTotalPrice(
                    itemTotal
            );


            /*
             * Add item to bill
             */

            bill.addItem(
                    billItem
            );

            /* Keep the selected physical devices in the same order as bill items.
             * They are linked after the bill item gets its database id. */
            imeiSelections.add(
                    itemRequest.getPurchaseSerialIds()
            );


            /*
             * =================================================
             * TOTALS
             * =================================================
             */

            subtotal =
                    subtotal.add(
                            itemGross
                    );


            itemDiscountTotal =
                    itemDiscountTotal.add(
                            itemDiscount
                    );


            taxableAmount =
                    taxableAmount.add(
                            itemTaxableAmount
                    );


            cgst =
                    cgst.add(
                            itemCgst
                    );


            sgst =
                    sgst.add(
                            itemSgst
                    );


            igst =
                    igst.add(
                            itemIgst
                    );


            /*
             * =================================================
             * REDUCE STOCK
             * =================================================
             */

            int stockBefore = product.getStock();
            int stockAfter = stockBefore - itemRequest.getQuantity();
            if (selectedVariant != null) {
                if (paymentMethod == PaymentMethod.RAZORPAY) variantInventory.reserve(product, selectedVariant.getId(), itemRequest.getQuantity());
                else variantInventory.sell(product, selectedVariant.getId(), itemRequest.getQuantity(), false, "OFFLINE_VARIANT_SALE", null);
            } else if (paymentMethod == PaymentMethod.RAZORPAY) {
                // The item is held for the exact payment session. It is not a
                // completed sale until the backend verifies Razorpay payment.
                int reserved = product.getReservedStock() == null ? 0 : product.getReservedStock();
                product.setReservedStock(reserved + itemRequest.getQuantity());
            } else {
                product.setStock(stockAfter);
            }


            productRepository.save(
                    product
            );

            /* POS sales and online sales use the same below-two low-stock alert rule. */
            if (paymentMethod != PaymentMethod.RAZORPAY) {
                sendLowStockAlertSafely(product, stockBefore, stockAfter);
            }
        }


        /*
         * =====================================================
         * TOTAL DISCOUNT
         * =====================================================
         */

        BigDecimal totalDiscount =
                itemDiscountTotal
                        .add(billDiscount)
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );


        /*
         * =====================================================
         * BILL DISCOUNT VALIDATION
         * =====================================================
         */

        BigDecimal remainingAmountBeforeBillDiscount =
                subtotal.subtract(
                        itemDiscountTotal
                );


        if (billDiscount.compareTo(
                BigDecimal.ZERO
        ) < 0) {

            throw new RuntimeException(
                    "Bill discount cannot be negative"
            );
        }


        if (billDiscount.compareTo(
                remainingAmountBeforeBillDiscount
        ) > 0) {

            throw new RuntimeException(
                    "Bill discount is greater than "
                    + "the remaining bill amount"
            );
        }


        /*
         * =====================================================
         * ADJUST TAXABLE/GST FOR BILL DISCOUNT
         * =====================================================
         */

        BigDecimal originalTaxable =
                taxableAmount;

        BigDecimal finalTaxableAmount =
                taxableAmount;


        if (remainingAmountBeforeBillDiscount.compareTo(
                BigDecimal.ZERO
        ) > 0
                && billDiscount.compareTo(
                        BigDecimal.ZERO
                ) > 0) {


            BigDecimal discountRatio =
                    billDiscount.divide(
                            remainingAmountBeforeBillDiscount,
                            8,
                            RoundingMode.HALF_UP
                    );

            BigDecimal taxableReduction =
                    originalTaxable.multiply(
                            discountRatio
                    );


            BigDecimal gstReductionCgst =
                    cgst.multiply(
                            discountRatio
                    );


            BigDecimal gstReductionSgst =
                    sgst.multiply(
                            discountRatio
                    );


            BigDecimal gstReductionIgst =
                    igst.multiply(
                            discountRatio
                    );


            finalTaxableAmount =
                    originalTaxable.subtract(
                            taxableReduction
                    )
                    .max(
                            BigDecimal.ZERO
                    )
                    .setScale(
                            2,
                            RoundingMode.HALF_UP
                    );


            cgst =
                    cgst.subtract(
                            gstReductionCgst
                    )
                    .max(
                            BigDecimal.ZERO
                    )
                    .setScale(
                            2,
                            RoundingMode.HALF_UP
                    );


            sgst =
                    sgst.subtract(
                            gstReductionSgst
                    )
                    .max(
                            BigDecimal.ZERO
                    )
                    .setScale(
                            2,
                            RoundingMode.HALF_UP
                    );


            igst =
                    igst.subtract(
                            gstReductionIgst
                    )
                    .max(
                            BigDecimal.ZERO
                    )
                    .setScale(
                            2,
                            RoundingMode.HALF_UP
                    );
        }

        finalTaxableAmount =
                finalTaxableAmount.setScale(
                        2,
                        RoundingMode.HALF_UP
                );


        /*
         * =====================================================
         * GRAND TOTAL
         * =====================================================
         */

        BigDecimal grandTotal =
                finalTaxableAmount
                        .add(cgst)
                        .add(sgst)
                        .add(igst)
                        .add(deliveryCharge)
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        LoyaltyService.RedemptionQuote loyaltyRedemption = loyaltyService.quoteOfflineRedemption(
                request.getCustomerId(), request.getCustomerMobile(), seller.getId(),
                request.getLoyaltyPointsToRedeem(), grandTotal);
        if (loyaltyRedemption.points() > 0) {
            grandTotal = grandTotal.subtract(loyaltyRedemption.discount()).max(BigDecimal.ZERO)
                    .setScale(2, RoundingMode.HALF_UP);
            bill.setLoyaltyPointsRedeemed(Math.toIntExact(loyaltyRedemption.points()));
            bill.setLoyaltyDiscount(loyaltyRedemption.discount());
        }


        /*
         * =====================================================
         * PAYMENT METHOD
         * =====================================================
         */

        /*
         * =====================================================
         * PAYMENT AMOUNT
         * =====================================================
         */

        if (request.getPaymentAmount() == null) {

            throw new RuntimeException(
                    "Payment amount is required"
            );
        }


        BigDecimal paymentAmount =
                request.getPaymentAmount()
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        if (paymentMethod == PaymentMethod.FINANCE) {
            if (request.getFinance() == null || request.getFinance().downpayment() == null)
                throw new IllegalArgumentException("Finance details and downpayment are required");
            paymentAmount = request.getFinance().downpayment().setScale(2, RoundingMode.HALF_UP);
        }


        if (paymentAmount.compareTo(
                BigDecimal.ZERO
        ) < 0) {

            throw new RuntimeException(
                    "Payment amount cannot be negative"
            );
        }


        /*
         * =====================================================
         * PAYMENT DEBUG
         * =====================================================
         */

        System.out.println(
                "========================================"
        );

        System.out.println(
                "OFFLINE BILL PAYMENT DEBUG"
        );

        System.out.println(
                "PAYMENT METHOD = "
                + paymentMethod
        );

        System.out.println(
                "PAYMENT AMOUNT = ₹"
                + paymentAmount
        );

        System.out.println(
                "GRAND TOTAL    = ₹"
                + grandTotal
        );

        System.out.println(
                "SUBTOTAL       = ₹"
                + subtotal
        );

        System.out.println(
                "ITEM DISCOUNT  = ₹"
                + itemDiscountTotal
        );

        System.out.println(
                "BILL DISCOUNT  = ₹"
                + billDiscount
        );

        System.out.println(
                "TOTAL DISCOUNT = ₹"
                + totalDiscount
        );

        System.out.println(
                "TAXABLE AMOUNT = ₹"
                + finalTaxableAmount
        );

        System.out.println(
                "CGST           = ₹"
                + cgst
        );

        System.out.println(
                "SGST           = ₹"
                + sgst
        );

        System.out.println(
                "IGST           = ₹"
                + igst
        );

        System.out.println(
                "DELIVERY       = ₹"
                + deliveryCharge
        );

        System.out.println(
                "========================================"
        );


        /*
         * =====================================================
         * PAYMENT VALIDATION
         * =====================================================
         *
         * Paid amount must be >= bill total.
         *
         * =====================================================
         */

        if (paymentAmount.compareTo(
                grandTotal
        ) < 0 && false) {

            throw new RuntimeException(
                    "Payment amount is less than grand total. "
                    + "Payment: ₹"
                    + paymentAmount
                    + ", Grand Total: ₹"
                    + grandTotal
            );
        }


        /*
         * =====================================================
         * TRANSACTION ID
         * =====================================================
         */

        if ((paymentMethod == PaymentMethod.UPI
                || paymentMethod == PaymentMethod.CARD
                || paymentMethod == PaymentMethod.BANK_TRANSFER)
                && (request.getTransactionId() == null
                || request.getTransactionId().isBlank())) {

            throw new RuntimeException(
                    "Transaction ID is required for "
                    + paymentMethod
            );
        }


        /*
         * =====================================================
         * SET BILL AMOUNTS
         * =====================================================
         */

        bill.setSubtotal(
                subtotal.setScale(
                        2,
                        RoundingMode.HALF_UP
                )
        );


        bill.setDiscount(
                totalDiscount
        );


        bill.setTaxableAmount(
                finalTaxableAmount
        );


        bill.setCgst(
                cgst
        );


        bill.setSgst(
                sgst
        );


        bill.setIgst(
                igst
        );


        bill.setDeliveryCharge(
                deliveryCharge
        );


        bill.setGrandTotal(
                grandTotal
        );


        /*
         * =====================================================
         * PAYMENT DETAILS
         * =====================================================
         */

        bill.setPaymentMethod(
                paymentMethod
        );


        bill.setPaymentAmount(
                paymentAmount
        );

        bill.setPaymentStatus(paymentAmount.compareTo(grandTotal) >= 0
                ? com.shivhub.backend.enums.PaymentStatus.PAID
                : paymentAmount.signum() > 0
                        ? com.shivhub.backend.enums.PaymentStatus.PARTIALLY_PAID
                        : com.shivhub.backend.enums.PaymentStatus.PENDING);


        bill.setTransactionId(
                request.getTransactionId()
        );


        bill.setNotes(
                request.getNotes()
        );


        /*
         * =====================================================
         * SAVE BILL
         * =====================================================
         */

        OfflineBill savedBill =
                offlineBillRepository.save(
                        bill
                );

        if (paymentMethod == PaymentMethod.FINANCE) {
            financeService.attach(savedBill, request.getFinance());
        }

        if (paymentMethod != PaymentMethod.RAZORPAY) {
            razorpayPaymentService.recordInitialOfflinePayment(
                    savedBill, seller, paymentAmount, paymentMethod,
                    request.getTransactionId(), request.getNotes());
            // An approved finance provider is the tender for the full sale. The
            // cash downpayment remains in payment history, while the provider
            // payout itself is tracked only in FinanceSale settlement records.
            // This keeps sales, loyalty and after-sales eligibility based on the
            // complete invoice—not merely on the collected downpayment.
            if (paymentMethod == PaymentMethod.FINANCE) {
                savedBill.setPaymentStatus(com.shivhub.backend.enums.PaymentStatus.PAID);
                offlineBillRepository.save(savedBill);
            }
            loyaltyService.awardForOfflineBill(savedBill);
        } else {
            savedBill.setPaymentStatus(com.shivhub.backend.enums.PaymentStatus.PENDING);
            savedBill.setPaymentAmount(BigDecimal.ZERO);
            savedBill.setInventoryPending(true);
            offlineBillRepository.save(savedBill);
        }

        /* A tracked mobile cannot be sold again: attach each selected IMEI to this
         * persisted bill line. Normal non-IMEI products continue using stock only. */
        for (int itemIndex = 0; itemIndex < savedBill.getItems().size(); itemIndex++) {
            OfflineBillItem savedItem = savedBill.getItems().get(itemIndex);
            boolean mobileVariant = savedItem.getProductVariantId() != null
                    && productRepository.findById(savedItem.getProductId()).map(ProductConfigurationService::isMobile).orElse(false);
            if (savedItem.getProductVariantId() != null && !mobileVariant) continue;
            if (paymentMethod == PaymentMethod.RAZORPAY) {
                offlineBillImeiService.reserveForOfflineBill(
                        seller,
                        savedItem.getProductId(),
                        savedItem.getQuantity(),
                        imeiSelections.get(itemIndex),
                        savedItem
                );
            } else {
                offlineBillImeiService.markSold(
                        seller,
                        savedItem.getProductId(),
                        savedItem.getQuantity(),
                        imeiSelections.get(itemIndex),
                        savedItem
                );
            }
        }


        if (paymentMethod != PaymentMethod.RAZORPAY && paymentMethod != PaymentMethod.FINANCE) {
            createCustomerBalanceForPartialPayment(savedBill, seller, request);
        }

        /*
         * =====================================================
         * CUSTOMER EMAIL + PDF
         * =====================================================
         *
         * IMPORTANT:
         *
         * Bill is already saved.
         *
         * If email fails:
         *
         * Bill remains saved.
         *
         * =====================================================
         */

        if (paymentMethod != PaymentMethod.RAZORPAY) {
            if (paymentMethod == PaymentMethod.FINANCE) {
                org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override public void afterCommit() { sendOfflineBillNotificationsSafely(savedBill); }
                    });
            } else sendOfflineBillNotificationsSafely(savedBill);
        }


        /*
         * =====================================================
         * RETURN RESPONSE
         * =====================================================
         */

        return convertToResponse(
                savedBill
        );
    }


    /** Creates the separate balance receipt ledger for an invoice that is not fully paid. */
    private void createCustomerBalanceForPartialPayment(OfflineBill bill, User seller, CreateOfflineBillRequest request) {
        if (bill.getPaymentAmount() == null || bill.getPaymentAmount().compareTo(bill.getGrandTotal()) >= 0) return;
        CustomerReceivableRequest receivable = new CustomerReceivableRequest();
        receivable.setCustomerName(bill.getCustomerName());
        receivable.setCustomerMobile(bill.getCustomerMobile());
        receivable.setCustomerEmail(bill.getCustomerEmail());
        receivable.setInvoiceNumber(bill.getBillNumber());
        receivable.setProductDetails(bill.getItems().stream().map(OfflineBillItem::getProductName)
                .collect(java.util.stream.Collectors.joining(", ")));
        receivable.setSaleAmount(bill.getGrandTotal());
        receivable.setDueDate(request.getBalanceDueDate());
        receivable.setNotes("Auto-created balance for invoice " + bill.getBillNumber());
        Long receivableId = (Long) customerReceivableService.create(receivable, seller).get("id");
        if (bill.getPaymentAmount().compareTo(BigDecimal.ZERO) > 0) {
            CustomerPaymentRequest firstPayment = new CustomerPaymentRequest();
            firstPayment.setAmount(bill.getPaymentAmount());
            firstPayment.setPaymentMethod(bill.getPaymentMethod());
            firstPayment.setTransactionReference(bill.getTransactionId());
            firstPayment.setNotes("Initial payment for invoice " + bill.getBillNumber());
            customerReceivableService.receive(receivableId, firstPayment, seller);
        }
    }

    /*
     * =========================================================
     * SEND OFFLINE BILL EMAIL SAFELY
     * =========================================================
     */

    private void sendOfflineBillNotificationsSafely(OfflineBill bill) {
        sendOfflineBillEmailSafely(bill);
        sendOfflineBillWhatsAppSafely(bill);
    }

    private void sendOfflineBillWhatsAppSafely(OfflineBill bill) {
        if (!bill.isWhatsappConsent()) return;
        try {
            whatsappNotificationService.sendOptedInMobileEvent(
                    bill.getCustomerMobile(),
                    WhatsAppNotificationEvent.INVOICE_GENERATED,
                    safeString(bill.getCustomerName(), "Customer"),
                    bill.getBillNumber(),
                    List.of(
                            safeString(bill.getCustomerName(), "Customer"),
                            safeString(bill.getBillNumber(), ""),
                            money(bill.getGrandTotal()),
                            bill.getPaymentMethod() == null ? "-" : bill.getPaymentMethod().name()));
        } catch (Exception ignored) {
            // WhatsApp delivery never changes an already-saved bill.
        }
    }

    private void sendOfflineBillEmailSafely(
            OfflineBill bill) {


        String customerEmail =
                bill.getCustomerEmail();


        /*
         * No email = nothing to send.
         *
         * This is allowed for walk-in customers.
         */

        if (customerEmail == null
                || customerEmail.isBlank()) {

            System.out.println(
                    "OFFLINE BILL EMAIL SKIPPED"
            );

            System.out.println(
                    "Reason: Customer email not available"
            );

            return;
        }


        try {

            /*
             * Generate invoice PDF.
             */

            byte[] invoicePdf =
                    offlineBillInvoiceService
                            .generateInvoicePdf(
                                    bill
                            );


            /*
             * Send email.
             */

            emailService.sendOfflineBillEmail(
                    customerEmail,
                    safeString(
                            bill.getCustomerName(),
                            "Customer"
                    ),
                    bill.getBillNumber(),
                    money(
                            bill.getGrandTotal()
                    ),
                    bill.getPaymentMethod() == null
                            ? "-"
                            : bill.getPaymentMethod().name(),
                    invoicePdf,
                    false
            );


            System.out.println(
                    "========================================"
            );

            System.out.println(
                    "OFFLINE BILL EMAIL SENT"
            );

            System.out.println(
                    "BILL   : "
                    + bill.getBillNumber()
            );

            System.out.println(
                    "TO     : "
                    + customerEmail
            );

            System.out.println(
                    "AMOUNT : ₹"
                    + money(
                            bill.getGrandTotal()
                    )
            );

            System.out.println(
                    "========================================"
            );


        } catch (Exception exception) {

            /*
             * IMPORTANT:
             *
             * Do NOT throw exception here.
             *
             * Bill has already been saved.
             */

            System.err.println(
                    "========================================"
            );

            System.err.println(
                    "OFFLINE BILL EMAIL FAILED"
            );

            System.err.println(
                    "BILL : "
                    + bill.getBillNumber()
            );

            System.err.println(
                    "TO   : "
                    + customerEmail
            );

            System.err.println(
                    "ERROR: "
                    + exception.getMessage()
            );

            System.err.println(
                    "========================================"
            );

            exception.printStackTrace();
        }
    }


    /*
     * =========================================================
     * GET BILL BY ID
     * =========================================================
     */

    @Transactional(readOnly = true)
    public OfflineBillResponse getBillById(
            Long billId) {


        if (billId == null) {

            throw new RuntimeException(
                    "Bill ID is required"
            );
        }


        OfflineBill bill =
                offlineBillRepository.findById(
                        billId
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Offline bill not found"
                        )
                );


        return convertToResponse(
                bill
        );
    }


    /*
     * =========================================================
     * GET BILL BY BILL NUMBER
     * =========================================================
     */

    @Transactional(readOnly = true)
    public OfflineBillResponse getBillByNumber(
            String billNumber) {


        if (billNumber == null
                || billNumber.isBlank()) {

            throw new RuntimeException(
                    "Bill number is required"
            );
        }


        OfflineBill bill =
                offlineBillRepository
                        .findByBillNumber(
                                billNumber
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Offline bill not found: "
                                        + billNumber
                                )
                        );


        return convertToResponse(
                bill
        );
    }


    /*
     * =========================================================
     * GET ALL BILLS
     * =========================================================
     */

    @Transactional(readOnly = true)
    public List<OfflineBillResponse> getAllBills() {

        return offlineBillRepository
                .findAll()
                .stream()
                .map(
                        this::convertToResponse
                )
                .toList();
    }


    /*
     * =========================================================
     * GET SELLER BILLS
     * =========================================================
     */

    @Transactional(readOnly = true)
    public List<OfflineBillResponse> getSellerBills(
            String sellerEmail) {


        if (sellerEmail == null
                || sellerEmail.isBlank()) {

            throw new RuntimeException(
                    "Seller email is required"
            );
        }


        User seller =
                userRepository.findByEmail(
                        sellerEmail
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Seller not found"
                        )
                );


        return offlineBillRepository
                .findBySellerIdOrderByCreatedAtDesc(
                        seller.getId()
                )
                .stream()
                .map(
                        this::convertToResponse
                )
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SalespersonSalesSummaryResponse> getSalespersonSalesSummary(String sellerEmail) {
        User seller = userRepository.findByEmail(sellerEmail)
                .orElseThrow(() -> new RuntimeException("Seller not found"));
        if (seller.getRole() == null || !"SELLER".equals(seller.getRole().name())) {
            throw new RuntimeException("Only seller can view salesperson sales");
        }
        Map<String, SalespersonSalesSummaryResponse> summary = new LinkedHashMap<>();
        for (OfflineBill bill : offlineBillRepository.findBySellerIdOrderByCreatedAtDesc(seller.getId())) {
            Long id = bill.getSalesPersonId() == null ? seller.getId() : bill.getSalesPersonId();
            String name = bill.getSalesPersonName() == null || bill.getSalesPersonName().isBlank()
                    ? seller.getName() : bill.getSalesPersonName();
            String key = String.valueOf(id);
            SalespersonSalesSummaryResponse current = summary.get(key);
            BigDecimal salesTotal = (current == null ? BigDecimal.ZERO : current.salesTotal())
                    .add(bill.getGrandTotal() == null ? BigDecimal.ZERO : bill.getGrandTotal());
            BigDecimal discountTotal = (current == null ? BigDecimal.ZERO : current.discountTotal())
                    .add(bill.getDiscount() == null ? BigDecimal.ZERO : bill.getDiscount());
            BigDecimal gstTotal = (current == null ? BigDecimal.ZERO : current.gstTotal())
                    .add(bill.getCgst() == null ? BigDecimal.ZERO : bill.getCgst())
                    .add(bill.getSgst() == null ? BigDecimal.ZERO : bill.getSgst())
                    .add(bill.getIgst() == null ? BigDecimal.ZERO : bill.getIgst());
            long mobileUnits = current == null ? 0 : current.mobileUnitsSold();
            if (bill.getItems() != null) {
                mobileUnits += bill.getItems().stream()
                        .map(OfflineBillItem::getQuantity)
                        .filter(java.util.Objects::nonNull)
                        .mapToLong(Integer::longValue)
                        .sum();
            }
            BigDecimal margin = salesTotal.signum() == 0
                    ? BigDecimal.ZERO
                    : BigDecimal.ZERO;

            summary.put(key, new SalespersonSalesSummaryResponse(
                    id,
                    name,
                    current == null ? 1 : current.billCount() + 1,
                    mobileUnits,
                    salesTotal,
                    discountTotal,
                    gstTotal,
                    salesTotal,
                    BigDecimal.ZERO,
                    margin
            ));
        }
        return new ArrayList<>(summary.values());
    }

    @Transactional(readOnly = true)
    public OfflineCustomerLookupResponse findCustomerForBilling(String sellerEmail, String mobile) {
        if (mobile == null || mobile.isBlank()) {
            throw new RuntimeException("Mobile number is required");
        }

        User seller = userRepository.findByEmail(sellerEmail)
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        User customer = userRepository.findByMobile(mobile.trim())
                .filter(user -> user.getRole() != null && "CUSTOMER".equals(user.getRole().name()))
                .orElseThrow(() -> new RuntimeException("Registered customer not found for this mobile"));

        OfflineBill lastBill = offlineBillRepository
                .findTopBySellerIdAndCustomerMobileOrderByCreatedAtDesc(seller.getId(), mobile.trim())
                .orElse(null);

        return new OfflineCustomerLookupResponse(
                customer.getId(),
                customer.getName(),
                customer.getMobile(),
                customer.getEmail(),
                lastBill == null ? null : lastBill.getCustomerAddress(),
                lastBill == null ? null : lastBill.getCustomerGstin()
        );
    }


    /**
     * Recreates an existing POS invoice for the seller who issued it. This is
     * intentionally seller-scoped so one shop cannot retrieve another shop's
     * customer bill merely by guessing an ID.
     */
    @Transactional(readOnly = true)
    public byte[] generateSellerInvoicePdf(
            Long billId,
            String sellerEmail) {

        if (billId == null) {
            throw new RuntimeException("Bill ID is required");
        }

        User seller = userRepository.findByEmail(sellerEmail)
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        OfflineBill bill = offlineBillRepository.findById(billId)
                .orElseThrow(() -> new RuntimeException("Offline bill not found"));

        if (!seller.isEnabled()
                || seller.getRole() == null
                || !"SELLER".equals(seller.getRole().name())
                || !seller.getId().equals(bill.getSellerId())) {
            throw new RuntimeException("You are not allowed to print this bill");
        }

        // A Razorpay bill is only a final invoice once the server has verified
        // the gateway payment (via checkout verification or webhook).
        if (bill.getPaymentMethod() == PaymentMethod.RAZORPAY
                && bill.getPaymentStatus() != PaymentStatus.PAID) {
            throw new IllegalStateException("Invoice will be available after the Razorpay payment is confirmed");
        }

        return offlineBillInvoiceService.generateInvoicePdf(bill);
    }


    /*
     * =========================================================
     * GET CUSTOMER BILLS
     * =========================================================
     */

    @Transactional(readOnly = true)
    public List<OfflineBillResponse> getCustomerBills(
            Long customerId) {


        if (customerId == null) {

            throw new RuntimeException(
                    "Customer ID is required"
            );
        }


        return offlineBillRepository
                .findByCustomerIdOrderByCreatedAtDesc(
                        customerId
                )
                .stream()
                .map(
                        this::convertToResponse
                )
                .toList();
    }


    /*
     * =========================================================
     * GENERATE BILL NUMBER
     * =========================================================
     */

    private String generateBillNumber(String invoicePrefix) {


        String date =
                LocalDateTime.now()
                        .format(
                                DateTimeFormatter
                                        .ofPattern(
                                                "yyyyMMdd"
                                        )
                        );


        String randomPart =
                UUID.randomUUID()
                        .toString()
                        .substring(
                                0,
                                6
                        )
                        .toUpperCase();


        return ((invoicePrefix == null || invoicePrefix.isBlank()) ? "SH-BILL" : invoicePrefix.trim().toUpperCase()) + "-"
                + date
                + "-"
                + randomPart;
    }


    /*
     * =========================================================
     * SAFE AMOUNT
     * =========================================================
     */

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** GSTIN state code is authoritative when present.  For B2C, a selected
     * place-of-supply state is compared only when a seller state code exists. */
    private boolean isInterstateSupply(String sellerGstin, String customerGstin, String placeOfSupply) {
        String sellerState = gstinValidator.stateCode(sellerGstin);
        String customerState = gstinValidator.stateCode(customerGstin);
        if (sellerState != null && customerState != null) return !sellerState.equals(customerState);
        return sellerState != null && placeOfSupply != null && !placeOfSupply.isBlank()
                && sellerStateFromName(placeOfSupply) != null && !sellerState.equals(sellerStateFromName(placeOfSupply));
    }

    private String sellerStateFromName(String state) {
        // A two-digit state/UT code is accepted for POS; names keep current intra-state behaviour.
        String value = state.trim();
        return value.matches("[0-9]{2}") ? value : null;
    }

    private BigDecimal safeAmount(
            BigDecimal amount) {


        if (amount == null) {

            return BigDecimal.ZERO
                    .setScale(
                            2,
                            RoundingMode.HALF_UP
                    );
        }


        return amount.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }


    /*
     * =========================================================
     * MONEY
     * =========================================================
     */

    private String money(
            BigDecimal amount) {


        if (amount == null) {

            return "0.00";
        }


        return amount
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                )
                .toPlainString();
    }

    private boolean hasBillWhatsappConsent(OfflineBill bill, User registeredCustomer, CustomerProfile customerProfile) {
        String digits = bill.getCustomerMobile() == null ? "" : bill.getCustomerMobile().replaceAll("\\D", "");
        if (digits.length() == 12 && digits.startsWith("91")) digits = digits.substring(2);
        if (!digits.matches("[6-9]\\d{9}")) return false;
        if (registeredCustomer != null && !registeredCustomer.isWhatsappOptIn()) return false;
        return customerProfile == null || customerProfile.getOnlineUser() == null
                || customerProfile.getOnlineUser().isWhatsappOptIn();
    }


    /*
     * =========================================================
     * SAFE STRING
     * =========================================================
     */

    private String safeString(
            String value,
            String fallback) {


        if (value == null
                || value.isBlank()) {

            return fallback;
        }


        return value;
    }


    /*
     * =========================================================
     * CONVERT BILL -> RESPONSE
     * =========================================================
     */

    private OfflineBillResponse convertToResponse(
            OfflineBill bill) {


        List<OfflineBillItemResponse> itemResponses =
                new ArrayList<>();


        if (bill.getItems() != null) {

            itemResponses =
                    bill.getItems()
                            .stream()
                            .map(
                                    this::convertItemToResponse
                            )
                            .toList();
        }


        return new OfflineBillResponse(

                bill.getId(),

                bill.getBillNumber(),

                bill.getSellerId(),

                bill.getSellerName(),

                bill.getSalesPersonId(),

                bill.getSalesPersonName(),

                bill.getCustomerId(),

                bill.getCustomerName(),

                bill.getCustomerMobile(),

                bill.getCustomerEmail(),

                bill.getCustomerGstin(),

                itemResponses,

                bill.getSubtotal(),

                bill.getDiscount(),

                bill.getTaxableAmount(),

                bill.getCgst(),

                bill.getSgst(),

                bill.getIgst(),

                bill.getDeliveryCharge(),

                bill.getGrandTotal(),

                bill.getPaymentMethod(),

                bill.getPaymentStatus(),

                bill.getPaymentAmount(),

                bill.getTransactionId(),

                bill.getNotes(),

                bill.getCreatedAt(),

                bill.getUpdatedAt()
        );
    }


    /* Low-stock warnings are non-blocking; a mail issue must not fail a completed POS bill. */
    private void sendLowStockAlertSafely(Product product, int stockBefore, int stockAfter) {
        if (stockBefore < 2 || stockAfter >= 2 || product.getSeller() == null) return;
        try {
            User seller = product.getSeller();
            if (seller.getEmail() != null && !seller.getEmail().isBlank())
                emailService.sendLowStockAlertEmail(seller.getEmail(), seller.getName(), product.getName(), stockAfter);
        } catch (Exception exception) {
            System.err.println("LOW STOCK EMAIL FAILED: " + exception.getMessage());
        }
    }

    /*
     * =========================================================
     * CONVERT ITEM -> RESPONSE
     * =========================================================
     */

    private OfflineBillItemResponse convertItemToResponse(
            OfflineBillItem item) {


        return new OfflineBillItemResponse(

                item.getId(),

                item.getProductId(),

                item.getProductName(),

                item.getSku(),

                item.getQuantity(),

                item.getUnitPrice(),

                item.getDiscount(),

                item.getTaxableAmount(),

                item.getGstRate(),

                item.getCgst(),

                item.getSgst(),

                item.getIgst(),

                item.getTotalPrice()
        );
    }
}
