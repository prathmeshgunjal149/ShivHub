package com.shivhub.backend.security;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.shivhub.backend.service.SellerEntitlementService;
import com.shivhub.backend.service.SubscriptionRequiredException;

/** API-level protection. UI locks are only a convenience; this guard stops direct API calls too. */
@Component
public class SellerSubscriptionAccessFilter extends OncePerRequestFilter {
    private final SellerEntitlementService entitlements;
    public SellerSubscriptionAccessFilter(SellerEntitlementService entitlements) { this.entitlements = entitlements; }

    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return HttpMethod.OPTIONS.matches(request.getMethod()) || request.getRequestURI().startsWith("/api/seller/subscription");
    }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getAuthorities().stream().noneMatch(value -> "ROLE_SELLER".equals(value.getAuthority()))) { chain.doFilter(request, response); return; }
        String feature = requiredFeature(request.getMethod(), request.getRequestURI());
        if (feature == null) { chain.doFilter(request, response); return; }
        try { entitlements.requireFeature(authentication.getName(), feature); chain.doFilter(request, response); }
        catch (SubscriptionRequiredException error) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN); response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"SUBSCRIPTION_REQUIRED\",\"message\":\"This feature requires an active ShivHub subscription.\",\"subscriptionStatus\":\"" + error.getStatus().name() + "\",\"renewalRequired\":true}");
        }
    }

    private String requiredFeature(String method, String path) {
        if (path.startsWith("/api/finance")) return SellerEntitlementService.OFFLINE_POS;
        boolean read = HttpMethod.GET.matches(method) || HttpMethod.HEAD.matches(method);
        // POS must be entirely unavailable after expiry, including barcode scans and bill creation.
        if (path.startsWith("/api/offline-billing") || path.startsWith("/api/offline-bills/imei") || path.startsWith("/api/seller/offline-billing")) return SellerEntitlementService.OFFLINE_POS;
        // Independent non-GST counter tools use the seller's POS entitlement,
        // but intentionally do not post to stock, purchase or GST services.
        if (path.startsWith("/api/seller/shop-register") || path.startsWith("/api/seller/instant-bills")) return SellerEntitlementService.OFFLINE_POS;
        // A registered customer is created or refreshed as part of POS billing.
        // It must use the same entitlement as the bill itself; otherwise an
        // active seller is incorrectly blocked by the generic fallback below.
        if (path.startsWith("/api/seller/customers/register-or-update")) return SellerEntitlementService.OFFLINE_POS;
        // Existing invoice/history reads remain available; only new bill payment actions are blocked.
        if (path.startsWith("/api/seller/customers/email-campaign") || path.startsWith("/api/seller/customers/whatsapp-offer")) return SellerEntitlementService.MARKETING;
        if (path.startsWith("/api/seller/customers")) return read ? null : SellerEntitlementService.CUSTOMER_MANAGEMENT;
        if (path.startsWith("/api/seller/payment-reminders")) return read ? null : SellerEntitlementService.RECEIVABLE_MANAGEMENT;
        if (path.startsWith("/api/seller/offline-bills") && !read) return SellerEntitlementService.OFFLINE_POS;
        // This is the narrow expired-seller exception: existing online orders can still be fulfilled.
        if (path.startsWith("/api/seller/orders")) return SellerEntitlementService.ONLINE_ORDER_FULFILMENT_LIMITED;
        if (path.startsWith("/api/seller/non-mobile-products") || path.startsWith("/api/product-images")) return read ? null : SellerEntitlementService.PRODUCT_MANAGEMENT;
        if (path.equals("/api/products") || path.startsWith("/api/products/")) return read ? null : SellerEntitlementService.PRODUCT_MANAGEMENT;
        if (path.startsWith("/api/purchases") || path.startsWith("/api/purchase-payments")) return read ? null : SellerEntitlementService.PURCHASE_MANAGEMENT;
        if (path.startsWith("/api/seller/products")) return read ? null : SellerEntitlementService.PRODUCT_MANAGEMENT;
        if (path.startsWith("/api/seller/distributor-requests") || path.startsWith("/api/seller-distributors") || path.startsWith("/api/distributor-credit-notes") || path.startsWith("/api/seller/distributor-payments")) return read ? null : SellerEntitlementService.DISTRIBUTOR_MANAGEMENT;
        if (path.startsWith("/api/customer-receivables")) return read ? null : SellerEntitlementService.RECEIVABLE_MANAGEMENT;
        if (path.startsWith("/api/seller/expenses")) return read ? null : SellerEntitlementService.EXPENSE_MANAGEMENT;
        if (path.startsWith("/api/seller/staff") || path.startsWith("/api/seller/salespeople") || path.startsWith("/api/seller/shops")) return read ? null : SellerEntitlementService.STAFF_MANAGEMENT;
        // Seller after-sales status/inspection/estimate actions are part of the
        // paid POS workflow. GET remains available for history; mutations must
        // use the same active subscription entitlement as billing.
        if (path.startsWith("/api/seller/after-sales")) return read ? null : SellerEntitlementService.OFFLINE_POS;
        if (path.startsWith("/api/ca-reports")) return SellerEntitlementService.CA_REPORTS;
        if (path.startsWith("/api/seller/reports")) return SellerEntitlementService.ADVANCED_REPORTS;
        // Seller profile/dashboard/settings and the subscription endpoints are system-required.
        if (path.startsWith("/api/stock-transfers")) return read ? null : SellerEntitlementService.INVENTORY_MANAGEMENT;
        if (path.startsWith("/api/accounting")) return SellerEntitlementService.CA_REPORTS;
        if (path.startsWith("/api/seller/gst-reports")) return SellerEntitlementService.GST_REPORTS;
        if (path.startsWith("/api/referrals")) return SellerEntitlementService.REFERRAL_MANAGEMENT;
        if (path.startsWith("/api/seller/profile") || path.startsWith("/api/seller/dashboard")) return null;
        // New seller mutation endpoints are locked until a feature is explicitly classified and mapped.
        if (path.startsWith("/api/seller/") && !read) return "UNCONFIGURED_SELLER_FEATURE";
        return null;
    }
}
