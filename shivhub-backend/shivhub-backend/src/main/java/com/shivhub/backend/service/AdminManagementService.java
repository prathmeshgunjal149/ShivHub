package com.shivhub.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shivhub.backend.dto.AdminCustomerSummaryResponse;
import com.shivhub.backend.dto.AdminPageResponse;
import com.shivhub.backend.dto.AdminSellerSummaryResponse;
import com.shivhub.backend.entity.*;
import com.shivhub.backend.enums.OrderStatus;
import com.shivhub.backend.enums.ProductStatus;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.enums.UserStatus;
import com.shivhub.backend.repository.*;

/**
 * Admin-only reporting read model. Financial values are calculated from the source
 * ledger/bill/order tables; no dashboard totals are stored as dummy data.
 */
@Service
@Transactional
public class AdminManagementService {
    private final UserRepository users;
    private final ShopRepository shops;
    private final ProductRepository products;
    private final OfflineBillRepository offlineBills;
    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final CustomerReceivableRepository receivables;
    private final CustomerReceivablePaymentRepository receivablePayments;
    private final PurchaseRepository purchases;
    private final PurchasePaymentRepository purchasePayments;
    private final ShopStaffAssignmentRepository staffAssignments;
    private final CustomerProfileRepository customerProfiles;
    private final SellerCustomerMappingRepository sellerCustomers;
    private final OfflineBillInvoiceService offlineBillInvoices;

    public AdminManagementService(
            UserRepository users, ShopRepository shops, ProductRepository products,
            OfflineBillRepository offlineBills, OrderRepository orders, OrderItemRepository orderItems,
            CustomerReceivableRepository receivables, CustomerReceivablePaymentRepository receivablePayments,
            PurchaseRepository purchases, PurchasePaymentRepository purchasePayments,
            ShopStaffAssignmentRepository staffAssignments, CustomerProfileRepository customerProfiles,
            SellerCustomerMappingRepository sellerCustomers, OfflineBillInvoiceService offlineBillInvoices) {
        this.users = users; this.shops = shops; this.products = products; this.offlineBills = offlineBills;
        this.orders = orders; this.orderItems = orderItems; this.receivables = receivables;
        this.receivablePayments = receivablePayments; this.purchases = purchases;
        this.purchasePayments = purchasePayments; this.staffAssignments = staffAssignments;
        this.customerProfiles = customerProfiles; this.sellerCustomers = sellerCustomers;
        this.offlineBillInvoices = offlineBillInvoices;
    }

    public AdminPageResponse<AdminSellerSummaryResponse> sellers(
            String search, String approvalStatus, String city, LocalDate registeredFrom,
            LocalDate registeredTo, Boolean active, Boolean gstVerified, int page, int size) {
        syncCustomerDirectory();
        List<AdminSellerSummaryResponse> result = users.findByRole(Role.SELLER).stream()
                .map(this::sellerSummary)
                .filter(s -> contains(search, s.ownerName(), s.shopName(), s.email(), s.mobile(), s.gstin()))
                .filter(s -> blank(approvalStatus) || approvalStatus.equalsIgnoreCase(s.approvalStatus()))
                .filter(s -> blank(city) || city.equalsIgnoreCase(s.city()))
                .filter(s -> active == null || active.equals(s.active()))
                .filter(s -> gstVerified == null || gstVerified.equals(s.gstVerified()))
                .filter(s -> registeredFrom == null || !s.registrationDate().toLocalDate().isBefore(registeredFrom))
                .filter(s -> registeredTo == null || !s.registrationDate().toLocalDate().isAfter(registeredTo))
                .sorted(Comparator.comparing(AdminSellerSummaryResponse::registrationDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return page(result, page, size);
    }

    public Map<String, Object> sellerDetails(Long sellerId) {
        syncCustomerDirectory();
        User seller = seller(sellerId);
        AdminSellerSummaryResponse summary = sellerSummary(seller);
        List<Shop> sellerShops = shops.findByOwnerOrderByNameAsc(seller);
        Map<String, Object> profile = row(
                "ownerName", seller.getName(), "shopName", summary.shopName(), "shopLogoUrl", seller.getShopLogoUrl(),
                "email", seller.getEmail(), "mobile", seller.getMobile(), "alternateMobile", seller.getAlternateMobile(),
                "address", summary.address(), "city", summary.city(), "district", seller.getBusinessDistrict(),
                "state", seller.getBusinessState(), "pincode", seller.getBusinessPincode(), "gstin", seller.getGstin(),
                "legalBusinessName", seller.getLegalBusinessName(), "websiteUrl", seller.getWebsiteUrl(),
                "instagramUrl", seller.getInstagramUrl(), "facebookUrl", seller.getFacebookUrl(),
                "whatsappUrl", seller.getWhatsappUrl(), "youtubeUrl", seller.getYoutubeUrl(),
                "approvalStatus", seller.getStatus().name(), "active", summary.active(),
                "registrationDate", seller.getCreatedAt(), "lastLoginAt", seller.getLastLoginAt(), "shops", sellerShops.stream()
                        .map(shop -> row("id", shop.getId(), "name", shop.getName(), "city", shop.getCity(), "address", shop.getAddress(), "active", shop.isActive()))
                        .toList());
        Map<String, Object> financial = row(
                "offlineSales", summary.offlineSales(), "onlineSales", summary.onlineSales(),
                "totalRevenue", summary.totalRevenue(), "profitEstimate", estimateProfit(seller),
                "totalProducts", summary.totalProducts(), "stockValue", summary.stockValue(),
                "customers", summary.totalCustomers(), "customerReceivable", summary.customerReceivable(),
                "pendingDistributorPayment", summary.pendingDistributorPayment());
        return row("seller", summary, "profile", profile, "financial", financial,
                "products", sellerProducts(seller), "sales", sellerSales(sellerId, null, null),
                "customers", sellerCustomerList(seller, null, null, null, null, null, 0, 200).content(),
                "payments", sellerPayments(sellerId), "staff", sellerStaff(seller));
    }

    public List<Map<String, Object>> sellerSales(Long sellerId, LocalDate from, LocalDate to) {
        User seller = seller(sellerId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (OfflineBill bill : offlineBills.findBySellerIdOrderByCreatedAtDesc(sellerId)) {
            if (!inside(bill.getCreatedAt(), from, to)) continue;
            result.add(row("id", bill.getId(), "type", "OFFLINE_BILL", "reference", bill.getBillNumber(),
                    "date", bill.getCreatedAt(), "customerName", bill.getCustomerName(), "customerMobile", bill.getCustomerMobile(),
                    "amount", money(bill.getGrandTotal()), "gst", money(bill.getCgst()).add(money(bill.getSgst())).add(money(bill.getIgst())),
                    "paymentStatus", paymentStatus(bill), "invoiceUrl", "/api/admin/sellers/" + sellerId + "/sales/OFFLINE_BILL/" + bill.getId() + "/invoice"));
        }
        for (OrderItem item : orderItems.findSellerOrderItems(sellerId)) {
            Order order = item.getOrder();
            if (order.getOrderStatus() == OrderStatus.CANCELLED || !inside(order.getCreatedAt(), from, to)) continue;
            User buyer = users.findById(order.getCustomerId()).orElse(null);
            result.add(row("id", item.getId(), "orderId", order.getId(), "type", "ONLINE_ORDER", "reference", order.getOrderNumber(),
                    "date", order.getCreatedAt(), "customerName", buyer == null ? null : buyer.getName(),
                    "customerMobile", buyer == null ? null : buyer.getMobile(), "productName", item.getProductName(),
                    "amount", money(item.getTotalPrice()), "gst", BigDecimal.ZERO, "paymentStatus", order.getPaymentStatus().name(),
                    "status", order.getOrderStatus().name(), "invoiceUrl", "/api/admin/orders/" + order.getId() + "/invoice"));
        }
        return result.stream().sorted(Comparator.comparing(row -> (LocalDateTime) row.get("date"), Comparator.nullsLast(Comparator.reverseOrder()))).toList();
    }

    /** Generates a POS invoice only after verifying it belongs to the selected seller. */
    public byte[] sellerOfflineInvoice(Long sellerId, Long billId) {
        seller(sellerId);
        OfflineBill bill = offlineBills.findById(billId)
                .orElseThrow(() -> new RuntimeException("Offline bill not found"));
        if (!sellerId.equals(bill.getSellerId())) {
            throw new IllegalArgumentException("Bill does not belong to this seller");
        }
        return offlineBillInvoices.generateInvoicePdf(bill);
    }

    public AdminPageResponse<AdminCustomerSummaryResponse> sellerCustomers(
            Long sellerId, String search, LocalDate from, LocalDate to, BigDecimal minimumAmount,
            BigDecimal maximumAmount, int page, int size) {
        syncCustomerDirectory();
        return sellerCustomerList(seller(sellerId), search, from, to, minimumAmount, maximumAmount, page, size);
    }

    public Map<String, Object> sellerPayments(Long sellerId) {
        User seller = seller(sellerId);
        List<Map<String, Object>> distributorPayments = new ArrayList<>();
        BigDecimal pending = BigDecimal.ZERO;
        for (Purchase purchase : purchases.findBySellerOrderByPurchaseDateDesc(seller)) {
            BigDecimal paid = purchasePayments.findByPurchaseOrderByPaymentDateDesc(purchase).stream()
                    .map(PurchasePayment::getAmount).map(this::money).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal balance = nonNegative(money(purchase.getGrandTotal()).subtract(paid));
            pending = pending.add(balance);
            distributorPayments.add(row("purchaseId", purchase.getId(), "invoiceNumber", purchase.getInvoiceNumber(),
                    "distributor", purchase.getDistributor() == null ? null : purchase.getDistributor().getBrand(),
                    "date", purchase.getPurchaseDate(), "amount", money(purchase.getGrandTotal()), "paid", paid, "pending", balance,
                    "status", balance.signum() == 0 ? "PAID" : "PENDING"));
        }
        List<Map<String, Object>> customerDues = sellerReceivables(seller);
        return row("pendingDistributorPayment", pending, "distributorPayments", distributorPayments,
                "customerReceivable", customerDues.stream().map(r -> (BigDecimal) r.get("pending")).reduce(BigDecimal.ZERO, BigDecimal::add),
                "customerReceivables", customerDues);
    }

    public AdminPageResponse<AdminCustomerSummaryResponse> customers(
            String search, String type, Long sellerId, String city, LocalDate from, LocalDate to,
            BigDecimal minimumAmount, BigDecimal maximumAmount, int page, int size) {
        // Backfill every historic POS buyer before reading the directory.  In
        // particular, a walk-in bill can have only a name, so it must still
        // receive its own canonical profile instead of disappearing from the
        // admin customer view.
        syncCustomerDirectory();
        List<OfflineBill> bills = offlineBills.findAll();
        List<AdminCustomerSummaryResponse> result = uniqueCustomerProfiles().stream()
                .map(profile -> customerSummary(profile, bills))
                .filter(c -> contains(search, c.name(), c.mobile(), c.email()))
                .filter(c -> blank(type) || "ALL".equalsIgnoreCase(type) || type.equalsIgnoreCase(c.customerType()))
                .filter(c -> sellerId == null || sellerCustomers.findByCustomerProfile(customerProfiles.getReferenceById(c.id())).stream()
                        .anyMatch(mapping -> mapping.getSeller().getId().equals(sellerId)))
                .filter(c -> blank(city) || contains(city, c.address()))
                .filter(c -> minimumAmount == null || c.totalPurchaseAmount().compareTo(minimumAmount) >= 0)
                .filter(c -> maximumAmount == null || c.totalPurchaseAmount().compareTo(maximumAmount) <= 0)
                .filter(c -> from == null || (c.lastPurchaseDate() != null && !c.lastPurchaseDate().toLocalDate().isBefore(from)))
                .filter(c -> to == null || (c.lastPurchaseDate() != null && !c.lastPurchaseDate().toLocalDate().isAfter(to)))
                .sorted(Comparator.comparing(AdminCustomerSummaryResponse::lastPurchaseDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return page(result, page, size);
    }

    public Map<String, Object> customerDetails(Long profileId) {
        syncCustomerDirectory();
        CustomerProfile profile = customerProfiles.findById(profileId).orElseThrow(() -> new RuntimeException("Customer not found"));
        List<OfflineBill> matchedBills = billsFor(profile, offlineBills.findAll());
        AdminCustomerSummaryResponse summary = customerSummary(profile, matchedBills, false);
        List<Map<String, Object>> purchases = new ArrayList<>();
        for (OfflineBill bill : matchedBills) {
            purchases.add(row("id", bill.getId(), "type", "OFFLINE_BILL", "reference", bill.getBillNumber(), "date", bill.getCreatedAt(),
                    "amount", money(bill.getGrandTotal()), "status", "COMPLETED", "paymentStatus", paymentStatus(bill),
                    "products", bill.getItems().stream().map(item -> row("name", item.getProductName(), "quantity", item.getQuantity(), "amount", money(item.getTotalPrice()))).toList()));
        }
        if (profile.getOnlineUser() != null) {
            for (Order order : orders.findByCustomerIdOrderByCreatedAtDesc(profile.getOnlineUser().getId())) {
                if (order.getOrderStatus() == OrderStatus.CANCELLED) continue;
                purchases.add(row("id", order.getId(), "type", "ONLINE_ORDER", "reference", order.getOrderNumber(), "date", order.getCreatedAt(),
                        "amount", money(order.getGrandTotal()), "status", order.getOrderStatus().name(), "paymentStatus", order.getPaymentStatus().name(),
                        "products", orderItems.findByOrderId(order.getId()).stream().map(item -> row("name", item.getProductName(), "quantity", item.getQuantity(), "amount", money(item.getTotalPrice()))).toList()));
            }
        }
        purchases.sort(Comparator.comparing(row -> (LocalDateTime) row.get("date"), Comparator.nullsLast(Comparator.reverseOrder())));
        List<Map<String, Object>> sellerList = sellerCustomers.findByCustomerProfile(profile).stream()
                .map(mapping -> row("sellerId", mapping.getSeller().getId(), "sellerName", mapping.getSeller().getName(),
                        "businessName", mapping.getSeller().getBusinessName(), "firstPurchaseAt", mapping.getFirstPurchaseAt(), "lastPurchaseAt", mapping.getLastPurchaseAt()))
                .toList();
        return row("customer", summary, "purchaseHistory", purchases, "associatedSellers", sellerList,
                "communication", row("consent", profile.isCommunicationConsent(), "onlineMarketingOptOut", profile.getOnlineUser() != null && profile.getOnlineUser().isMarketingOptOut()));
    }

    public void setSellerProductStatus(Long sellerId, Long productId, ProductStatus status) {
        Product product = products.findById(productId).orElseThrow(() -> new RuntimeException("Product not found"));
        if (!product.getSeller().getId().equals(sellerId)) throw new IllegalArgumentException("Product does not belong to this seller");
        product.setApprovalStatus(status);
        if (status == ProductStatus.REJECTED || status == ProductStatus.SUSPENDED) product.setActive(false);
        products.save(product);
    }

    public void suspendSeller(Long sellerId) {
        User seller = seller(sellerId);
        seller.setStatus(UserStatus.SUSPENDED);
        seller.setEnabled(false);
        users.save(seller);
    }

    /** Campaign service uses this selector for preview, send-now, and scheduled sends. */
    public List<CampaignRecipient> resolveCampaignAudience(
            List<Long> sellerIds, String customerType, String city, Integer inactiveDays,
            BigDecimal minimumPurchaseAmount, Long productId, String productCategory) {
        syncCustomerDirectory();
        LocalDateTime inactiveBefore = inactiveDays == null || inactiveDays <= 0 ? null : LocalDateTime.now().minusDays(inactiveDays);
        Set<Long> selectedSellers = sellerIds == null ? Set.of() : new HashSet<>(sellerIds);
        List<OfflineBill> allBills = offlineBills.findAll();
        return uniqueCustomerProfiles().stream()
                .map(profile -> new CampaignRecipient(profile, customerSummary(profile, allBills)))
                .filter(candidate -> blank(customerType) || "ALL".equalsIgnoreCase(customerType) || customerType.equalsIgnoreCase(candidate.summary.customerType()))
                .filter(candidate -> blank(city) || contains(city, candidate.summary.address()))
                .filter(candidate -> minimumPurchaseAmount == null || candidate.summary.totalPurchaseAmount().compareTo(minimumPurchaseAmount) >= 0)
                .filter(candidate -> inactiveBefore == null || candidate.summary.lastPurchaseDate() == null || candidate.summary.lastPurchaseDate().isBefore(inactiveBefore))
                .filter(candidate -> selectedSellers.isEmpty() || sellerCustomers.findByCustomerProfile(candidate.profile).stream().anyMatch(mapping -> selectedSellers.contains(mapping.getSeller().getId())))
                .filter(candidate -> productId == null || hasPurchasedProduct(candidate.profile, productId, allBills))
                .filter(candidate -> blank(productCategory) || hasPurchasedCategory(candidate.profile, productCategory, allBills))
                .filter(candidate -> candidate.profile.isCommunicationConsent())
                .toList();
    }

    public record CampaignRecipient(CustomerProfile profile, AdminCustomerSummaryResponse summary) { }

    /** Read-only counts from actual unique customer identities; no dashboard read creates a profile. */
    @Transactional(readOnly = true)
    public Map<String, Object> customerCounts() {
        syncCustomerDirectory();
        List<CustomerProfile> profiles = uniqueCustomerProfiles();
        long online = profiles.stream().filter(profile -> profile.getOnlineUser() != null).count();
        long seller = profiles.stream().filter(profile -> !sellerCustomers.findByCustomerProfile(profile).isEmpty()).count();
        long offlineRegistered = profiles.stream().filter(profile -> profile.getOnlineUser() == null && !sellerCustomers.findByCustomerProfile(profile).isEmpty()).count();
        long walkIn = profiles.stream().filter(profile -> profile.getOnlineUser() == null && sellerCustomers.findByCustomerProfile(profile).isEmpty()).count();
        Map<String, Long> sellerWise = new LinkedHashMap<>();
        sellerCustomers.findAll().stream().filter(mapping -> customerIdentity(mapping.getCustomerProfile()) != null).collect(Collectors.groupingBy(mapping -> mapping.getSeller().getId(), Collectors.mapping(mapping -> customerIdentity(mapping.getCustomerProfile()), Collectors.toSet())))
                .forEach((sellerId, customerIds) -> sellerWise.put(String.valueOf(sellerId), (long) customerIds.size()));
        return row("totalUniqueCustomers", (long) profiles.size(), "shivHubOnlineCustomers", online, "sellerCustomers", seller,
                "offlineRegisteredCustomers", offlineRegistered, "walkInCustomers", walkIn, "sellerWise", sellerWise);
    }

    private AdminSellerSummaryResponse sellerSummary(User seller) {
        List<Shop> sellerShops = shops.findByOwnerOrderByNameAsc(seller);
        Shop primaryShop = sellerShops.stream().findFirst().orElse(null);
        List<Product> sellerProducts = products.findBySeller(seller);
        int stock = sellerProducts.stream().mapToInt(Product::getAvailableStock).sum();
        BigDecimal stockValue = sellerProducts.stream().map(product -> money(product.getPrice()).multiply(BigDecimal.valueOf(product.getAvailableStock()))).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<OfflineBill> bills = offlineBills.findBySellerIdOrderByCreatedAtDesc(seller.getId());
        BigDecimal offline = bills.stream().map(OfflineBill::getGrandTotal).map(this::money).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<OrderItem> onlineItems = orderItems.findSellerOrderItems(seller.getId()).stream().filter(item -> item.getOrder().getOrderStatus() != OrderStatus.CANCELLED).toList();
        BigDecimal online = onlineItems.stream().map(OrderItem::getTotalPrice).map(this::money).reduce(BigDecimal.ZERO, BigDecimal::add);
        long onlineOrders = onlineItems.stream().map(item -> item.getOrder().getId()).distinct().count();
        BigDecimal pendingReceivables = sellerReceivables(seller).stream().map(row -> (BigDecimal) row.get("pending")).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal distributorDue = pendingDistributorDue(seller);
        boolean shopActive = sellerShops.isEmpty() ? seller.isEnabled() : sellerShops.stream().anyMatch(Shop::isActive) && seller.isEnabled();
        return new AdminSellerSummaryResponse(seller.getId(), seller.getName(), businessName(seller, primaryShop), seller.getShopLogoUrl(),
                seller.getEmail(), seller.getMobile(), businessAddress(seller, primaryShop), city(seller, primaryShop), seller.getGstin(),
                seller.getStatus().name(), shopActive, !blank(seller.getGstin()), sellerProducts.size(), stock, stockValue, offline, online,
                bills.size() + onlineOrders, offline.add(online), distributorDue, pendingReceivables,
                sellerCustomers.findBySeller(seller).stream().map(SellerCustomerMapping::getCustomerProfile).map(this::customerIdentity).filter(Objects::nonNull).distinct().count(), seller.getCreatedAt(), seller.getLastLoginAt());
    }

    private List<Map<String, Object>> sellerProducts(User seller) {
        return products.findBySeller(seller).stream().map(product -> row(
                "id", product.getId(), "imageUrl", product.getImageUrl(), "name", product.getName(), "category", product.getCategory(),
                "sellingPrice", money(product.getFinalSellingPrice()), "stock", product.getAvailableStock(), "active", product.isActive(),
                "status", product.getApprovalStatus().name(), "lowStock", product.getAvailableStock() > 0 && product.getAvailableStock() <= 5,
                "outOfStock", product.getAvailableStock() <= 0)).toList();
    }

    private List<Map<String, Object>> sellerReceivables(User seller) {
        return receivables.findBySellerOrderByDueDateAsc(seller).stream().map(receivable -> {
            BigDecimal paid = money(receivablePayments.totalPaid(receivable));
            BigDecimal pending = nonNegative(money(receivable.getSaleAmount()).subtract(paid));
            boolean overdue = pending.signum() > 0 && receivable.getDueDate() != null && receivable.getDueDate().isBefore(LocalDate.now());
            return row("id", receivable.getId(), "customerName", receivable.getCustomerName(), "mobile", receivable.getCustomerMobile(),
                    "email", receivable.getCustomerEmail(), "invoiceNumber", receivable.getInvoiceNumber(), "saleDate", receivable.getSaleDate(),
                    "dueDate", receivable.getDueDate(), "amount", money(receivable.getSaleAmount()), "paid", paid, "pending", pending, "overdue", overdue);
        }).toList();
    }

    private List<Map<String, Object>> sellerStaff(User seller) {
        List<OfflineBill> bills = offlineBills.findBySellerIdOrderByCreatedAtDesc(seller.getId());
        return staffAssignments.findByShopOwnerAndActiveTrue(seller).stream().map(assignment -> {
            User staff = assignment.getStaff();
            List<OfflineBill> staffBills = bills.stream().filter(bill -> staff.getId().equals(bill.getSalesPersonId())).toList();
            return row("id", staff.getId(), "name", staff.getName(), "email", staff.getEmail(), "mobile", staff.getMobile(),
                    "role", assignment.getAccessRole().name(), "shop", assignment.getShop().getName(), "bills", staffBills.size(),
                    "sales", staffBills.stream().map(OfflineBill::getGrandTotal).map(this::money).reduce(BigDecimal.ZERO, BigDecimal::add),
                    "customersHandled", staffBills.stream().map(this::contactKey).filter(Objects::nonNull).distinct().count());
        }).toList();
    }

    private AdminPageResponse<AdminCustomerSummaryResponse> sellerCustomerList(User seller, String search, LocalDate from, LocalDate to,
            BigDecimal minimumAmount, BigDecimal maximumAmount, int page, int size) {
        List<OfflineBill> allBills = offlineBills.findAll();
        List<AdminCustomerSummaryResponse> result = sellerCustomers.findBySeller(seller).stream()
                .map(SellerCustomerMapping::getCustomerProfile).distinct().map(profile -> customerSummary(profile, allBills))
                .filter(customer -> contains(search, customer.name(), customer.mobile(), customer.email()))
                .filter(customer -> minimumAmount == null || customer.totalPurchaseAmount().compareTo(minimumAmount) >= 0)
                .filter(customer -> maximumAmount == null || customer.totalPurchaseAmount().compareTo(maximumAmount) <= 0)
                .filter(customer -> from == null || (customer.lastPurchaseDate() != null && !customer.lastPurchaseDate().toLocalDate().isBefore(from)))
                .filter(customer -> to == null || (customer.lastPurchaseDate() != null && !customer.lastPurchaseDate().toLocalDate().isAfter(to)))
                .sorted(Comparator.comparing(AdminCustomerSummaryResponse::lastPurchaseDate, Comparator.nullsLast(Comparator.reverseOrder()))).toList();
        return page(result, page, size);
    }

    private AdminCustomerSummaryResponse customerSummary(CustomerProfile profile, List<OfflineBill> allBills) {
        return customerSummary(profile, allBills, true);
    }

    private AdminCustomerSummaryResponse customerSummary(CustomerProfile profile, List<OfflineBill> candidateBills, boolean candidatesMayNeedMatching) {
        List<OfflineBill> bills = candidatesMayNeedMatching ? billsFor(profile, candidateBills) : candidateBills;
        List<Order> onlineOrders = profile.getOnlineUser() == null ? List.of() : orders.findByCustomerIdOrderByCreatedAtDesc(profile.getOnlineUser().getId()).stream()
                .filter(order -> order.getOrderStatus() != OrderStatus.CANCELLED).toList();
        BigDecimal offlineAmount = bills.stream().map(OfflineBill::getGrandTotal).map(this::money).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal onlineAmount = onlineOrders.stream().map(Order::getGrandTotal).map(this::money).reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDateTime last = bills.stream().map(OfflineBill::getCreatedAt).filter(Objects::nonNull).max(LocalDateTime::compareTo).orElse(null);
        for (Order order : onlineOrders) if (last == null || order.getCreatedAt().isAfter(last)) last = order.getCreatedAt();
        Set<Long> sellerIds = new LinkedHashSet<>();
        bills.forEach(bill -> sellerIds.add(bill.getSellerId()));
        onlineOrders.forEach(order -> orderItems.findByOrderId(order.getId()).forEach(item -> { if (item.getSellerId() != null) sellerIds.add(item.getSellerId()); }));
        List<String> sellerNames = sellerIds.stream().map(id -> users.findById(id).map(user -> businessName(user, shops.findByOwnerOrderByNameAsc(user).stream().findFirst().orElse(null))).orElse("Seller #" + id)).toList();
        BigDecimal due = receivables.findAll().stream().filter(receivable -> matches(profile, receivable.getCustomerName(), receivable.getCustomerMobile(), receivable.getCustomerEmail()))
                .map(receivable -> nonNegative(money(receivable.getSaleAmount()).subtract(money(receivablePayments.totalPaid(receivable))))).reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean online = profile.getOnlineUser() != null;
        boolean sellerCustomer = !sellerIds.isEmpty() || !sellerCustomers.findByCustomerProfile(profile).isEmpty();
        String type = online && sellerCustomer ? "BOTH" : online ? "ONLINE" : "SELLER";
        return new AdminCustomerSummaryResponse(profile.getId(), online ? profile.getOnlineUser().getId() : null, profile.getName(), profile.getMobile(), profile.getEmail(), profile.getAddress(),
                type, sellerNames, bills.size() + onlineOrders.size(), offlineAmount.add(onlineAmount), due, last,
                profile.isCommunicationConsent(), !online || profile.getOnlineUser().isEnabled());
    }

    private List<OfflineBill> billsFor(CustomerProfile profile, List<OfflineBill> source) {
        return source.stream().filter(bill ->
                (profile.getId() != null && profile.getId().equals(bill.getCustomerProfileId()))
                || (profile.getOnlineUser() != null && profile.getOnlineUser().getId().equals(bill.getCustomerId()))
                || matches(profile, bill.getCustomerName(), bill.getCustomerMobile(), bill.getCustomerEmail())
        ).toList();
    }

    private boolean matches(CustomerProfile profile, String name, String mobile, String email) {
        if (!blank(profile.getMobile()) && profile.getMobile().equals(normalizeContact(mobile))) return true;
        return !blank(profile.getEmail()) && profile.getEmail().equalsIgnoreCase(email == null ? "" : email.trim());
    }

    private boolean hasPurchasedProduct(CustomerProfile profile, Long productId, List<OfflineBill> allBills) {
        boolean offline = billsFor(profile, allBills).stream().flatMap(bill -> bill.getItems().stream()).anyMatch(item -> productId.equals(item.getProductId()));
        if (offline) return true;
        return profile.getOnlineUser() != null && orders.findByCustomerIdOrderByCreatedAtDesc(profile.getOnlineUser().getId()).stream()
                .anyMatch(order -> orderItems.findByOrderId(order.getId()).stream().anyMatch(item -> productId.equals(item.getProductId())));
    }

    private boolean hasPurchasedCategory(CustomerProfile profile, String productCategory, List<OfflineBill> allBills) {
        boolean offline = billsFor(profile, allBills).stream().flatMap(bill -> bill.getItems().stream())
                .anyMatch(item -> productInCategory(item.getProductId(), productCategory));
        if (offline) return true;
        return profile.getOnlineUser() != null && orders.findByCustomerIdOrderByCreatedAtDesc(profile.getOnlineUser().getId()).stream()
                .anyMatch(order -> orderItems.findByOrderId(order.getId()).stream()
                        .anyMatch(item -> productInCategory(item.getProductId(), productCategory)));
    }

    private boolean productInCategory(Long productId, String category) {
        if (productId == null) return false;
        return products.findById(productId).map(product -> contains(category, product.getCategory(),
                product.getCategoryEntity() == null ? null : product.getCategoryEntity().getName())).orElse(false);
    }

    /** Hydrates customer_profiles and seller_customer_mappings from existing real users, bills and online orders. */
    private void syncCustomerDirectory() {
        Map<Long, User> allUsers = users.findAll().stream().collect(Collectors.toMap(User::getId, user -> user));
        Map<String, User> usersByMobile = allUsers.values().stream().filter(user -> user.getRole() == Role.CUSTOMER && !blank(user.getMobile()))
                .collect(Collectors.toMap(user -> normalizeContact(user.getMobile()), user -> user, (first, ignored) -> first));
        Map<String, User> usersByEmail = allUsers.values().stream().filter(user -> user.getRole() == Role.CUSTOMER && !blank(user.getEmail()))
                .collect(Collectors.toMap(user -> user.getEmail().trim().toLowerCase(Locale.ROOT), user -> user, (first, ignored) -> first));
        for (User customer : allUsers.values().stream().filter(user -> user.getRole() == Role.CUSTOMER).toList()) ensureProfile(customer, customer.getName(), customer.getMobile(), customer.getEmail(), null);
        for (OfflineBill bill : offlineBills.findAll()) {
            User customer = bill.getCustomerId() == null ? null : allUsers.get(bill.getCustomerId());
            if (customer == null && !blank(bill.getCustomerMobile())) customer = usersByMobile.get(normalizeContact(bill.getCustomerMobile()));
            if (customer == null && !blank(bill.getCustomerEmail())) customer = usersByEmail.get(bill.getCustomerEmail().trim().toLowerCase(Locale.ROOT));
            CustomerProfile profile = bill.getCustomerProfileId() == null ? null : customerProfiles.findById(bill.getCustomerProfileId()).orElse(null);
            if (profile == null) {
                profile = ensureProfile(customer, bill.getCustomerName(), bill.getCustomerMobile(), bill.getCustomerEmail(), bill.getCustomerAddress());
            }
            User seller = allUsers.get(bill.getSellerId());
            if (profile != null) {
                // Persist the canonical relation once.  This prevents a
                // name-only walk-in customer from becoming a fresh profile on
                // each admin page refresh, while preserving the bill snapshot.
                if (!Objects.equals(bill.getCustomerProfileId(), profile.getId())) {
                    bill.setCustomerProfileId(profile.getId());
                    offlineBills.save(bill);
                }
                if (seller != null && seller.getRole() == Role.SELLER) link(seller, profile, bill.getCreatedAt());
            }
        }
        for (Order order : orders.findAll()) {
            User customer = allUsers.get(order.getCustomerId());
            if (customer == null || customer.getRole() != Role.CUSTOMER) continue;
            CustomerProfile profile = ensureProfile(customer, customer.getName(), customer.getMobile(), customer.getEmail(), null);
            for (OrderItem item : orderItems.findByOrderId(order.getId())) {
                User seller = item.getSellerId() == null ? null : allUsers.get(item.getSellerId());
                if (seller != null && seller.getRole() == Role.SELLER) link(seller, profile, order.getCreatedAt());
            }
        }
    }

    private CustomerProfile ensureProfile(User onlineUser, String name, String mobile, String email, String address) {
        CustomerProfile profile = onlineUser == null ? null : customerProfiles.findByOnlineUser(onlineUser).orElse(null);
        if (profile == null && !blank(mobile)) profile = customerProfiles.findFirstByMobile(normalizeContact(mobile)).orElse(null);
        if (profile == null && !blank(email)) profile = customerProfiles.findFirstByEmailIgnoreCase(email.trim()).orElse(null);
        if (profile == null) {
            if (blank(name) && blank(mobile) && blank(email)) return null;
            profile = new CustomerProfile();
            profile.setName(blank(name) ? "Walk-in customer" : name.trim());
            profile.setMobile(normalizeContact(mobile)); profile.setEmail(blank(email) ? null : email.trim()); profile.setAddress(address);
        }
        if (onlineUser != null) { profile.setOnlineUser(onlineUser); profile.setCommunicationConsent(!onlineUser.isMarketingOptOut()); }
        if (blank(profile.getName()) && !blank(name)) profile.setName(name.trim());
        if (blank(profile.getMobile()) && !blank(mobile)) profile.setMobile(normalizeContact(mobile));
        if (blank(profile.getEmail()) && !blank(email)) profile.setEmail(email.trim());
        if (blank(profile.getAddress()) && !blank(address)) profile.setAddress(address);
        return customerProfiles.save(profile);
    }

    private void link(User seller, CustomerProfile profile, LocalDateTime at) {
        SellerCustomerMapping mapping = sellerCustomers.findBySellerAndCustomerProfile(seller, profile).orElse(null);
        if (mapping == null) {
            mapping = new SellerCustomerMapping(); mapping.setSeller(seller); mapping.setCustomerProfile(profile); mapping.setFirstPurchaseAt(at); mapping.setLastPurchaseAt(at);
        } else if (at != null && (mapping.getLastPurchaseAt() == null || at.isAfter(mapping.getLastPurchaseAt()))) mapping.setLastPurchaseAt(at);
        sellerCustomers.save(mapping);
    }

    /** Collapses historic duplicate sync rows without deleting any original record. */
    private List<CustomerProfile> uniqueCustomerProfiles() {
        Map<String, CustomerProfile> result = new LinkedHashMap<>();
        for (CustomerProfile profile : customerProfiles.findAll()) {
            String key = customerIdentity(profile);
            // A POS invoice can legally contain only a name.  Its profile ID
            // is still a durable identity and must be visible to an admin.
            if (key == null && profile.getId() != null) key = "PROFILE:" + profile.getId();
            if (key == null) continue;
            CustomerProfile previous = result.get(key);
            if (previous == null || (profile.getId() != null && profile.getId() < previous.getId())) result.put(key, profile);
        }
        return new ArrayList<>(result.values());
    }

    private String customerIdentity(CustomerProfile profile) {
        if (profile == null) return null;
        if (profile.getOnlineUser() != null && profile.getOnlineUser().getId() != null) return "USER:" + profile.getOnlineUser().getId();
        if (!blank(profile.getMobile())) return "MOBILE:" + normalizeContact(profile.getMobile());
        if (!blank(profile.getEmail())) return "EMAIL:" + profile.getEmail().trim().toLowerCase(Locale.ROOT);
        return null;
    }

    private User seller(Long sellerId) {
        User seller = users.findById(sellerId).orElseThrow(() -> new RuntimeException("Seller not found"));
        if (seller.getRole() != Role.SELLER) throw new IllegalArgumentException("Selected user is not a seller");
        return seller;
    }

    private BigDecimal pendingDistributorDue(User seller) {
        return purchases.findBySellerOrderByPurchaseDateDesc(seller).stream().map(purchase -> {
            BigDecimal paid = purchasePayments.findByPurchaseOrderByPaymentDateDesc(purchase).stream().map(PurchasePayment::getAmount).map(this::money).reduce(BigDecimal.ZERO, BigDecimal::add);
            return nonNegative(money(purchase.getGrandTotal()).subtract(paid));
        }).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal estimateProfit(User seller) {
        BigDecimal revenue = sellerSummary(seller).totalRevenue();
        BigDecimal purchaseCost = purchases.findBySellerOrderByPurchaseDateDesc(seller).stream().map(Purchase::getGrandTotal).map(this::money).reduce(BigDecimal.ZERO, BigDecimal::add);
        return revenue.subtract(purchaseCost);
    }

    private String businessName(User seller, Shop shop) { return !blank(seller.getBusinessName()) ? seller.getBusinessName() : shop == null ? seller.getName() : shop.getName(); }
    private String businessAddress(User seller, Shop shop) { return !blank(seller.getBusinessAddress()) ? seller.getBusinessAddress() : shop == null ? null : shop.getAddress(); }
    private String city(User seller, Shop shop) { return !blank(seller.getBusinessCity()) ? seller.getBusinessCity() : shop == null ? null : shop.getCity(); }
    private String contactKey(OfflineBill bill) { return !blank(bill.getCustomerMobile()) ? normalizeContact(bill.getCustomerMobile()) : bill.getCustomerEmail(); }
    private boolean inside(LocalDateTime value, LocalDate from, LocalDate to) { return value != null && (from == null || !value.toLocalDate().isBefore(from)) && (to == null || !value.toLocalDate().isAfter(to)); }
    private boolean contains(String search, String... values) { return blank(search) || Arrays.stream(values).filter(Objects::nonNull).anyMatch(value -> value.toLowerCase(Locale.ROOT).contains(search.trim().toLowerCase(Locale.ROOT))); }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String normalizeContact(String value) { return blank(value) ? null : value.replaceAll("[^0-9+]", ""); }
    private BigDecimal money(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private BigDecimal nonNegative(BigDecimal value) { return value.signum() < 0 ? BigDecimal.ZERO : value; }
    private String paymentStatus(OfflineBill bill) { BigDecimal total = money(bill.getGrandTotal()), paid = money(bill.getPaymentAmount()); return paid.compareTo(total) >= 0 ? "PAID" : paid.signum() > 0 ? "PARTIAL" : "PENDING"; }
    private Map<String, Object> row(Object... pairs) { Map<String, Object> row = new LinkedHashMap<>(); for (int index = 0; index < pairs.length; index += 2) row.put((String) pairs[index], pairs[index + 1]); return row; }
    private <T> AdminPageResponse<T> page(List<T> list, int requestedPage, int requestedSize) { int size = Math.max(1, Math.min(requestedSize <= 0 ? 20 : requestedSize, 100)); int page = Math.max(0, requestedPage); int from = Math.min(page * size, list.size()); int to = Math.min(from + size, list.size()); return new AdminPageResponse<>(list.subList(from, to), list.size(), (int) Math.ceil(list.size() / (double) size), page, size); }
}
