package com.shivhub.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shivhub.backend.dto.SellerProductSalesRow;
import com.shivhub.backend.entity.Product;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.repository.OfflineBillItemRepository;
import com.shivhub.backend.repository.OrderItemRepository;
import com.shivhub.backend.repository.ProductRepository;
import com.shivhub.backend.repository.UserRepository;

/** Product-wise totals for a seller. Category is taken only from that seller's product catalogue. */
@Service
public class SellerProductSalesReportService {
    private final UserRepository users; private final ProductRepository products;
    private final OrderItemRepository onlineItems; private final OfflineBillItemRepository posItems;
    public SellerProductSalesReportService(UserRepository users, ProductRepository products, OrderItemRepository onlineItems, OfflineBillItemRepository posItems) {
        this.users = users; this.products = products; this.onlineItems = onlineItems; this.posItems = posItems;
    }
    @Transactional(readOnly = true)
    public List<SellerProductSalesRow> get(String email, LocalDate startDate, LocalDate endDate) {
        User seller = users.findByEmail(email).orElseThrow(() -> new RuntimeException("Seller not found"));
        if (!seller.isEnabled() || seller.getRole() == null || !"SELLER".equals(seller.getRole().name())) throw new RuntimeException("Only active sellers can access reports");
        LocalDate start = startDate == null ? LocalDate.now().withDayOfMonth(1) : startDate;
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        if (end.isBefore(start)) throw new RuntimeException("End date cannot be before start date.");
        Map<Long, Product> catalogue = new HashMap<>(); for (Product product : products.findBySeller(seller)) catalogue.put(product.getId(), product);
        Map<Long, Totals> totals = new HashMap<>(); LocalDateTime from = start.atStartOfDay(), until = end.plusDays(1).atStartOfDay();
        onlineItems.findSellerReportItemsBetween(seller.getId(), from, until).forEach(item -> totals.computeIfAbsent(item.getProductId(), id -> new Totals(item.getProductName())).online(item.getQuantity(), item.getTotalPrice()));
        posItems.findSellerReportItemsBetween(seller.getId(), from, until).forEach(item -> totals.computeIfAbsent(item.getProductId(), id -> new Totals(item.getProductName())).pos(item.getQuantity(), item.getTotalPrice()));
        return totals.entrySet().stream().map(entry -> { Product product = catalogue.get(entry.getKey()); Totals value = entry.getValue(); String category = product == null ? null : product.getCategoryEntity() == null ? product.getCategory() : product.getCategoryEntity().getName(); return new SellerProductSalesRow(entry.getKey(), value.name, category, value.online, value.pos, value.online + value.pos, value.sales); }).sorted((a,b) -> b.totalSales().compareTo(a.totalSales())).toList();
    }
    private static class Totals { String name; long online; long pos; BigDecimal sales = BigDecimal.ZERO; Totals(String name) { this.name=name; } void online(Integer q, BigDecimal amount) { online += q == null ? 0 : q; sales=sales.add(amount == null ? BigDecimal.ZERO : amount); } void pos(Integer q, BigDecimal amount) { pos += q == null ? 0 : q; sales=sales.add(amount == null ? BigDecimal.ZERO : amount); } }
}
