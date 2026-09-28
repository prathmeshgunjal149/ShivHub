package com.shivhub.backend.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.shivhub.backend.dto.UserResponse;
import com.shivhub.backend.dto.AdminCustomerSummaryResponse;
import com.shivhub.backend.dto.AdminPageResponse;
import com.shivhub.backend.dto.AdminSellerSummaryResponse;
import com.shivhub.backend.enums.ProductStatus;
import com.shivhub.backend.service.AdminManagementService;
import com.shivhub.backend.service.AdminService;


/*
 * =========================================================
 * AdminController
 * =========================================================
 *
 * Handles Admin-only operations.
 *
 * SELLER:
 *
 * 1. View pending sellers
 * 2. Approve seller
 * 3. Reject seller
 *
 *
 * CUSTOMER:
 *
 * 4. View all customers
 * 5. View customer by ID
 * 6. Block customer
 * 7. Unblock customer
 *
 * =========================================================
 */

@RestController
@RequestMapping("/api/admin")
public class AdminController {


    private final AdminService adminService;
    private final AdminManagementService managementService;


    /*
     * =========================================================
     * CONSTRUCTOR
     * =========================================================
     */

    public AdminController(
            AdminService adminService,
            AdminManagementService managementService) {

        this.adminService = adminService;
        this.managementService = managementService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        return ResponseEntity.ok(adminService.getDashboardSummary());
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
     *
     * GET:
     *
     * /api/admin/sellers/pending
     *
     * =========================================================
     */

    @GetMapping("/sellers/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>>
            getPendingSellers() {


        List<UserResponse> sellers =
                adminService.getPendingSellers();


        return ResponseEntity.ok(sellers);
    }

    @GetMapping("/sellers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminPageResponse<AdminSellerSummaryResponse>> getSellers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String approvalStatus,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) LocalDate registeredFrom,
            @RequestParam(required = false) LocalDate registeredTo,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean gstVerified,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(managementService.sellers(search, approvalStatus, city, registeredFrom, registeredTo, active, gstVerified, page, size));
    }

    @GetMapping("/sellers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSellerById(@PathVariable Long id) {
        return ResponseEntity.ok(managementService.sellerDetails(id));
    }

    @GetMapping("/sellers/{id}/customers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminPageResponse<AdminCustomerSummaryResponse>> getSellerCustomers(
            @PathVariable Long id, @RequestParam(required = false) String search,
            @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) BigDecimal minimumAmount, @RequestParam(required = false) BigDecimal maximumAmount,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(managementService.sellerCustomers(id, search, from, to, minimumAmount, maximumAmount, page, size));
    }

    @GetMapping("/sellers/{id}/sales")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getSellerSales(
            @PathVariable Long id, @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to) {
        return ResponseEntity.ok(managementService.sellerSales(id, from, to));
    }

    @GetMapping(value = "/sellers/{sellerId}/sales/OFFLINE_BILL/{billId}/invoice", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadSellerOfflineInvoice(@PathVariable Long sellerId, @PathVariable Long billId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=shivhub-offline-bill-" + billId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(managementService.sellerOfflineInvoice(sellerId, billId));
    }

    @GetMapping("/sellers/{id}/payments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSellerPayments(@PathVariable Long id) {
        return ResponseEntity.ok(managementService.sellerPayments(id));
    }

    @PutMapping("/sellers/{id}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> suspendSeller(@PathVariable Long id) {
        managementService.suspendSeller(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/sellers/{sellerId}/products/{productId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateSellerProductStatus(
            @PathVariable Long sellerId, @PathVariable Long productId, @RequestBody ProductReviewRequest request) {
        managementService.setSellerProductStatus(sellerId, productId, ProductStatus.valueOf(request.status.trim().toUpperCase()));
        return ResponseEntity.noContent().build();
    }

    public static class ProductReviewRequest { public String status; }


    /*
     * =========================================================
     * APPROVE SELLER
     * =========================================================
     *
     * PUT:
     *
     * /api/admin/sellers/{id}/approve
     *
     * =========================================================
     */

    @PutMapping("/sellers/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse>
            approveSeller(
                    @PathVariable Long id) {


        UserResponse seller =
                adminService.approveSeller(id);


        return ResponseEntity.ok(seller);
    }


    /*
     * =========================================================
     * REJECT SELLER
     * =========================================================
     *
     * PUT:
     *
     * /api/admin/sellers/{id}/reject
     *
     * =========================================================
     */

    @PutMapping("/sellers/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse>
            rejectSeller(
                    @PathVariable Long id,
                    @RequestBody SellerReviewRequest request) {


        UserResponse seller =
                adminService.rejectSeller(id, request == null ? null : request.reason);


        return ResponseEntity.ok(seller);
    }

    public static class SellerReviewRequest {
        public String reason;
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
     * Only ADMIN can access.
     *
     * =========================================================
     */

    @GetMapping("/customers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminPageResponse<AdminCustomerSummaryResponse>> getCustomers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long sellerId,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) BigDecimal minimumAmount,
            @RequestParam(required = false) BigDecimal maximumAmount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(managementService.customers(search, type, sellerId, city, from, to, minimumAmount, maximumAmount, page, size));
    }

    @GetMapping("/customers/counts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> customerCounts() { return ResponseEntity.ok(managementService.customerCounts()); }


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

    @GetMapping("/customers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>>
            getCustomerById(
                    @PathVariable Long id) {
        return ResponseEntity.ok(managementService.customerDetails(id));
    }

    @GetMapping("/customers/{id}/details")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>>
            getCustomerDetails(
                    @PathVariable Long id) {

        return ResponseEntity.ok(managementService.customerDetails(id));
    }


    /*
     * =========================================================
     * BLOCK CUSTOMER
     * =========================================================
     *
     * PUT:
     *
     * /api/admin/customers/{id}/block
     *
     * =========================================================
     */

    @PutMapping("/customers/{id}/block")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse>
            blockCustomer(
                    @PathVariable Long id) {


        UserResponse customer =
                adminService.blockCustomer(id);


        return ResponseEntity.ok(customer);
    }


    /*
     * =========================================================
     * UNBLOCK CUSTOMER
     * =========================================================
     *
     * PUT:
     *
     * /api/admin/customers/{id}/unblock
     *
     * =========================================================
     */

    @PutMapping("/customers/{id}/unblock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse>
            unblockCustomer(
                    @PathVariable Long id) {


        UserResponse customer =
                adminService.unblockCustomer(id);


        return ResponseEntity.ok(customer);
    }

}
