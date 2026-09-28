package com.shivhub.backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shivhub.backend.dto.CreateOrderRequest;
import com.shivhub.backend.dto.OrderItemSerialSelectionRequest;
import com.shivhub.backend.dto.OrderItemRequest;
import com.shivhub.backend.dto.OrderItemResponse;
import com.shivhub.backend.dto.OrderResponse;
import com.shivhub.backend.entity.Order;
import com.shivhub.backend.entity.OrderItem;
import com.shivhub.backend.entity.Product;
import com.shivhub.backend.entity.ProductImage;
import com.shivhub.backend.entity.PurchaseItemSerial;
import com.shivhub.backend.entity.Coupon;
import com.shivhub.backend.entity.CouponUsage;
import com.shivhub.backend.entity.CustomerAddress;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.OrderStatus;
import com.shivhub.backend.enums.PaymentMethod;
import com.shivhub.backend.enums.ProductStatus;
import com.shivhub.backend.enums.PaymentStatus;
import com.shivhub.backend.repository.OrderItemRepository;
import com.shivhub.backend.repository.OrderRepository;
import com.shivhub.backend.repository.ProductRepository;
import com.shivhub.backend.repository.PurchaseItemSerialRepository;
import com.shivhub.backend.repository.UserRepository;
import com.shivhub.backend.repository.CouponUsageRepository;


/*
 * =========================================================
 * OrderService
 * =========================================================
 *
 * Handles:
 *
 * 1. Create order
 * 2. Validate customer
 * 3. Validate product
 * 4. Validate stock
 * 5. Calculate totals
 * 6. Reduce stock
 * 7. Save order
 * 8. Generate invoice PDF
 * 9. Send order confirmation email
 * 10. Send invoice as email attachment
 * 11. Get customer orders
 * 12. Get order details
 * 13. Get all orders
 * 14. Get orders by status
 * 15. Update order status
 * 16. Send status update email
 *
 * =========================================================
 */

@Service
public class OrderService {

    private static final Set<String> CUSTOMER_CANCELLATION_REASONS = Set.of(
            "ORDERED_BY_MISTAKE", "CHANGE_OF_MIND", "FOUND_BETTER_PRICE",
            "DELIVERY_TIME_TOO_LONG", "WANT_TO_CHANGE_ADDRESS",
            "WANT_TO_CHANGE_PRODUCT", "OTHER");


    /*
     * =====================================================
     * REPOSITORIES
     * =====================================================
     */

    private final OrderRepository orderRepository;

    private final OrderItemRepository orderItemRepository;

    private final ProductRepository productRepository;

    private final PurchaseItemSerialRepository serialRepository;

    private final UserRepository userRepository;
    private final CouponService couponService;
    private final CouponUsageRepository couponUsageRepository;
    private final ReferralService referralService;
    private final CustomerAddressService customerAddressService;
    private final LoyaltyService loyaltyService;
    @org.springframework.beans.factory.annotation.Autowired
    private VariantInventoryService variantInventory;
    @org.springframework.beans.factory.annotation.Autowired private DeliveryEstimateService deliveryEstimates;
    @org.springframework.beans.factory.annotation.Autowired private OrderDeliveryExpectationService deliveryExpectations;


    /*
     * =====================================================
     * SERVICES
     * =====================================================
     */

    private final EmailService emailService;

    private final InvoiceService invoiceService;


    /*
     * =====================================================
     * CONSTRUCTOR
     * =====================================================
     */

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ProductRepository productRepository,
            PurchaseItemSerialRepository serialRepository,
            UserRepository userRepository,
            CouponService couponService,
            CouponUsageRepository couponUsageRepository,
            ReferralService referralService,
            CustomerAddressService customerAddressService,
            LoyaltyService loyaltyService,
            EmailService emailService,
            InvoiceService invoiceService) {

        this.orderRepository = orderRepository;

        this.orderItemRepository = orderItemRepository;

        this.productRepository = productRepository;
        this.serialRepository = serialRepository;

        this.userRepository = userRepository;
        this.couponService = couponService;
        this.couponUsageRepository = couponUsageRepository;
        this.referralService = referralService;
        this.customerAddressService = customerAddressService;
        this.loyaltyService = loyaltyService;

        this.emailService = emailService;

        this.invoiceService = invoiceService;
    }


    /*
     * =========================================================
     * CREATE ORDER
     * =========================================================
     */

    @Transactional
    public OrderResponse createOrder(
            CreateOrderRequest request,
            String customerEmail) {


        /*
         * =====================================================
         * FIND CUSTOMER
         * =====================================================
         */

        User customer =
                userRepository.findByEmail(
                        customerEmail
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Customer not found"
                        )
                );


        /*
         * =====================================================
         * VERIFY CUSTOMER ROLE
         * =====================================================
         */

        if (customer.getRole() == null
                || !"CUSTOMER".equals(
                        customer.getRole().name())) {

            throw new RuntimeException(
                    "Only customers can place orders"
            );
        }


        /*
         * =====================================================
         * VERIFY CUSTOMER ACCOUNT
         * =====================================================
         */

        if (!customer.isEnabled()) {

            throw new RuntimeException(
                    "Customer account is disabled"
            );
        }


        /*
         * =====================================================
         * CREATE ORDER
         * =====================================================
         */

        Order order = new Order();


        order.setOrderNumber(
                generateOrderNumber()
        );


        order.setCustomerId(
                customer.getId()
        );


        /*
         * New checkout clients send either a saved address id or the first
         * address payload.  Keep the old shippingAddress payload as a
         * compatibility fallback for older released clients.
         */
        Long resolvedDeliveryAddressId = request.getDeliveryAddressId();
        if (request.getDeliveryAddressId() == null && request.getDeliveryAddress() == null) {
            if (request.getShippingAddress() == null || request.getShippingAddress().isBlank()) {
                throw new RuntimeException("A delivery address is required");
            }
            order.setShippingAddress(request.getShippingAddress().trim());
        } else {
            CustomerAddress deliveryAddress = customerAddressService.resolveForOrder(
                    customer,
                    request.getDeliveryAddressId(),
                    request.getDeliveryAddress()
            );
            resolvedDeliveryAddressId = deliveryAddress.getId();
            order.setDeliveryName(deliveryAddress.getRecipientName());
            order.setDeliveryMobile(deliveryAddress.getMobileNumber());
            order.setDeliveryAddressLine1(deliveryAddress.getAddressLine1());
            order.setDeliveryAddressLine2(deliveryAddress.getAddressLine2());
            order.setDeliveryLandmark(deliveryAddress.getLandmark());
            order.setDeliveryCity(deliveryAddress.getCity());
            order.setDeliveryDistrict(deliveryAddress.getDistrict());
            order.setDeliveryState(deliveryAddress.getState());
            order.setDeliveryPincode(deliveryAddress.getPincode());
            order.setShippingAddress(String.join(", ",
                    deliveryAddress.getRecipientName(),
                    deliveryAddress.getMobileNumber(),
                    deliveryAddress.getAddressLine1(),
                    deliveryAddress.getAddressLine2(),
                    deliveryAddress.getCity(),
                    deliveryAddress.getDistrict(),
                    deliveryAddress.getState() + " - " + deliveryAddress.getPincode()
            ));
        }


        /*
         * New order starts as PENDING.
         */

        order.setOrderStatus(
                OrderStatus.PENDING
        );


        /*
         * Payment initially PENDING.
         */

        order.setPaymentStatus(
                PaymentStatus.PENDING
        );

        /* Selection is a preference; a gateway or seller must record payment separately. */
        order.setPaymentMethod(
                request.getPaymentMethod() == null
                        ? PaymentMethod.CASH
                        : request.getPaymentMethod()
        );


        /*
         * =====================================================
         * INITIAL AMOUNTS
         * =====================================================
         */

        BigDecimal subtotal =
                BigDecimal.ZERO;

        BigDecimal discount =
                BigDecimal.ZERO;

        BigDecimal tax =
                BigDecimal.ZERO;

        BigDecimal deliveryCharge =
                BigDecimal.ZERO;
        java.util.Map<Long, com.shivhub.backend.dto.DeliveryEstimateResponse> deliveryBySeller = new java.util.HashMap<>();


        /*
         * =====================================================
         * PROCESS EACH ITEM
         * =====================================================
         */

        for (OrderItemRequest itemRequest :
                request.getItems()) {


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
             * =================================================
             * PRODUCT MUST BE APPROVED
             * =================================================
             */

            if (product.getApprovalStatus()
                    != ProductStatus.APPROVED) {

                throw new RuntimeException(
                        "Product is not approved: "
                        + product.getName()
                );
            }


            /*
             * =================================================
             * PRODUCT MUST BE ACTIVE
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
             * VALIDATE QUANTITY
             * =================================================
             */

            Integer requestedQuantity =
                    itemRequest.getQuantity();


            if (requestedQuantity == null
                    || requestedQuantity <= 0) {

                throw new RuntimeException(
                        "Invalid quantity for product: "
                        + product.getName()
                );
            }


            /*
             * =================================================
             * CHECK STOCK
             * =================================================
             */

            if (product.getAvailableStock()
                    < requestedQuantity) {

                throw new RuntimeException(
                        "Insufficient stock for product: "
                        + product.getName()
                        + ". Available stock: "
                        + product.getAvailableStock()
                );
            }


            /*
             * =================================================
             * GET CURRENT PRODUCT PRICE
             * =================================================
             */

            com.shivhub.backend.entity.ProductVariant selectedVariant = product.isVariantsEnabled()
                    ? variantInventory.reserve(product, itemRequest.getVariantId(), requestedQuantity) : null;
            if (!product.isVariantsEnabled() && itemRequest.getVariantId() != null) throw new IllegalArgumentException("Variant does not belong to this product flow");
            BigDecimal unitPrice = selectedVariant == null ? product.getFinalSellingPrice() : selectedVariant.getSellingPriceIncludingGst();


            if (unitPrice == null
                    || unitPrice.compareTo(
                            BigDecimal.ZERO
                    ) < 0) {

                throw new RuntimeException(
                        "Invalid price for product: "
                        + product.getName()
                );
            }


            /*
             * =================================================
             * CALCULATE ITEM TOTAL
             * =================================================
             */

            BigDecimal itemTotal =
                    unitPrice.multiply(
                            BigDecimal.valueOf(
                                    requestedQuantity
                            )
                    );


            /*
             * =================================================
             * CREATE ORDER ITEM
             * =================================================
             */

            OrderItem orderItem =
                    new OrderItem();
            if (selectedVariant != null) { orderItem.setProductVariantId(selectedVariant.getId()); orderItem.setSelectedAttributes(selectedVariant.getAttributesJson()); }


            orderItem.setProductId(
                    product.getId()
            );


            orderItem.setProductName(
                    product.getName()
            );
            if (ProductConfigurationService.isMobile(product)) {
                Long sellerKey = product.getSeller() == null ? -1L : product.getSeller().getId();
                var estimate = deliveryBySeller.get(sellerKey);
                if (estimate == null) {
                    estimate = deliveryEstimates.forOrder(customer, product, resolvedDeliveryAddressId);
                    deliveryBySeller.put(sellerKey, estimate);
                    if (Boolean.FALSE.equals(estimate.serviceAvailable())) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, estimate.estimatedDeliveryText());
                    deliveryCharge = deliveryCharge.add(estimate.deliveryCharge());
                }
                orderItem.setDeliveryEstimateText(estimate.estimatedDeliveryText() == null ? estimate.message() : estimate.estimatedDeliveryText());
                orderItem.setDeliveryDistanceKm(estimate.distanceKm()); orderItem.setDeliveryRuleId(estimate.ruleId());
            }

            if (product.getSeller() != null) {
                orderItem.setSellerId(product.getSeller().getId());
                orderItem.setSellerName(product.getSeller().getBusinessName() == null || product.getSeller().getBusinessName().isBlank()
                        ? product.getSeller().getName() : product.getSeller().getBusinessName());
                orderItem.setSellerGstin(product.getSeller().getGstin());
                orderItem.setSellerAddress(product.getSeller().getBusinessAddress());
                orderItem.setSellerMobile(product.getSeller().getMobile());
                orderItem.setSellerWarrantyPeriod(product.getSeller().getWarrantyPeriod());
                orderItem.setSellerWarrantyType(product.getSeller().getWarrantyType());
                orderItem.setSellerWarrantyTerms(product.getSeller().getWarrantyTerms());
                orderItem.setSellerReturnPolicy(product.getSeller().getReturnPolicy());
            }


            orderItem.setUnitPrice(
                    unitPrice
            );


            orderItem.setQuantity(
                    requestedQuantity
            );


            orderItem.setTotalPrice(
                    itemTotal
            );


            /*
             * Add item to order.
             */

            order.addItem(
                    orderItem
            );


            /*
             * Add to subtotal.
             */

            subtotal =
                    subtotal.add(
                            itemTotal
                    );


            /*
             * =================================================
             * RESERVE STOCK
             * =================================================
             */

            if (selectedVariant == null) product.setReservedStock(
                    (product.getReservedStock() == null ? 0 : product.getReservedStock())
                            + requestedQuantity
            );


            productRepository.save(
                    product
            );
        }


        /*
         * =====================================================
         * DISCOUNT
         * =====================================================
         */

        Coupon appliedCoupon = null;
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            appliedCoupon = couponService.applyToOrder(request.getCouponCode(), customer.getId(), subtotal);
            discount = couponService.calculateDiscount(appliedCoupon, subtotal);
            order.setCouponCode(appliedCoupon.getCode());
            order.setCouponDiscount(discount);
        }


        /*
         * =====================================================
         * TAX
         * =====================================================
         */

        tax =
                BigDecimal.ZERO;

        /*
         * Loyalty is priced after any coupon and delivery calculation.  The request
         * is never trusted: LoyaltyService validates the authenticated customer's
         * actual balance, 3-points-per-rupee conversion, limits, and final amount.
         */
        BigDecimal amountBeforeLoyalty = subtotal.subtract(discount).add(tax).add(deliveryCharge);
        LoyaltyService.RedemptionQuote loyaltyQuote = loyaltyService.quoteOnlineRedemption(
                customer.getId(), request.getLoyaltyPointsToRedeem(), amountBeforeLoyalty);
        // Kept defensive for older integrations and isolated unit-test doubles that
        // do not yet implement the optional checkout quote.
        if (loyaltyQuote == null) loyaltyQuote = new LoyaltyService.RedemptionQuote(0, BigDecimal.ZERO, 0);
        BigDecimal loyaltyDiscount = loyaltyQuote.discount();
        order.setLoyaltyPointsRedeemed(loyaltyQuote.points());
        order.setLoyaltyDiscount(loyaltyDiscount);
        discount = discount.add(loyaltyDiscount);


        /*
         * =====================================================
         * GRAND TOTAL
         * =====================================================
         */

        BigDecimal grandTotal =
                subtotal
                        .subtract(discount)
                        .add(tax)
                        .add(deliveryCharge);


        /*
         * =====================================================
         * SET ORDER AMOUNTS
         * =====================================================
         */

        order.setSubtotal(
                subtotal
        );


        order.setDiscount(
                discount
        );


        order.setTax(
                tax
        );


        order.setDeliveryCharge(
                deliveryCharge
        );


        order.setGrandTotal(
                grandTotal
        );


        /*
         * =====================================================
         * SAVE ORDER
         * =====================================================
         */

        Order savedOrder =
                orderRepository.save(
                        order
                );

        // This debit is idempotent and protected by a wallet lock. If anything in
        // this order transaction fails, both the order and the points debit roll back.
        loyaltyService.redeemForOnlineOrder(savedOrder);

        // Persist the original, seller-specific promise after the order ID is available.
        // Manual seller/admin updates always go through the isolated expectation service.
        deliveryExpectations.initializeFromDistanceRules(savedOrder, deliveryBySeller);

        if (appliedCoupon != null) {
            CouponUsage usage = new CouponUsage();
            usage.setCouponId(appliedCoupon.getId());
            usage.setCustomerId(customer.getId());
            usage.setOrderId(savedOrder.getId());
            usage.setDiscountAmount(savedOrder.getCouponDiscount());
            usage.setUsedAt(LocalDateTime.now());
            couponUsageRepository.save(usage);
            try {
                emailService.sendCouponUsedEmail(customer.getEmail(), customer.getName(), savedOrder.getOrderNumber(), appliedCoupon.getCode(), discount);
            } catch (Exception emailException) {
                System.err.println("Coupon notification email failed for order " + savedOrder.getOrderNumber());
            }
        }


        /*
         * =====================================================
         * SEND ORDER CONFIRMATION EMAIL
         * =====================================================
         *
         * Order is already saved before email processing.
         *
         * If email fails, order remains successfully created.
         *
         * =====================================================
         */

        if (request.getPaymentMethod() != PaymentMethod.RAZORPAY) try {


            emailService.sendOrderConfirmationEmail(

                    customer.getEmail(),

                    customer.getName(),

                    savedOrder.getOrderNumber(),

                    savedOrder.getGrandTotal()
                            .setScale(2)
                            .toPlainString(),

                    null
            );


        } catch (Exception emailException) {


            /*
             * Do not fail the order if email fails.
             */

            System.err.println(
                    "Order created successfully, "
                    + "but confirmation email failed: "
                    + emailException.getMessage()
            );
        }


        /*
         * =====================================================
         * RETURN RESPONSE
         * =====================================================
         */

        return convertToOrderResponse(
                savedOrder
        );
    }


    /*
     * =========================================================
     * GET CUSTOMER ORDERS BY EMAIL
     * =========================================================
     *
     * Used by:
     *
     * GET /api/orders/my
     *
     * The email comes from JWT Authentication.
     *
     * Customer ID is NOT taken from frontend.
     *
     * =========================================================
     */

    @Transactional(readOnly = true)
    public List<OrderResponse> getCustomerOrdersByEmail(
            String email) {


        /*
         * =====================================================
         * FIND CUSTOMER
         * =====================================================
         */

        User customer =
                userRepository.findByEmail(
                        email
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Customer not found"
                        )
                );


        /*
         * =====================================================
         * VERIFY CUSTOMER ROLE
         * =====================================================
         */

        if (customer.getRole() == null
                || !"CUSTOMER".equals(
                        customer.getRole().name())) {

            throw new RuntimeException(
                    "Only customers can access their orders"
            );
        }


        /*
         * =====================================================
         * GET CUSTOMER ORDERS
         * =====================================================
         */

        return getCustomerOrders(
                customer.getId()
        );
    }


    /*
     * =========================================================
     * GET CUSTOMER ORDERS
     * =========================================================
     */

    @Transactional(readOnly = true)
    public List<OrderResponse> getCustomerOrders(
            Long customerId) {


        /*
         * Verify customer exists.
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
         * Get orders.
         */

        List<Order> orders =
                orderRepository
                        .findByCustomerIdOrderByCreatedAtDesc(
                                customerId
                        );


        /*
         * Convert entities to DTOs.
         */

        return orders.stream()
                .map(
                        this::convertToOrderResponse
                )
                .toList();
    }


    /*
     * =========================================================
     * GET ORDER BY ID
     * =========================================================
     */

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(
            Long orderId) {


        Order order =
                orderRepository.findById(
                        orderId
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Order not found"
                        )
                );


        return convertToOrderResponse(
                order
        );
    }

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

    @Transactional(readOnly = true)
    public OrderResponse getCustomerOrderById(
            Long orderId,
            String customerEmail) {

        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!customer.getId().equals(order.getCustomerId())) {
            throw new RuntimeException("You are not allowed to access this order");
        }
        return convertToOrderResponse(order);
    }


    /*
     * =========================================================
     * GET ALL ORDERS
     * =========================================================
     */

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {

        return orderRepository
                .findAll()
                .stream()
                .map(
                        this::convertToOrderResponse
                )
                .toList();
    }

    /** Generates an invoice for an administrator to download. */
    @Transactional(readOnly = true)
    public byte[] generateInvoice(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (order.getPaymentMethod() == PaymentMethod.RAZORPAY && order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new RuntimeException("Invoice is available after Razorpay payment is verified");
        }
        User customer = userRepository.findById(order.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        return invoiceService.generateInvoicePdf(order, customer);
    }

    /** Generates an invoice only when the logged-in customer owns the order. */
    @Transactional(readOnly = true)
    public byte[] generateCustomerInvoice(Long orderId, String customerEmail) {
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!customer.getId().equals(order.getCustomerId())) {
            throw new RuntimeException("You are not allowed to download this invoice");
        }

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Invoice is not available for cancelled orders");
        }

        if (order.getPaymentMethod() == PaymentMethod.RAZORPAY && order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new RuntimeException("Invoice is available after Razorpay payment is verified");
        }

        return invoiceService.generateInvoicePdf(order, customer);
    }

    @Transactional
    public void reserveOrderSerials(
            Long orderId,
            List<OrderItemSerialSelectionRequest> selections) {

        if (selections == null || selections.isEmpty()) {
            return;
        }

        Order order =
                orderRepository.findById(orderId)
                        .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getOrderStatus() == OrderStatus.CANCELLED
                || order.getOrderStatus() == OrderStatus.DELIVERED
                || order.getStockDeductedAt() != null) {
            throw new RuntimeException("IMEI/serial selection cannot be changed for this order status");
        }

        reserveOrderSerials(order, selections);
    }


    /*
     * =========================================================
     * GET ORDERS BY STATUS
     * =========================================================
     */

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByStatus(
            OrderStatus status) {

        return orderRepository
                .findByOrderStatusOrderByCreatedAtDesc(
                        status
                )
                .stream()
                .map(
                        this::convertToOrderResponse
                )
                .toList();
    }


    /*
     * =========================================================
     * UPDATE ORDER STATUS
     * =========================================================
     *
     * Admin changes order status.
     *
     * Customer receives email after status change.
     *
     * =========================================================
     */

    @Transactional
    public OrderResponse updateOrderStatus(
            Long orderId,
            OrderStatus newStatus) {

        return updateOrderStatus(orderId, newStatus, null);
    }

    @Transactional
    public OrderResponse updateOrderStatus(
            Long orderId,
            OrderStatus newStatus,
            List<OrderItemSerialSelectionRequest> serialSelections) {

        /*
         * =====================================================
         * VALIDATE NEW STATUS
         * =====================================================
         */

        if (newStatus == null) {
            throw new RuntimeException(
                    "Order status cannot be null"
            );
        }


        /*
         * =====================================================
         * FIND ORDER
         * =====================================================
         */

        Order order =
                orderRepository.findById(
                        orderId
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Order not found"
                        )
                );


        /*
         * =====================================================
         * STORE OLD STATUS
         * =====================================================
         */

        OrderStatus oldStatus =
                order.getOrderStatus();


        /*
         * =====================================================
         * PREVENT INVALID DELIVERED UPDATE
         * =====================================================
         */

        if (oldStatus == OrderStatus.DELIVERED
                || oldStatus == OrderStatus.CANCELLED) {

            throw new RuntimeException(
                    "A delivered or cancelled order cannot be changed"
            );
        }


        /*
         * =====================================================
         * NO STATUS CHANGE
         * =====================================================
         */

        if (oldStatus == newStatus) {

            return convertToOrderResponse(
                    order
            );
        }


        if (newStatus == OrderStatus.CANCELLED
                && (oldStatus == OrderStatus.SHIPPED
                || oldStatus == OrderStatus.OUT_FOR_DELIVERY
                || order.getStockDeductedAt() != null)) {
            throw new RuntimeException(
                    "Dispatched orders cannot be cancelled here. Use return flow after product verification"
            );
        }

        if (serialSelections != null && !serialSelections.isEmpty()) {
            reserveOrderSerials(order, serialSelections);
        }

        if (newStatus == OrderStatus.SHIPPED
                || newStatus == OrderStatus.OUT_FOR_DELIVERY
                || newStatus == OrderStatus.DELIVERED) {
            consumeReservedStock(order);
        }

        order.setOrderStatus(newStatus);

        if (newStatus == OrderStatus.CANCELLED) {
            releaseOrderReservation(order);
            order.setCancelledBy("ADMIN");
            order.setCancelledAt(LocalDateTime.now());
            if (order.getCancellationReason() == null) {
                order.setCancellationReason("Cancelled by administrator");
            }
        }


        /*
         * =====================================================
         * SAVE ORDER
         * =====================================================
         */

        Order savedOrder =
                orderRepository.save(
                        order
                );

        if (newStatus == OrderStatus.DELIVERED) {
            referralService.rewardForDeliveredOrder(savedOrder.getCustomerId(), savedOrder.getId());
        }


        /*
         * =====================================================
         * SEND STATUS EMAIL
         * =====================================================
         *
         * Email failure must NOT undo the status update.
         *
         */

        try {

            User customer =
                    userRepository.findById(
                            savedOrder.getCustomerId()
                    )
                    .orElse(null);


            if (customer != null
                    && customer.getEmail() != null
                    && !customer.getEmail().isBlank()) {

                System.out.println(
                        "========================================"
                );
                System.out.println(
                        "ORDER STATUS EMAIL"
                );
                System.out.println(
                        "CUSTOMER : " + customer.getName()
                );
                System.out.println(
                        "EMAIL    : " + customer.getEmail()
                );
                System.out.println(
                        "ORDER    : " + savedOrder.getOrderNumber()
                );
                System.out.println(
                        "OLD      : " + oldStatus
                );
                System.out.println(
                        "NEW      : " + newStatus
                );

                byte[] deliveredInvoicePdf =
                        newStatus == OrderStatus.DELIVERED
                                ? invoiceService.generateInvoicePdf(savedOrder, customer)
                                : null;

                emailService.sendOrderStatusEmail(
                        customer.getEmail(),
                        customer.getName(),
                        savedOrder.getOrderNumber(),
                        newStatus.name(),
                        savedOrder.getDeliveryPersonName(),
                        savedOrder.getDeliveryPersonMobile(),
                        deliveredInvoicePdf
                );

                System.out.println(
                        "ORDER STATUS EMAIL SENT SUCCESSFULLY"
                );
                System.out.println(
                        "========================================"
                );

            } else {

                System.err.println(
                        "ORDER STATUS EMAIL NOT SENT: customer/email not found"
                );
            }


        } catch (Exception emailException) {

            System.err.println(
                    "========================================"
            );
            System.err.println(
                    "ORDER STATUS EMAIL FAILED"
            );
            System.err.println(
                    "ORDER : " + savedOrder.getOrderNumber()
            );
            System.err.println(
                    "STATUS : " + newStatus
            );
            System.err.println(
                    "ERROR : " + emailException.getMessage()
            );
            System.err.println(
                    "========================================"
            );

            emailException.printStackTrace();
        }


        /*
         * =====================================================
         * RETURN UPDATED RESPONSE
         * =====================================================
         */

        return convertToOrderResponse(
                savedOrder
        );
    }

    /** Applies a seller-requested status only after an administrator approves it. */
    @Transactional
    public OrderResponse approveSellerStatusRequest(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        OrderStatus requested = order.getSellerRequestedStatus();
        if (requested == null) throw new RuntimeException("No seller status request is pending for this order");
        OrderResponse response = updateOrderStatus(orderId, requested);
        Order updated = orderRepository.findById(orderId).orElseThrow();
        updated.setSellerRequestedStatus(null); updated.setSellerStatusRequestedAt(null);
        orderRepository.save(updated);
        return response;
    }

    /** Customer cancellation is allowed only until the parcel is packed. */
    @Transactional
    public OrderResponse cancelOrder(
            Long orderId,
            String customerEmail,
            String reason,
            String comment) {

        String normalizedReason = reason == null ? "" : reason.trim().toUpperCase();
        if (!CUSTOMER_CANCELLATION_REASONS.contains(normalizedReason)) {
            throw new RuntimeException("Please select a valid cancellation reason");
        }
        if ("OTHER".equals(normalizedReason) && (comment == null || comment.isBlank())) {
            throw new RuntimeException("Please provide a reason for selecting Other");
        }

        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (!customer.getId().equals(order.getCustomerId())) {
            throw new RuntimeException("You are not allowed to cancel this order");
        }
        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("This order is already cancelled");
        }
        if (order.getOrderStatus() == OrderStatus.PACKED
                || order.getOrderStatus() == OrderStatus.SHIPPED
                || order.getOrderStatus() == OrderStatus.OUT_FOR_DELIVERY
                || order.getOrderStatus() == OrderStatus.DELIVERED) {
            throw new RuntimeException("This order has already been packed and cannot be cancelled");
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        releaseOrderReservation(order);
        order.setCancellationReason(normalizedReason);
        order.setCancellationComment(comment == null || comment.isBlank() ? null : comment.trim());
        order.setCancelledBy("CUSTOMER");
        order.setCancelledAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);
        loyaltyService.reverseForOnlineOrder(savedOrder, "Order cancelled");

        try {
            emailService.sendOrderStatusEmail(customer.getEmail(), customer.getName(),
                    savedOrder.getOrderNumber(), "CANCELLED");
        } catch (Exception exception) {
            System.err.println("Cancellation email failed for order " + savedOrder.getOrderNumber());
        }
        return convertToOrderResponse(savedOrder);
    }

    /** Admin reconciliation action for COD, gateway settlements and refunds. */
    @Transactional
    public OrderResponse updatePaymentStatus(Long orderId, PaymentStatus paymentStatus) {
        if (paymentStatus == null) throw new RuntimeException("Payment status is required");
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        PaymentStatus previousPaymentStatus = order.getPaymentStatus();
        order.setPaymentStatus(paymentStatus);
        Order saved = orderRepository.save(order);
        if (paymentStatus == PaymentStatus.PAID) loyaltyService.awardForOnlineOrder(saved);
        if (paymentStatus == PaymentStatus.REFUNDED || paymentStatus == PaymentStatus.PARTIALLY_REFUNDED) loyaltyService.reverseForOnlineOrder(saved, "Payment refunded");
        if (previousPaymentStatus != paymentStatus
                && (paymentStatus == PaymentStatus.PAID || paymentStatus == PaymentStatus.PARTIALLY_PAID
                        || paymentStatus == PaymentStatus.REFUNDED || paymentStatus == PaymentStatus.PARTIALLY_REFUNDED)) {
            try {
                if (saved.getCustomerId() != null) userRepository.findById(saved.getCustomerId()).ifPresent(customer ->
                        emailService.sendOnlineOrderPaymentEmail(customer.getEmail(), customer.getName(),
                                saved.getOrderNumber(), paymentStatus.name(), saved.getGrandTotal()));
            } catch (Exception exception) {
                System.err.println("Payment update email failed for order " + saved.getOrderNumber());
            }
        }
        return convertToOrderResponse(saved);
    }

    /*
     * =========================================================
     * GET ORDER COUNT
     * =========================================================
     */

    @Transactional(readOnly = true)
    public long getOrderCount() {

        return orderRepository.count();
    }


    /*
     * =========================================================
     * GET ORDER COUNT BY STATUS
     * =========================================================
     */

    @Transactional(readOnly = true)
    public long getOrderCountByStatus(
            OrderStatus status) {

        return orderRepository.countByOrderStatus(
                status
        );
    }


    /*
     * =========================================================
     * GENERATE ORDER NUMBER
     * =========================================================
     */

    private String generateOrderNumber() {

        String randomPart =
                UUID.randomUUID()
                        .toString()
                        .substring(
                                0,
                                6
                        )
                        .toUpperCase();


        return "SH-"
                + System.currentTimeMillis()
                + "-"
                + randomPart;
    }


    /*
     * =========================================================
     * CONVERT ORDER -> RESPONSE
     * =========================================================
     */

    private OrderResponse convertToOrderResponse(
            Order order) {


        /*
         * =====================================================
         * CONVERT ORDER ITEMS
         * =====================================================
         */

        List<OrderItemResponse> itemResponses =
                new ArrayList<>();


        if (order.getItems() != null) {

            itemResponses =
                    order.getItems()
                            .stream()
                            .map(
                                    this::convertToOrderItemResponse
                            )
                            .toList();
        }


        /*
         * =====================================================
         * GET CUSTOMER DETAILS
         * =====================================================
         */

        Long customerId =
                order.getCustomerId();


        String customerName =
                "Customer";


        String customerEmail =
                "-";


        /*
         * Find customer.
         */

        if (customerId != null) {

            User customer =
                    userRepository.findById(
                            customerId
                    )
                    .orElse(null);


            if (customer != null) {

                customerName =
                        customer.getName();

                customerEmail =
                        customer.getEmail();
            }
        }


        /*
         * =====================================================
         * CREATE RESPONSE
         * =====================================================
         */

        OrderResponse response = new OrderResponse();
        response.setOrderId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setCustomerId(order.getCustomerId());
        response.setCustomerName(customerName);
        response.setCustomerEmail(customerEmail);
        response.setItems(itemResponses);
        response.setDeliveryExpectations(deliveryExpectations.forOrder(order.getId()));
        response.setSubtotal(order.getSubtotal());
        response.setDiscount(order.getDiscount());
        response.setTax(order.getTax());
        response.setDeliveryCharge(order.getDeliveryCharge());
        response.setLoyaltyPointsRedeemed(order.getLoyaltyPointsRedeemed());
        response.setLoyaltyDiscount(order.getLoyaltyDiscount());
        response.setGrandTotal(order.getGrandTotal());
        response.setOrderStatus(order.getOrderStatus());
        response.setSellerRequestedStatus(order.getSellerRequestedStatus());
        response.setSellerStatusRequestedAt(order.getSellerStatusRequestedAt());
        response.setDeliveryPersonName(order.getDeliveryPersonName());
        response.setDeliveryPersonMobile(order.getDeliveryPersonMobile());
        response.setPaymentStatus(order.getPaymentStatus());
        response.setPaymentMethod(order.getPaymentMethod());
        response.setShippingAddress(order.getShippingAddress());
        response.setDeliveryName(order.getDeliveryName());
        response.setDeliveryMobile(order.getDeliveryMobile());
        response.setDeliveryAddressLine1(order.getDeliveryAddressLine1());
        response.setDeliveryAddressLine2(order.getDeliveryAddressLine2());
        response.setDeliveryLandmark(order.getDeliveryLandmark());
        response.setDeliveryCity(order.getDeliveryCity());
        response.setDeliveryDistrict(order.getDeliveryDistrict());
        response.setDeliveryState(order.getDeliveryState());
        response.setDeliveryPincode(order.getDeliveryPincode());
        response.setCancellationReason(order.getCancellationReason());
        response.setCancellationComment(order.getCancellationComment());
        response.setCancelledBy(order.getCancelledBy());
        response.setCancelledAt(order.getCancelledAt());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());
        return response;
    }


    /*
     * =========================================================
     * CONVERT ORDER ITEM -> RESPONSE
     * =========================================================
     */

    private OrderItemResponse convertToOrderItemResponse(
            OrderItem item) {

        OrderItemResponse response = new OrderItemResponse(

                item.getId(),

                item.getProductId(),

                item.getProductName(),

                item.getUnitPrice(),

                item.getQuantity(),

                item.getTotalPrice(),

                item.getAssignedSerialSummary()
        );
        response.setVariantId(item.getProductVariantId()); response.setSelectedAttributes(item.getSelectedAttributes());
        response.setDeliveryEstimateText(item.getDeliveryEstimateText()); response.setDeliveryDistanceKm(item.getDeliveryDistanceKm());
        response.setSellerId(item.getSellerId()); response.setSellerName(item.getSellerName());
        productRepository.findById(item.getProductId()).ifPresent(product -> {
            String image = product.getImageUrl();
            if ((image == null || image.isBlank()) && product.getImages() != null) {
                image = product.getImages().stream().map(ProductImage::getImageUrl).filter(value -> value != null && !value.isBlank()).findFirst().orElse(null);
            }
            response.setImageUrl(image);
        });
        return response;
    }

    private void reserveOrderSerials(
            Order order,
            List<OrderItemSerialSelectionRequest> selections) {

        for (OrderItemSerialSelectionRequest selection : selections) {
            if (selection.getOrderItemId() == null
                    || selection.getSerialIds() == null
                    || selection.getSerialIds().isEmpty()) {
                throw new RuntimeException("Order item and IMEI/serial selections are required");
            }

            OrderItem item = findOrderItem(order, selection.getOrderItemId());
            if (selection.getSerialIds().size() != item.getQuantity()) {
                throw new RuntimeException(
                        "Select exactly " + item.getQuantity() + " IMEI/serial item(s) for " + item.getProductName()
                );
            }
            if (item.getSellerId() == null) {
                throw new RuntimeException("Seller snapshot missing for order item " + item.getId());
            }

            List<PurchaseItemSerial> existingSerials =
                    serialRepository.findAssignedForOrderItem(item.getId());

            for (PurchaseItemSerial existingSerial : existingSerials) {
                if (existingSerial.isSold()) {
                    throw new RuntimeException("IMEI/serial selection cannot be changed after dispatch");
                }
                existingSerial.setReservedOrderItem(null);
                existingSerial.setStatus("AVAILABLE");
            }
            serialRepository.saveAll(existingSerials);

            List<PurchaseItemSerial> serials =
                    serialRepository.findAvailableForOnlineOrder(
                            item.getSellerId(),
                            item.getProductId(),
                            selection.getSerialIds()
                    );

            if (serials.size() != selection.getSerialIds().size()) {
                throw new RuntimeException(
                        "One or more selected IMEI/serial numbers are not available for " + item.getProductName()
                );
            }
            if (item.getProductVariantId() != null && serials.stream().anyMatch(serial -> serial.getPurchaseItem() == null
                    || !item.getProductVariantId().equals(serial.getPurchaseItem().getProductVariantId()))) {
                throw new RuntimeException("Selected IMEI/serial belongs to a different product variant");
            }

            item.setAssignedSerialSummary(
                    String.join("\n", serials.stream().map(this::serialDisplay).toList())
            );

            for (PurchaseItemSerial serial : serials) {
                serial.setReservedOrderItem(item);
                serial.setStatus("RESERVED");
            }

            orderItemRepository.save(item);
            serialRepository.saveAll(serials);
        }
    }

    private OrderItem findOrderItem(Order order, Long orderItemId) {
        if (order.getItems() == null) {
            throw new RuntimeException("Order has no items");
        }

        return order.getItems()
                .stream()
                .filter(item -> orderItemId.equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Order item does not belong to this order"));
    }

    private void consumeReservedStock(Order order) {
        if (order.getStockDeductedAt() != null) {
            return;
        }

        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + item.getProductId()));
            boolean mobileVariant = item.getProductVariantId() != null && ProductConfigurationService.isMobile(product);
            if (item.getProductVariantId() != null) {
                variantInventory.sell(product, item.getProductVariantId(), item.getQuantity(), true, "ONLINE_VARIANT_ORDER_ITEM", item.getId());
                if (!mobileVariant) continue;
            }
            List<PurchaseItemSerial> serials =
                    serialRepository.findAssignedForOrderItem(item.getId());

            if (serials.size() != item.getQuantity()) {
                throw new RuntimeException(
                        "Select IMEI/serial numbers before dispatching " + item.getProductName()
                );
            }

            if (!mobileVariant) {
                int stockBefore = product.getStock() == null ? 0 : product.getStock();
                int stockAfter = stockBefore - item.getQuantity();
                if (stockAfter < 0) throw new RuntimeException("Insufficient physical stock for " + item.getProductName());
                product.setStock(stockAfter);
                product.setReservedStock(Math.max(0, (product.getReservedStock() == null ? 0 : product.getReservedStock()) - item.getQuantity()));
                productRepository.save(product);
                sendLowStockAlertSafely(product, stockBefore, stockAfter);
            }

            for (PurchaseItemSerial serial : serials) {
                serial.setStatus("SOLD");
                serial.setSoldOrderItem(item);
                serial.setSoldAt(LocalDateTime.now());
            }
            serialRepository.saveAll(serials);
        }

        order.setStockDeductedAt(LocalDateTime.now());
    }

    private void releaseOrderReservation(Order order) {
        if (order.getStockDeductedAt() != null) {
            return;
        }

        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId()).orElse(null);
            if (item.getProductVariantId() != null && product != null) {
                variantInventory.release(product, item.getProductVariantId(), item.getQuantity());
                continue;
            }
            if (product != null) {
                product.setReservedStock(
                        Math.max(0, (product.getReservedStock() == null ? 0 : product.getReservedStock()) - item.getQuantity())
                );
                productRepository.save(product);
            }
            item.setAssignedSerialSummary(null);
            orderItemRepository.save(item);
        }

        List<PurchaseItemSerial> serials =
                serialRepository.findReservedForOrder(order.getId());

        for (PurchaseItemSerial serial : serials) {
            serial.setReservedOrderItem(null);
            serial.setStatus("AVAILABLE");
        }
        serialRepository.saveAll(serials);
    }

    private String serialDisplay(PurchaseItemSerial serial) {
        if (serial.getImei1() != null && !serial.getImei1().isBlank()) {
            return serial.getImei1();
        }
        if (serial.getSerialNumber() != null && !serial.getSerialNumber().isBlank()) {
            return serial.getSerialNumber();
        }
        if (serial.getImei2() != null && !serial.getImei2().isBlank()) {
            return serial.getImei2();
        }
        return String.valueOf(serial.getId());
    }

}
