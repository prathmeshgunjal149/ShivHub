package com.shivhub.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shivhub.backend.dto.SellerDashboardProductResponse;
import com.shivhub.backend.dto.SellerDashboardResponse;
import com.shivhub.backend.dto.SellerDashboardSummaryResponse;
import com.shivhub.backend.dto.SellerDashboardDueResponse;
import com.shivhub.backend.entity.CustomerReceivable;
import com.shivhub.backend.entity.Distributor;
import com.shivhub.backend.entity.Product;
import com.shivhub.backend.entity.Purchase;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.ProductStatus;
import com.shivhub.backend.repository.OfflineBillRepository;
import com.shivhub.backend.repository.OrderItemRepository;
import com.shivhub.backend.repository.ProductRepository;
import com.shivhub.backend.repository.CustomerReceivablePaymentRepository;
import com.shivhub.backend.repository.CustomerReceivableRepository;
import com.shivhub.backend.repository.DistributorCreditNoteRepository;
import com.shivhub.backend.repository.PurchasePaymentRepository;
import com.shivhub.backend.repository.PurchaseRepository;
import com.shivhub.backend.repository.UserRepository;

/**
 * Read-only composition layer for the Seller / Shop Owner dashboard.
 *
 * This class has no write operations. Every dashboard number is calculated from
 * the source-of-truth product, order-item and offline-bill tables.
 */
@Service
public class SellerDashboardService {

    private static final int LOW_STOCK_THRESHOLD = 5;

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final OfflineBillRepository offlineBillRepository;
    private final CustomerReceivableRepository receivableRepository;
    private final CustomerReceivablePaymentRepository receivablePaymentRepository;
    private final PurchaseRepository purchaseRepository;
    private final PurchasePaymentRepository purchasePaymentRepository;
    private final DistributorCreditNoteRepository creditNoteRepository;

    public SellerDashboardService(
            UserRepository userRepository,
            ProductRepository productRepository,
            OrderItemRepository orderItemRepository,
            OfflineBillRepository offlineBillRepository,
            CustomerReceivableRepository receivableRepository,
            CustomerReceivablePaymentRepository receivablePaymentRepository,
            PurchaseRepository purchaseRepository,
            PurchasePaymentRepository purchasePaymentRepository,
            DistributorCreditNoteRepository creditNoteRepository) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
        this.offlineBillRepository = offlineBillRepository;
        this.receivableRepository = receivableRepository;
        this.receivablePaymentRepository = receivablePaymentRepository;
        this.purchaseRepository = purchaseRepository;
        this.purchasePaymentRepository = purchasePaymentRepository;
        this.creditNoteRepository = creditNoteRepository;
    }

    /**
     * Creates a seller-scoped snapshot. The email originates from the verified
     * JWT principal; callers must never supply a seller ID themselves.
     */
    @Transactional(readOnly = true)
    public SellerDashboardResponse getDashboard(String sellerEmail) {
        User seller = getVerifiedSeller(sellerEmail);
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfTomorrow = startOfToday.plusDays(1);

        BigDecimal onlineSales = amountOrZero(orderItemRepository.sumSellerOnlineSalesBetween(
                seller.getId(), startOfToday, startOfTomorrow));
        BigDecimal offlineSales = amountOrZero(offlineBillRepository.sumSellerSalesBetween(
                seller.getId(), startOfToday, startOfTomorrow));

        SellerDashboardSummaryResponse summary = new SellerDashboardSummaryResponse(
                productRepository.countBySeller(seller),
                productRepository.countBySellerAndActiveTrue(seller),
                productRepository.countBySellerAndApprovalStatus(seller, ProductStatus.PENDING),
                productRepository.countBySellerAndStockLessThanEqual(seller, LOW_STOCK_THRESHOLD),
                orderItemRepository.countSellerOnlineOrdersBetween(seller.getId(), startOfToday, startOfTomorrow),
                offlineBillRepository.countBySellerIdAndCreatedAtBetween(seller.getId(), startOfToday, startOfTomorrow),
                onlineSales,
                offlineSales,
                onlineSales.add(offlineSales));

        return new SellerDashboardResponse(
                summary,
                productRepository.findTop6BySellerOrderByUpdatedAtDesc(seller).stream()
                        .map(this::toProductResponse).toList(),
                productRepository.findTop6BySellerAndStockLessThanEqualOrderByStockAscUpdatedAtDesc(
                        seller, LOW_STOCK_THRESHOLD).stream()
                        .map(this::toProductResponse).toList(),
                todayDues(seller, LocalDate.now()),
                LocalDateTime.now());
    }

    private SellerDashboardDueResponse todayDues(User seller, LocalDate today) {
        List<SellerDashboardDueResponse.DueItem> receivables = receivableRepository.findBySellerOrderByDueDateAsc(seller).stream()
                .filter(row -> row.getDueDate() != null && !row.getDueDate().isAfter(today))
                .map(row -> customerDue(row, today))
                .filter(row -> row.remainingAmount().signum() > 0)
                .toList();

        List<SellerDashboardDueResponse.DueItem> payables = purchaseRepository.findBySellerOrderByPurchaseDateDesc(seller).stream()
                .map(purchase -> distributorDue(purchase, today))
                .filter(java.util.Objects::nonNull)
                .filter(row -> row.remainingAmount().signum() > 0)
                .sorted(java.util.Comparator.comparing(SellerDashboardDueResponse.DueItem::dueDate))
                .toList();

        return new SellerDashboardDueResponse(receivables, dueTotal(receivables), payables, dueTotal(payables));
    }

    private SellerDashboardDueResponse.DueItem customerDue(CustomerReceivable row, LocalDate today) {
        BigDecimal paid = amountOrZero(receivablePaymentRepository.totalPaid(row));
        BigDecimal total = amountOrZero(row.getSaleAmount());
        BigDecimal remaining = total.subtract(paid).max(BigDecimal.ZERO);
        long overdueDays = Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(row.getDueDate(), today));
        return new SellerDashboardDueResponse.DueItem(row.getCustomerName(), row.getCustomerMobile(), row.getInvoiceNumber(), row.getDueDate(), total, paid, remaining, overdueDays, overdueDays > 0 ? "OVERDUE" : "DUE_TODAY");
    }

    private SellerDashboardDueResponse.DueItem distributorDue(Purchase purchase, LocalDate today) {
        if (purchase.getDistributor() == null || purchase.getDistributor().getCreditPeriodDays() == null || purchase.getPurchaseDate() == null) return null;
        LocalDate dueDate = purchase.getPurchaseDate().toLocalDate().plusDays(Math.max(0, purchase.getDistributor().getCreditPeriodDays()));
        if (dueDate.isAfter(today)) return null;
        BigDecimal paid = purchasePaymentRepository.findByPurchaseOrderByPaymentDateDesc(purchase).stream()
                .map(payment -> amountOrZero(payment.getAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = amountOrZero(purchase.getGrandTotal()).subtract(amountOrZero(creditNoteRepository.totalForPurchase(purchase))).max(BigDecimal.ZERO);
        BigDecimal remaining = total.subtract(paid).max(BigDecimal.ZERO);
        Distributor distributor = purchase.getDistributor().getDistributor();
        long overdueDays = Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(dueDate, today));
        return new SellerDashboardDueResponse.DueItem(distributor == null ? "Distributor" : distributor.getBusinessName(), distributor == null ? null : distributor.getMobile(), purchase.getInvoiceNumber(), dueDate, total, paid, remaining, overdueDays, overdueDays > 0 ? "OVERDUE" : "DUE_TODAY");
    }

    private BigDecimal dueTotal(List<SellerDashboardDueResponse.DueItem> rows) {
        return rows.stream().map(SellerDashboardDueResponse.DueItem::remainingAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private User getVerifiedSeller(String sellerEmail) {
        User seller = userRepository.findByEmail(sellerEmail)
                .orElseThrow(() -> new RuntimeException("Seller not found"));
        if (!seller.isEnabled() || seller.getRole() == null || !"SELLER".equals(seller.getRole().name())) {
            throw new RuntimeException("Only active sellers can access the seller dashboard");
        }
        return seller;
    }

    private SellerDashboardProductResponse toProductResponse(Product product) {
        String category = product.getCategoryEntity() != null
                ? product.getCategoryEntity().getName() : product.getCategory();
        String imageUrl = product.getImageUrl();
        if ((imageUrl == null || imageUrl.isBlank()) && product.getImages() != null && !product.getImages().isEmpty()) {
            imageUrl = product.getImages().get(0).getImageUrl();
        }
        return new SellerDashboardProductResponse(product.getId(), product.getName(), category,
                imageUrl, product.getFinalSellingPrice(), product.getStock(),
                product.isActive(), product.getApprovalStatus(), product.getUpdatedAt());
    }

    private BigDecimal amountOrZero(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
