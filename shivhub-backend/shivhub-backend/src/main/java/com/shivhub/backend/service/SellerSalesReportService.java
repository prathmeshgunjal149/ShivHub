package com.shivhub.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shivhub.backend.dto.SellerSalesReportResponse;
import com.shivhub.backend.dto.SellerSalesReportRow;
import com.shivhub.backend.dto.SellerSalesReportSummary;
import com.shivhub.backend.entity.OfflineBill;
import com.shivhub.backend.entity.OfflineBillItem;
import com.shivhub.backend.entity.OrderItem;
import com.shivhub.backend.entity.Product;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.repository.OfflineBillRepository;
import com.shivhub.backend.repository.OrderItemRepository;
import com.shivhub.backend.repository.ProductRepository;
import com.shivhub.backend.repository.UserRepository;

/**
 * Seller-scoped sales reporting. It composes existing POS bills and seller
 * order-items without altering either financial source of truth.
 */
@Service
public class SellerSalesReportService {
    private final UserRepository userRepository;
    private final OfflineBillRepository offlineBillRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final GstCalculator gstCalculator;

    public SellerSalesReportService(UserRepository userRepository,
            OfflineBillRepository offlineBillRepository,
            OrderItemRepository orderItemRepository,
            ProductRepository productRepository,
            GstCalculator gstCalculator) {
        this.userRepository = userRepository;
        this.offlineBillRepository = offlineBillRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.gstCalculator = gstCalculator;
    }

    @Transactional(readOnly = true)
    public SellerSalesReportResponse getReport(String sellerEmail, LocalDate startDate,
            LocalDate endDate, String search, Long productId, String category,
            String paymentMethod, int page, int size) {
        LocalDate start = startDate == null ? LocalDate.now().withDayOfMonth(1) : startDate;
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        if (end.isBefore(start)) throw new IllegalArgumentException("End date cannot be before start date.");
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        List<SellerSalesReportRow> matched = findRows(sellerEmail, start, end, search, productId, category, paymentMethod);
        SellerSalesReportSummary summary = summarize(matched);
        int from = Math.min(safePage * safeSize, matched.size());
        int to = Math.min(from + safeSize, matched.size());
        return new SellerSalesReportResponse(start, end, summary, matched.subList(from, to),
                matched.size(), safePage, safeSize, (int) Math.ceil((double) matched.size() / safeSize));
    }

    @Transactional(readOnly = true)
    public List<SellerSalesReportRow> getExportRows(String sellerEmail, LocalDate startDate,
            LocalDate endDate, String search, Long productId, String category, String paymentMethod) {
        LocalDate start = startDate == null ? LocalDate.now().withDayOfMonth(1) : startDate;
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        if (end.isBefore(start)) throw new IllegalArgumentException("End date cannot be before start date.");
        return findRows(sellerEmail, start, end, search, productId, category, paymentMethod);
    }

    private List<SellerSalesReportRow> findRows(String sellerEmail, LocalDate start, LocalDate end,
            String search, Long productId, String category, String paymentMethod) {
        User seller = getSeller(sellerEmail);
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime until = end.plusDays(1).atStartOfDay();
        Set<Long> permittedProductIds = productIdsForCategory(seller, category);
        if (category != null && !category.isBlank() && permittedProductIds.isEmpty()) return List.of();
        String query = normalized(search);
        String method = normalized(paymentMethod);
        List<SellerSalesReportRow> rows = new ArrayList<>();

        // Online totals are line-item totals because one customer order can belong to multiple sellers.
        if (method.isEmpty()) {
            for (OrderItem item : orderItemRepository.findSellerReportItemsBetween(seller.getId(), from, until)) {
                if (!matchesProduct(item.getProductId(), productId, permittedProductIds)) continue;
                GstBreakup onlineGst = onlineGstBreakup(item);
                SellerSalesReportRow row = new SellerSalesReportRow("ONLINE", item.getId(),
                        item.getOrder().getOrderNumber(), item.getOrder().getCreatedAt(), "Online customer",
                        null, item.getProductName(), item.getQuantity(), BigDecimal.ZERO,
                        onlineGst.taxableAmount(), onlineGst.cgst(), onlineGst.sgst(), onlineGst.igst(),
                        onlineGst.totalGst(), amount(item.getTotalPrice()));
                if (matchesSearch(row, query)) rows.add(row);
            }
        }

        for (OfflineBill bill : offlineBillRepository.findBySellerIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                seller.getId(), from, until)) {
            if (!method.isEmpty() && (bill.getPaymentMethod() == null || !method.equals(bill.getPaymentMethod().name()))) continue;
            if (!matchesOfflineBillProducts(bill, productId, permittedProductIds)) continue;
            SellerSalesReportRow row = new SellerSalesReportRow("POS", bill.getId(), bill.getBillNumber(),
                    bill.getCreatedAt(), bill.getCustomerName(), bill.getPaymentMethod() == null ? null : bill.getPaymentMethod().name(),
                    productLabel(bill), quantity(bill), amount(bill.getDiscount()),
                    amount(bill.getTaxableAmount()), amount(bill.getCgst()), amount(bill.getSgst()), amount(bill.getIgst()),
                    amount(bill.getCgst()).add(amount(bill.getSgst())).add(amount(bill.getIgst())), amount(bill.getGrandTotal()));
            if (matchesSearch(row, query)) rows.add(row);
        }
        rows.sort(Comparator.comparing(SellerSalesReportRow::saleDate, Comparator.nullsLast(Comparator.reverseOrder())));
        return rows;
    }

    private Set<Long> productIdsForCategory(User seller, String category) {
        if (category == null || category.isBlank()) return Set.of();
        String expected = category.trim().toLowerCase(Locale.ROOT);
        Set<Long> ids = new HashSet<>();
        for (Product product : productRepository.findBySeller(seller)) {
            String name = product.getCategoryEntity() == null ? product.getCategory() : product.getCategoryEntity().getName();
            if (name != null && expected.equals(name.trim().toLowerCase(Locale.ROOT))) ids.add(product.getId());
        }
        return ids;
    }

    private boolean matchesProduct(Long rowProductId, Long requestedProductId, Set<Long> categoryProductIds) {
        return (requestedProductId == null || requestedProductId.equals(rowProductId))
                && (categoryProductIds.isEmpty() || categoryProductIds.contains(rowProductId));
    }

    private boolean matchesOfflineBillProducts(OfflineBill bill, Long productId, Set<Long> categoryProductIds) {
        if (productId == null && categoryProductIds.isEmpty()) return true;
        return bill.getItems().stream().anyMatch(item -> matchesProduct(item.getProductId(), productId, categoryProductIds));
    }

    private String productLabel(OfflineBill bill) {
        return bill.getItems().stream().map(OfflineBillItem::getProductName).filter(name -> name != null && !name.isBlank())
                .reduce((first, second) -> first + ", " + second).orElse("POS sale");
    }

    private int quantity(OfflineBill bill) { return bill.getItems().stream().map(OfflineBillItem::getQuantity).filter(q -> q != null).mapToInt(Integer::intValue).sum(); }
    private boolean matchesSearch(SellerSalesReportRow row, String query) {
        if (query.isEmpty()) return true;
        return contains(row.referenceNumber(), query) || contains(row.customerName(), query) || contains(row.productName(), query) || contains(row.paymentMethod(), query) || contains(row.source(), query);
    }
    private boolean contains(String value, String query) { return value != null && value.toLowerCase(Locale.ROOT).contains(query); }
    private String normalized(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }
    private BigDecimal amount(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private GstBreakup onlineGstBreakup(OrderItem item) {
        BigDecimal total = amount(item.getTotalPrice());
        BigDecimal gstRate = productRepository.findById(item.getProductId())
                .map(Product::getGstRate)
                .orElse(BigDecimal.ZERO);
        return gstCalculator.inclusive(total, gstRate);
    }
    private User getSeller(String email) {
        User seller = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Seller not found"));
        if (!seller.isEnabled() || seller.getRole() == null || !"SELLER".equals(seller.getRole().name())) throw new RuntimeException("Only active sellers can access reports");
        return seller;
    }
    private SellerSalesReportSummary summarize(List<SellerSalesReportRow> rows) {
        BigDecimal sales = BigDecimal.ZERO, gst = BigDecimal.ZERO, discount = BigDecimal.ZERO;
        long items = 0;
        Set<String> bills = new HashSet<>();
        for (SellerSalesReportRow row : rows) {
            sales = sales.add(amount(row.total())); gst = gst.add(amount(row.gst())); discount = discount.add(amount(row.discount()));
            items += row.quantity() == null ? 0 : row.quantity(); bills.add(row.source() + ":" + row.referenceNumber());
        }
        return new SellerSalesReportSummary(sales, bills.size(), items, gst, discount);
    }
}
