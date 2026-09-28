package com.shivhub.backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.shivhub.backend.dto.UserResponse;
import com.shivhub.backend.entity.OfflineBill;
import com.shivhub.backend.entity.Order;
import com.shivhub.backend.entity.Product;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.enums.UserStatus;
import com.shivhub.backend.enums.OrderStatus;
import com.shivhub.backend.enums.PaymentStatus;
import com.shivhub.backend.enums.ProductStatus;
import com.shivhub.backend.repository.OrderRepository;
import com.shivhub.backend.repository.OfflineBillRepository;
import com.shivhub.backend.repository.ProductRepository;
import com.shivhub.backend.repository.CouponRepository;
import com.shivhub.backend.repository.UserRepository;


/*
 * =========================================================
 * AdminService
 * =========================================================
 *
 * Admin-related business logic.
 *
 * SELLER:
 *
 * 1. Get pending sellers
 * 2. Approve seller
 * 3. Reject seller
 *
 *
 * CUSTOMER:
 *
 * 4. Get all customers
 * 5. Get customer by ID
 * 6. Block customer
 * 7. Unblock customer
 *
 * =========================================================
 */

@Service
public class AdminService {


    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OfflineBillRepository offlineBillRepository;
    private final CouponRepository couponRepository;
    private final EmailService emailService;
    private final SellerEntitlementService sellerEntitlementService;


    /*
     * =========================================================
     * CONSTRUCTOR
     * =========================================================
     */

    public AdminService(
            UserRepository userRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            OfflineBillRepository offlineBillRepository,
            CouponRepository couponRepository,
            EmailService emailService,
            SellerEntitlementService sellerEntitlementService) {

        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.offlineBillRepository = offlineBillRepository;
        this.couponRepository = couponRepository;
        this.emailService = emailService;
        this.sellerEntitlementService = sellerEntitlementService;
    }

    /** Summary used by the admin dashboard. */
    public Map<String, Object> getDashboardSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalCustomers", userRepository.countByRole(Role.CUSTOMER));
        summary.put("totalSellers", userRepository.findByRole(Role.SELLER).size());
        summary.put("pendingSellers", userRepository.findByRoleAndStatus(Role.SELLER, UserStatus.PENDING).size());
        summary.put("totalProducts", productRepository.count());
        summary.put("pendingProducts", productRepository.findByApprovalStatus(ProductStatus.PENDING).size());
        List<Product> liveProducts = productRepository.findByActiveTrueAndApprovalStatus(ProductStatus.APPROVED);
        summary.put("activeProducts", liveProducts.size());
        summary.put("outOfStockProducts", liveProducts.stream()
                .filter(product -> stock(product) <= 0)
                .count());
        summary.put("lowStockProducts", liveProducts.stream()
                .filter(product -> stock(product) > 0 && stock(product) <= 5)
                .count());
        LocalDateTime now = LocalDateTime.now();
        summary.put("activeCoupons", couponRepository.findByActiveTrueOrderByCreatedAtDesc().stream()
                .filter(coupon -> coupon.getStartsAt() == null || !coupon.getStartsAt().isAfter(now))
                .filter(coupon -> coupon.getEndsAt() == null || !coupon.getEndsAt().isBefore(now))
                .count());
        summary.put("totalOrders", orderRepository.count());
        summary.put("totalPayments", orderRepository.countByPaymentStatus(PaymentStatus.PAID));
        summary.put("activeOrders", orderRepository.countByOrderStatusNot(OrderStatus.CANCELLED));
        summary.put("totalRevenue", orderRepository.sumRevenueExcludingCancelled());
        return summary;
    }

    private int stock(Product product) {
        return product.getAvailableStock();
    }


    /*
     * =========================================================
     * SELLER MANAGEMENT
     * =========================================================
     */


    /*
     * =========================================================
     * GET PENDING SELLERS
     * =========================================================
     */

    public List<UserResponse> getPendingSellers() {

        List<User> sellers =
                userRepository.findByRoleAndStatus(
                        Role.SELLER,
                        UserStatus.PENDING
                );


        return sellers.stream()
                .map(this::convertToUserResponse)
                .toList();
    }

    public List<UserResponse> getSellers() {
        return userRepository.findByRole(Role.SELLER).stream()
                .map(this::convertToUserResponse)
                .toList();
    }

    public UserResponse getSellerById(Long sellerId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));
        if (seller.getRole() != Role.SELLER) {
            throw new RuntimeException("Selected user is not a seller");
        }
        return convertToUserResponse(seller);
    }


    /*
     * =========================================================
     * APPROVE SELLER
     * =========================================================
     *
     * PENDING
     *      ↓
     * APPROVED
     *
     * enabled = true
     *
     * =========================================================
     */

    public UserResponse approveSeller(
            Long sellerId) {


        User seller =
                userRepository.findById(sellerId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Seller not found"
                                )
                        );


        /*
         * Verify selected user is a seller.
         */

        if (seller.getRole() != Role.SELLER) {

            throw new RuntimeException(
                    "Selected user is not a seller"
            );
        }


        /*
         * Only PENDING sellers can be approved.
         */

        if (seller.getStatus() != UserStatus.PENDING) {

            throw new RuntimeException(
                    "Seller is not pending approval"
            );
        }


        /*
         * Approve seller.
         */

        seller.setStatus(
                UserStatus.APPROVED
        );


        /*
         * Enable seller account.
         */

        seller.setEnabled(true);
        seller.setRejectionReason(null);


        /*
         * Save changes.
         */

        User savedSeller =
                userRepository.save(seller);

        // The only place a free trial is started. Re-approval/account toggles
        // cannot create a replacement subscription or reset these dates.
        sellerEntitlementService.ensureFirstApprovalTrial(savedSeller);

        emailService.sendSellerApprovedEmail(savedSeller.getEmail(), savedSeller.getName());


        /*
         * Return safe DTO.
         */

        return convertToUserResponse(
                savedSeller
        );
    }


    /*
     * =========================================================
     * REJECT SELLER
     * =========================================================
     *
     * PENDING
     *      ↓
     * REJECTED
     *
     * enabled = false
     *
     * =========================================================
     */

    public UserResponse rejectSeller(Long sellerId, String rejectionReason) {

        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }


        User seller =
                userRepository.findById(sellerId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Seller not found"
                                )
                        );


        /*
         * Verify selected user is a seller.
         */

        if (seller.getRole() != Role.SELLER) {

            throw new RuntimeException(
                    "Selected user is not a seller"
            );
        }


        /*
         * Only PENDING sellers can be rejected.
         */

        if (seller.getStatus() != UserStatus.PENDING) {

            throw new RuntimeException(
                    "Seller is not pending approval"
            );
        }


        /*
         * Reject seller.
         */

        seller.setStatus(
                UserStatus.REJECTED
        );


        /*
         * Keep account disabled.
         */

        seller.setEnabled(false);
        seller.setRejectionReason(rejectionReason.trim());


        /*
         * Save changes.
         */

        User savedSeller =
                userRepository.save(seller);

        emailService.sendSellerRejectedEmail(savedSeller.getEmail(), savedSeller.getName(), savedSeller.getRejectionReason());


        /*
         * Return safe DTO.
         */

        return convertToUserResponse(
                savedSeller
        );
    }


    /*
     * =========================================================
     * CUSTOMER MANAGEMENT
     * =========================================================
     */


    /*
     * =========================================================
     * GET ALL CUSTOMERS
     * =========================================================
     *
     * GET:
     *
     * /api/admin/customers
     *
     * Returns only users whose role is CUSTOMER.
     *
     * =========================================================
     */

    public List<UserResponse> getCustomers() {

        List<User> customers =
                userRepository.findByRole(
                        Role.CUSTOMER
                );


        return customers.stream()
                .map(this::convertToUserResponse)
                .toList();
    }


    /*
     * =========================================================
     * GET CUSTOMER BY ID
     * =========================================================
     *
     * GET:
     *
     * /api/admin/customers/{id}
     *
     * =========================================================
     */

    public UserResponse getCustomerById(
            Long customerId) {


        User customer =
                userRepository.findById(customerId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Customer not found"
                                )
                        );


        /*
         * Make sure selected user
         * is actually a customer.
         */

        if (customer.getRole() != Role.CUSTOMER) {

            throw new RuntimeException(
                    "Selected user is not a customer"
            );
        }


        return convertToUserResponse(
                customer
        );
    }

    public Map<String, Object> getCustomerDetails(
            Long customerId) {

        User customer =
                userRepository.findById(customerId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Customer not found"
                                )
                        );

        if (customer.getRole() != Role.CUSTOMER) {
            throw new RuntimeException(
                    "Selected user is not a customer"
            );
        }

        List<Order> onlineOrders =
                orderRepository.findByCustomerIdOrderByCreatedAtDesc(
                        customer.getId()
                );

        List<OfflineBill> offlineBills =
                loadCustomerOfflineBills(customer);

        BigDecimal onlineTotal =
                onlineOrders.stream()
                        .filter(order -> order.getOrderStatus() != OrderStatus.CANCELLED)
                        .map(order -> safe(order.getGrandTotal()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal offlineTotal =
                offlineBills.stream()
                        .map(bill -> safe(bill.getGrandTotal()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDateTime lastOnline =
                onlineOrders.stream()
                        .map(Order::getCreatedAt)
                        .filter(Objects::nonNull)
                        .max(Comparator.naturalOrder())
                        .orElse(null);

        LocalDateTime lastOffline =
                offlineBills.stream()
                        .map(OfflineBill::getCreatedAt)
                        .filter(Objects::nonNull)
                        .max(Comparator.naturalOrder())
                        .orElse(null);

        LocalDateTime lastPurchase =
                latest(lastOnline, lastOffline);

        List<Map<String, Object>> purchases =
                new ArrayList<>();

        onlineOrders.forEach(order -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", order.getId());
            row.put("type", "ONLINE");
            row.put("reference", order.getOrderNumber());
            row.put("date", order.getCreatedAt());
            row.put("status", order.getOrderStatus());
            row.put("paymentStatus", order.getPaymentStatus());
            row.put("amount", safe(order.getGrandTotal()));
            row.put("tax", safe(order.getTax()));
            row.put("discount", safe(order.getDiscount()).add(safe(order.getCouponDiscount())));
            purchases.add(row);
        });

        offlineBills.forEach(bill -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", bill.getId());
            row.put("type", bill.getCustomerId() == null ? "WALK_IN" : "OFFLINE");
            row.put("reference", bill.getBillNumber());
            row.put("date", bill.getCreatedAt());
            row.put("status", "BILLED");
            row.put("paymentStatus", paymentStatusForBill(bill));
            row.put("amount", safe(bill.getGrandTotal()));
            row.put("tax", safe(bill.getCgst()).add(safe(bill.getSgst())).add(safe(bill.getIgst())));
            row.put("discount", safe(bill.getDiscount()));
            row.put("sellerName", bill.getSellerName());
            purchases.add(row);
        });

        purchases.sort((left, right) -> {
            LocalDateTime leftDate = (LocalDateTime) left.get("date");
            LocalDateTime rightDate = (LocalDateTime) right.get("date");
            if (leftDate == null && rightDate == null) {
                return 0;
            }
            if (leftDate == null) {
                return 1;
            }
            if (rightDate == null) {
                return -1;
            }
            return rightDate.compareTo(leftDate);
        });

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("customer", customerProfile(customer));
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("onlineOrders", onlineOrders.size());
        summary.put("offlineBills", offlineBills.size());
        summary.put("totalOrdersAndBills", onlineOrders.size() + offlineBills.size());
        summary.put("onlinePurchaseAmount", onlineTotal);
        summary.put("offlinePurchaseAmount", offlineTotal);
        summary.put("totalPurchaseAmount", onlineTotal.add(offlineTotal));
        summary.put("lastPurchaseAt", lastPurchase);

        response.put("summary", summary);
        response.put("purchases", purchases);
        return response;
    }


    /*
     * =========================================================
     * BLOCK CUSTOMER
     * =========================================================
     *
     * enabled:
     *
     * true
     *   ↓
     * false
     *
     * Customer will no longer be allowed
     * to use the account.
     *
     * =========================================================
     */

    public UserResponse blockCustomer(
            Long customerId) {


        User customer =
                userRepository.findById(customerId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Customer not found"
                                )
                        );


        /*
         * Verify selected user is CUSTOMER.
         */

        if (customer.getRole() != Role.CUSTOMER) {

            throw new RuntimeException(
                    "Selected user is not a customer"
            );
        }


        /*
         * Disable customer account.
         */

        customer.setEnabled(false);


        /*
         * Keep customer status approved.
         *
         * enabled controls whether the
         * account can actually use/login.
         */

        customer.setStatus(
                UserStatus.APPROVED
        );


        /*
         * Save customer.
         */

        User savedCustomer =
                userRepository.save(customer);


        return convertToUserResponse(
                savedCustomer
        );
    }


    /*
     * =========================================================
     * UNBLOCK CUSTOMER
     * =========================================================
     *
     * enabled:
     *
     * false
     *   ↓
     * true
     *
     * =========================================================
     */

    public UserResponse unblockCustomer(
            Long customerId) {


        User customer =
                userRepository.findById(customerId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Customer not found"
                                )
                        );


        /*
         * Verify selected user is CUSTOMER.
         */

        if (customer.getRole() != Role.CUSTOMER) {

            throw new RuntimeException(
                    "Selected user is not a customer"
            );
        }


        /*
         * Enable customer account.
         */

        customer.setEnabled(true);


        /*
         * Customer remains approved.
         */

        customer.setStatus(
                UserStatus.APPROVED
        );


        /*
         * Save customer.
         */

        User savedCustomer =
                userRepository.save(customer);


        return convertToUserResponse(
                savedCustomer
        );
    }


    /*
     * =========================================================
     * USER → USER RESPONSE
     * =========================================================
     *
     * Password is NEVER returned.
     *
     * =========================================================
     */

    private UserResponse convertToUserResponse(
            User user) {


        UserResponse response = new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getMobile(),
                user.getRole(),
                user.getStatus()
        );
        response.setEnabled(user.isEnabled());
        response.setRejectionReason(user.getRejectionReason());
        return response;
    }

    private Map<String, Object> customerProfile(User customer) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", customer.getId());
        profile.put("name", customer.getName());
        profile.put("email", customer.getEmail());
        profile.put("mobile", customer.getMobile());
        profile.put("profilePhotoUrl", customer.getProfilePhotoUrl());
        profile.put("role", customer.getRole());
        profile.put("status", customer.getStatus());
        profile.put("enabled", customer.isEnabled());
        profile.put("createdAt", customer.getCreatedAt());
        profile.put("updatedAt", customer.getUpdatedAt());
        profile.put("lastLoginAt", customer.getLastLoginAt());
        return profile;
    }

    private List<OfflineBill> loadCustomerOfflineBills(User customer) {
        Map<Long, OfflineBill> uniqueBills = new LinkedHashMap<>();

        offlineBillRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId())
                .forEach(bill -> uniqueBills.put(bill.getId(), bill));

        if (customer.getMobile() != null && !customer.getMobile().isBlank()) {
            offlineBillRepository.findByCustomerMobileOrderByCreatedAtDesc(customer.getMobile())
                    .forEach(bill -> uniqueBills.put(bill.getId(), bill));
        }

        if (customer.getEmail() != null && !customer.getEmail().isBlank()) {
            offlineBillRepository.findByCustomerEmailOrderByCreatedAtDesc(customer.getEmail())
                    .forEach(bill -> uniqueBills.put(bill.getId(), bill));
        }

        return uniqueBills.values().stream()
                .sorted(Comparator.comparing(
                        OfflineBill::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();
    }

    private String paymentStatusForBill(OfflineBill bill) {
        BigDecimal grandTotal = safe(bill.getGrandTotal());
        BigDecimal paid = safe(bill.getPaymentAmount());
        if (grandTotal.signum() <= 0 || paid.compareTo(grandTotal) >= 0) {
            return "PAID";
        }
        if (paid.signum() > 0) {
            return "PARTIAL";
        }
        return "PENDING";
    }

    private LocalDateTime latest(LocalDateTime first, LocalDateTime second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.isAfter(second) ? first : second;
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

}
