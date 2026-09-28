package com.shivhub.backend.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import com.shivhub.backend.entity.CustomerReceivable;
import com.shivhub.backend.entity.OfflineBill;
import com.shivhub.backend.enums.ProductStatus;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.enums.UserStatus;
import com.shivhub.backend.repository.CustomerReceivablePaymentRepository;
import com.shivhub.backend.repository.CustomerReceivableRepository;
import com.shivhub.backend.repository.MarketingCampaignRepository;
import com.shivhub.backend.repository.OfflineBillRepository;
import com.shivhub.backend.repository.ProductRepository;
import com.shivhub.backend.repository.PurchaseRepository;
import com.shivhub.backend.repository.SellerExpenseRepository;
import com.shivhub.backend.repository.UserRepository;
import com.shivhub.backend.repository.CouponUsageRepository;
import com.shivhub.backend.repository.OrderRepository;
import com.shivhub.backend.repository.OrderItemRepository;
import com.shivhub.backend.entity.Order;
import com.shivhub.backend.entity.OrderItem;
import com.shivhub.backend.entity.Purchase;
import com.shivhub.backend.entity.SellerExpense;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.OrderStatus;

/** Live business metrics for the admin report centre. */
@Service
public class ReportService {
    private final UserRepository users;
    private final ProductRepository products;
    private final MarketingCampaignRepository campaigns;
    private final CouponUsageRepository couponUsages;
    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final OfflineBillRepository offlineBills;
    private final PurchaseRepository purchases;
    private final SellerExpenseRepository expenses;
    private final CustomerReceivableRepository receivables;
    private final CustomerReceivablePaymentRepository receivablePayments;

    public ReportService(UserRepository users, ProductRepository products, MarketingCampaignRepository campaigns,
            CouponUsageRepository couponUsages, OrderRepository orders, OrderItemRepository orderItems,
            OfflineBillRepository offlineBills, PurchaseRepository purchases, SellerExpenseRepository expenses,
            CustomerReceivableRepository receivables, CustomerReceivablePaymentRepository receivablePayments) {
        this.users = users; this.products = products; this.campaigns = campaigns; this.couponUsages = couponUsages; this.orders = orders; this.orderItems = orderItems;
        this.offlineBills = offlineBills; this.purchases = purchases; this.expenses = expenses; this.receivables = receivables; this.receivablePayments = receivablePayments;
    }

    public Map<String, Object> summary() {
        Map<String, Object> report = new LinkedHashMap<>();
        long customers = users.findByRole(Role.CUSTOMER).size();
        long activeCustomers = users.findByRoleAndEnabled(Role.CUSTOMER, true).size();
        long sellers = users.findByRole(Role.SELLER).size();
        long approvedSellers = users.findByRoleAndStatus(Role.SELLER, UserStatus.APPROVED).size();
        long pendingSellers = users.findByRoleAndStatus(Role.SELLER, UserStatus.PENDING).size();
        long totalProducts = products.count();
        long approvedProducts = products.findByApprovalStatus(ProductStatus.APPROVED).size();
        long pendingProducts = products.findByApprovalStatus(ProductStatus.PENDING).size();
        long activeCampaigns = campaigns.findAll().stream().filter(c -> c.isActive()).count();

        report.put("generatedAt", java.time.LocalDateTime.now());
        report.put("customers", Map.of("total", customers, "active", activeCustomers, "blocked", customers - activeCustomers));
        report.put("sellers", Map.of("total", sellers, "approved", approvedSellers, "pending", pendingSellers));
        report.put("products", Map.of("total", totalProducts, "approved", approvedProducts, "pending", pendingProducts));
        report.put("marketing", Map.of("total", campaigns.count(), "active", activeCampaigns));
        var usages = couponUsages.findAll();
        java.math.BigDecimal couponDiscount = usages.stream().map(u -> u.getDiscountAmount()).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        long couponsCreated = campaigns.findByTypeOrderByCreatedAtDesc("COUPON").size();
        report.put("coupons", Map.of("created", couponsCreated, "used", usages.size(), "discountGiven", couponDiscount));
        return report;
    }

    /** Accountant-oriented, immutable-order snapshot report. Cancelled orders are excluded from sales. */
    public Map<String, Object> accounting() {
        var validOrders = orders.findAll().stream().filter(order -> order.getOrderStatus() != OrderStatus.CANCELLED).toList();
        java.math.BigDecimal sales = validOrders.stream().map(Order::getGrandTotal).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        java.math.BigDecimal taxableValue = validOrders.stream().map(Order::getSubtotal).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        java.math.BigDecimal taxCollected = validOrders.stream().map(Order::getTax).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        java.math.BigDecimal couponDiscount = validOrders.stream().map(order -> order.getCouponDiscount() == null ? java.math.BigDecimal.ZERO : order.getCouponDiscount()).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        Map<String, java.math.BigDecimal> sellerSales = new LinkedHashMap<>();
        for (OrderItem item : orderItems.findAll()) {
            if (item.getOrder() == null || item.getOrder().getOrderStatus() == OrderStatus.CANCELLED) continue;
            String seller = item.getSellerName() == null ? "Unassigned seller" : item.getSellerName();
            sellerSales.merge(seller, item.getTotalPrice(), java.math.BigDecimal::add);
        }
        Map<Long, Long> customerCouponUsage = new LinkedHashMap<>();
        couponUsages.findAll().forEach(usage -> customerCouponUsage.merge(usage.getCustomerId(), 1L, Long::sum));
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("generatedAt", java.time.LocalDateTime.now());
        report.put("sales", Map.of("orders", validOrders.size(), "grossSales", sales, "taxableValue", taxableValue, "taxCollected", taxCollected, "couponDiscount", couponDiscount));
        report.put("sellerSales", sellerSales);
        report.put("customerCouponUsage", customerCouponUsage);
        report.put("sellerCompliance", users.findByRole(Role.SELLER).stream().map(seller -> Map.of("sellerId", seller.getId(), "sellerName", seller.getBusinessName() == null || seller.getBusinessName().isBlank() ? seller.getName() : seller.getBusinessName(), "gstinPresent", seller.getGstin() != null && !seller.getGstin().isBlank())).toList());
        return report;
    }

    public Map<String, Object> sellerPerformance(LocalDate start, LocalDate end) {
        LocalDate reportEnd = end == null ? LocalDate.now() : end;
        LocalDate reportStart = start == null ? reportEnd.withDayOfMonth(1) : start;
        LocalDateTime from = reportStart.atStartOfDay();
        LocalDateTime until = reportEnd.plusDays(1).atStartOfDay();

        var rows = users.findByRole(Role.SELLER).stream()
                .map(seller -> sellerPerformanceRow(seller, reportStart, reportEnd, from, until))
                .toList();

        BigDecimal totalSales = rows.stream().map(row -> (BigDecimal) row.get("totalSales")).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalGst = rows.stream().map(row -> (BigDecimal) row.get("gstCollected")).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpenses = rows.stream().map(row -> (BigDecimal) row.get("expenses")).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pendingReceivables = rows.stream().map(row -> (BigDecimal) row.get("pendingReceivables")).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("generatedAt", LocalDateTime.now());
        report.put("startDate", reportStart);
        report.put("endDate", reportEnd);
        report.put("summary", Map.of(
                "sellerCount", rows.size(),
                "totalSales", totalSales,
                "gstCollected", totalGst,
                "expenses", totalExpenses,
                "pendingReceivables", pendingReceivables
        ));
        report.put("sellers", rows);
        return report;
    }

    private Map<String, Object> sellerPerformanceRow(User seller, LocalDate reportStart, LocalDate reportEnd,
            LocalDateTime from, LocalDateTime until) {

        var onlineItems = orderItems.findSellerReportItemsBetween(seller.getId(), from, until);
        var sellerBills = offlineBills.findBySellerIdAndCreatedAtBetweenOrderByCreatedAtDesc(seller.getId(), from, until);
        var sellerPurchases = purchases.findBySellerOrderByPurchaseDateDesc(seller).stream()
                .filter(purchase -> purchase.getPurchaseDate() != null
                        && !purchase.getPurchaseDate().isBefore(from)
                        && purchase.getPurchaseDate().isBefore(until))
                .toList();

        BigDecimal onlineSales = onlineItems.stream().map(OrderItem::getTotalPrice).map(this::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal offlineSales = sellerBills.stream().map(OfflineBill::getGrandTotal).map(this::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal offlineGst = sellerBills.stream()
                .map(bill -> amount(bill.getCgst()).add(amount(bill.getSgst())).add(amount(bill.getIgst())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal onlineGst = onlineItems.stream()
                .map(item -> item.getOrder() == null ? BigDecimal.ZERO : amount(item.getOrder().getTax()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal purchaseAmount = sellerPurchases.stream().map(Purchase::getGrandTotal).map(this::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expenseAmount = expenses.findBySellerAndExpenseDateBetweenOrderByExpenseDateDesc(seller, reportStart, reportEnd)
                .stream().map(SellerExpense::getAmount).map(this::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal dueAmount = receivables.findBySellerOrderByDueDateAsc(seller).stream()
                .map(row -> amount(row.getSaleAmount()).subtract(amount(receivablePayments.totalPaid(row))).max(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long onlineCustomers = onlineItems.stream()
                .map(item -> item.getOrder() == null ? null : item.getOrder().getCustomerId())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();
        long offlineCustomers = sellerBills.stream()
                .filter(bill -> bill.getCustomerId() != null)
                .map(OfflineBill::getCustomerId)
                .distinct()
                .count();
        long walkInCustomers = sellerBills.stream()
                .filter(bill -> bill.getCustomerId() == null)
                .map(bill -> bill.getCustomerMobile() == null || bill.getCustomerMobile().isBlank() ? bill.getBillNumber() : bill.getCustomerMobile())
                .distinct()
                .count();

        long invoices = onlineItems.stream().map(item -> item.getOrder().getId()).distinct().count() + sellerBills.size();
        BigDecimal totalSales = onlineSales.add(offlineSales);
        BigDecimal averageOrderValue = invoices == 0 ? BigDecimal.ZERO : totalSales.divide(BigDecimal.valueOf(invoices), 2, java.math.RoundingMode.HALF_UP);

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("sellerId", seller.getId());
        row.put("sellerName", seller.getName());
        row.put("shopName", seller.getBusinessName());
        row.put("shopLogo", seller.getProfilePhotoUrl());
        row.put("email", seller.getEmail());
        row.put("mobile", seller.getMobile());
        row.put("gstin", seller.getGstin());
        row.put("totalCustomers", onlineCustomers + offlineCustomers + walkInCustomers);
        row.put("onlineCustomers", onlineCustomers);
        row.put("offlineCustomers", offlineCustomers);
        row.put("walkInCustomers", walkInCustomers);
        row.put("totalSales", totalSales);
        row.put("onlineSales", onlineSales);
        row.put("offlineSales", offlineSales);
        row.put("totalBillsOrders", invoices);
        row.put("averageOrderValue", averageOrderValue);
        row.put("gstCollected", onlineGst.add(offlineGst));
        row.put("pendingReceivables", dueAmount);
        row.put("purchaseAmount", purchaseAmount);
        row.put("expenses", expenseAmount);
        row.put("grossProfitEstimate", totalSales.subtract(purchaseAmount));
        row.put("netAfterExpensesEstimate", totalSales.subtract(purchaseAmount).subtract(expenseAmount));
        return row;
    }

    private BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
