package com.shivhub.backend.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shivhub.backend.dto.AdminPurchaseApprovalResponse;
import com.shivhub.backend.entity.Distributor;
import com.shivhub.backend.entity.Product;
import com.shivhub.backend.entity.Purchase;
import com.shivhub.backend.entity.PurchaseItem;
import com.shivhub.backend.enums.ProductStatus;
import com.shivhub.backend.enums.PurchaseStatus;
import com.shivhub.backend.repository.PurchaseRepository;

/** Admin review for a supplier invoice which contains pending seller products. */
@Service
public class AdminPurchaseApprovalService {

    private final PurchaseRepository purchaseRepository;
    private final AdminProductService adminProductService;

    public AdminPurchaseApprovalService(
            PurchaseRepository purchaseRepository,
            AdminProductService adminProductService) {
        this.purchaseRepository = purchaseRepository;
        this.adminProductService = adminProductService;
    }

    @Transactional(readOnly = true)
    public List<AdminPurchaseApprovalResponse> pendingApprovals() {
        return purchaseRepository.findForProductApproval(ProductStatus.PENDING)
                .stream()
                .map(this::response)
                .toList();
    }

    /**
     * A purchase receipt is already saved for audit. Approval makes each of
     * its pending product listings sellable, using the invoice as review proof.
     */
    @Transactional
    public void approve(Long purchaseId, String review) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new RuntimeException("Purchase invoice not found"));

        if (purchase.getStatus() != PurchaseStatus.COMPLETED) {
            throw new RuntimeException("Only completed purchase receipts can be approved");
        }

        List<Long> pendingProductIds = purchase.getItems().stream()
                .map(PurchaseItem::getProduct)
                .filter(product -> product != null && product.getApprovalStatus() == ProductStatus.PENDING)
                .map(Product::getId)
                .distinct()
                .toList();

        if (pendingProductIds.isEmpty()) {
            throw new RuntimeException("No pending products remain on this purchase invoice");
        }

        String note = review == null || review.isBlank()
                ? "Approved from purchase invoice " + purchase.getInvoiceNumber() + "."
                : review.trim();

        pendingProductIds.forEach(productId ->
                adminProductService.approveProduct(productId, note));
    }

    private AdminPurchaseApprovalResponse response(Purchase purchase) {
        Distributor supplier = purchase.getDistributor() == null
                ? null
                : purchase.getDistributor().getDistributor();

        List<AdminPurchaseApprovalResponse.Item> items = purchase.getItems().stream()
                .map(item -> itemResponse(item))
                .toList();

        int pendingProductCount = (int) purchase.getItems().stream()
                .map(PurchaseItem::getProduct)
                .filter(product -> product != null && product.getApprovalStatus() == ProductStatus.PENDING)
                .map(Product::getId)
                .distinct()
                .count();

        return new AdminPurchaseApprovalResponse(
                purchase.getId(),
                purchase.getInvoiceNumber(),
                purchase.getPurchaseDate(),
                purchase.getCreatedAt(),
                purchase.getSeller() == null ? null : purchase.getSeller().getName(),
                purchase.getSeller() == null ? null : purchase.getSeller().getEmail(),
                purchase.getSeller() == null ? null : purchase.getSeller().getMobile(),
                supplier == null ? null : supplier.getBusinessName(),
                supplier == null ? null : supplier.getGstin(),
                supplier == null ? null : firstNonBlank(supplier.getMobile(), supplier.getEmail()),
                money(purchase.getSubtotal()),
                money(purchase.getCgst()).add(money(purchase.getSgst())).add(money(purchase.getIgst())),
                money(purchase.getDiscount()),
                money(purchase.getGrandTotal()),
                purchase.getInvoiceFileUrl(),
                purchase.getNotes(),
                pendingProductCount,
                items
        );
    }

    private AdminPurchaseApprovalResponse.Item itemResponse(PurchaseItem item) {
        Product product = item.getProduct();

        return new AdminPurchaseApprovalResponse.Item(
                product == null ? null : product.getId(),
                item.getProductName(),
                item.getSku(),
                item.getBrand(),
                item.getModel(),
                item.getColor(),
                item.getRam(),
                item.getStorage(),
                item.getQuantity(),
                item.getUnit(),
                money(item.getUnitPrice()),
                money(item.getTotalPrice()),
                product != null && product.getApprovalStatus() == ProductStatus.PENDING
        );
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }
}
