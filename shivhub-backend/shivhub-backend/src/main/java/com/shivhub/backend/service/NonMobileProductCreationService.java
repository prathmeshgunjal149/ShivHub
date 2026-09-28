package com.shivhub.backend.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.shivhub.backend.dto.*;
import com.shivhub.backend.entity.Product;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.repository.CategoryRepository;
import com.shivhub.backend.repository.PurchaseItemSerialRepository;
import com.shivhub.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NonMobileProductCreationService {
    private final ProductService products;
    private final PurchaseService purchases;
    private final UserRepository users;
    private final CategoryRepository categories;
    private final PurchaseItemSerialRepository serials;

    public NonMobileProductCreationService(ProductService products, PurchaseService purchases,
            UserRepository users, CategoryRepository categories, PurchaseItemSerialRepository serials) {
        this.products = products; this.purchases = purchases; this.users = users;
        this.categories = categories; this.serials = serials;
    }

    public record CreatedProduct(Long productId, String name, String approvalStatus,
            boolean active, Integer stock, int serialUnitCount, Long purchaseId) {}

    // Serializable exact-code checks protect concurrent opening-stock registration.
    // No unit is SOLD here: existing purchase and completed billing own its lifecycle.
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CreatedProduct create(String email, CreateProductRequest request,
            MultipartFile[] images, List<String> imageUrls) {
        User seller = users.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!"SELLER".equals(String.valueOf(seller.getRole())) || !seller.isEnabled())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "An active seller account is required");
        if (request.getCategoryId() == null) fail("Select a category");
        var category = categories.findById(request.getCategoryId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found"));
        if ("Mobiles".equalsIgnoreCase(category.getName())) fail("Use the dedicated Add Mobile page for mobiles");
        request.setProductType("NON_MOBILE");
        int openingStock = request.getStock() == null ? 0 : request.getStock();
        boolean tracked = Boolean.TRUE.equals(request.getSerialTrackingRequired());
        InitialProductPurchaseRequest initial = request.getInitialPurchase();
        if (!tracked && initial != null) fail("Opening serial purchase requires IMEI/serial tracking");
        if (tracked && openingStock > 0) {
            if (request.getVariants() != null && !request.getVariants().isEmpty())
                fail("Serialized products must use the existing serial stock flow, not generic variants");
            validateInitialPurchase(initial, openingStock);
            // PurchaseService adds stock and records the purchase movement exactly once.
            request.setStock(0);
        } else if (initial != null) fail("Opening purchase requires a positive stock quantity");
        Product product = products.createProduct(request, seller.getId(), images, imageUrls);
        Long purchaseId = null;
        if (tracked && openingStock > 0) {
            PurchaseItemRequest item = new PurchaseItemRequest();
            item.setProductId(product.getId()); item.setQuantity(openingStock);
            item.setUnitPrice(initial.unitPrice()); item.setGstRate(initial.gstRate());
            item.setSerials(initial.serials());
            // Per-unit identifiers are validated below; serial-only laptops/TVs are permitted.
            item.setImeiTrackingRequired(false);
            PurchaseRequest purchase = new PurchaseRequest();
            purchase.setSellerDistributorId(initial.sellerDistributorId());
            purchase.setInvoiceNumber(initial.invoiceNumber().trim());
            purchase.setPurchaseDate(initial.purchaseDate()); purchase.setItems(List.of(item));
            purchaseId = purchases.createPurchase(purchase, seller).getId();
        }
        return new CreatedProduct(product.getId(), product.getName(), String.valueOf(product.getApprovalStatus()),
                product.isActive(), product.getStock(), purchaseId == null ? 0 : openingStock, purchaseId);
    }

    void validateInitialPurchase(InitialProductPurchaseRequest initial, int quantity) {
        if (initial == null || initial.sellerDistributorId() == null || initial.invoiceNumber() == null
                || initial.invoiceNumber().isBlank()) fail("Select your distributor and enter the actual purchase invoice number");
        if (quantity > 500) fail("Register at most 500 serialized units per opening purchase");
        if (initial.unitPrice() == null || initial.unitPrice().signum() <= 0)
            fail("Enter the actual purchase rate excluding GST");
        if (initial.gstRate() == null || initial.gstRate().signum() < 0
                || initial.gstRate().compareTo(new java.math.BigDecimal("100")) > 0) fail("Invalid purchase GST rate");
        if (initial.purchaseDate() != null && initial.purchaseDate().isAfter(java.time.LocalDateTime.now().plusMinutes(1)))
            fail("Purchase date cannot be in the future");
        if (initial.serials() == null || initial.serials().size() != quantity)
            fail("Enter one IMEI/serial record for each stock unit");
        Set<String> codes = new HashSet<>();
        for (PurchaseItemSerialRequest unit : initial.serials()) {
            if (unit == null) fail("Invalid serial unit");
            unit.setImei1(normalize(unit.getImei1())); unit.setImei2(normalize(unit.getImei2()));
            unit.setSerialNumber(normalize(unit.getSerialNumber()));
            if (unit.getImei1() == null && unit.getSerialNumber() == null) fail("Each unit needs IMEI 1 or a serial number");
            if (unit.getImei2() != null && unit.getImei1() == null) fail("IMEI 2 requires IMEI 1");
            for (String imei : new String[] {unit.getImei1(), unit.getImei2()})
                if (imei != null && !imei.matches("\\d{15}")) fail("IMEI must contain exactly 15 digits");
            if (unit.getSerialNumber() != null && unit.getSerialNumber().length() > 100) fail("Serial number is too long");
            for (String code : new String[] {unit.getImei1(), unit.getImei2(), unit.getSerialNumber()}) {
                if (code == null) continue;
                if (!codes.add(code.toLowerCase(java.util.Locale.ROOT))) fail("Duplicate IMEI/serial in opening stock");
                if (!serials.findByExactScanCode(code).isEmpty())
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "IMEI/serial is already registered");
            }
        }
    }
    private static String normalize(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private static void fail(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
