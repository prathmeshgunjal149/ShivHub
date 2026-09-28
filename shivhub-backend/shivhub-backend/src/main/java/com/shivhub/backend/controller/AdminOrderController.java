package com.shivhub.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.shivhub.backend.dto.OrderResponse;
import com.shivhub.backend.dto.OrderDeliveryExpectationRequest;
import com.shivhub.backend.dto.OrderDeliveryExpectationResponse;
import com.shivhub.backend.dto.UpdateOrderStatusRequest;
import com.shivhub.backend.enums.OrderStatus;
import com.shivhub.backend.enums.PaymentStatus;
import com.shivhub.backend.service.OrderService;
import com.shivhub.backend.service.OrderDeliveryExpectationService;

import jakarta.validation.Valid;


/*
 * =========================================================
 * AdminOrderController
 * =========================================================
 *
 * Admin-only Order APIs.
 *
 * Handles:
 *
 * 1. View all orders
 * 2. View order details
 * 3. Filter orders by status
 * 4. Update order status
 * 5. Get order count
 * 6. Get order count by status
 *
 * =========================================================
 */

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {


    private final OrderService orderService;
    private final OrderDeliveryExpectationService deliveryExpectations;


    /*
     * =========================================================
     * CONSTRUCTOR
     * =========================================================
     */

    public AdminOrderController(
            OrderService orderService, OrderDeliveryExpectationService deliveryExpectations) {

        this.orderService =
                orderService;
        this.deliveryExpectations = deliveryExpectations;
    }


    /*
     * =========================================================
     * GET ALL ORDERS
     * =========================================================
     *
     * GET:
     *
     * /api/admin/orders
     *
     * ADMIN ONLY
     *
     * =========================================================
     */

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponse>>
            getAllOrders() {


        List<OrderResponse> orders =
                orderService.getAllOrders();


        return ResponseEntity.ok(
                orders
        );
    }


    /*
     * =========================================================
     * GET ORDER DETAILS
     * =========================================================
     *
     * GET:
     *
     * /api/admin/orders/{id}
     *
     * =========================================================
     */

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse>
            getOrderById(

                    @PathVariable Long id) {


        OrderResponse order =
                orderService.getOrderById(
                        id
                );


        return ResponseEntity.ok(
                order
        );
    }


    /*
     * =========================================================
     * GET ORDERS BY STATUS
     * =========================================================
     *
     * GET:
     *
     * /api/admin/orders/status/PENDING
     *
     * /api/admin/orders/status/CONFIRMED
     *
     * /api/admin/orders/status/PROCESSING
     *
     * /api/admin/orders/status/SHIPPED
     *
     * /api/admin/orders/status/DELIVERED
     *
     * /api/admin/orders/status/CANCELLED
     *
     * =========================================================
     */

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponse>>
            getOrdersByStatus(

                    @PathVariable OrderStatus status) {


        List<OrderResponse> orders =
                orderService.getOrdersByStatus(
                        status
                );


        return ResponseEntity.ok(
                orders
        );
    }


    /*
     * =========================================================
     * UPDATE ORDER STATUS
     * =========================================================
     *
     * PUT:
     *
     * /api/admin/orders/{id}/status
     *
     * Request:
     *
     * {
     *     "orderStatus": "CONFIRMED"
     * }
     *
     * =========================================================
     */

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse>
            updateOrderStatus(

                    @PathVariable Long id,

                    @Valid
                    @RequestBody
                    UpdateOrderStatusRequest request) {


        /*
         * =====================================================
         * VALIDATE REQUEST
         * =====================================================
         */

        if (request == null ||
                request.getOrderStatus() == null) {

            return ResponseEntity
                    .badRequest()
                    .build();
        }


        /*
         * =====================================================
         * UPDATE STATUS
         * =====================================================
         */

        OrderResponse response =
                orderService.updateOrderStatus(

                        id,

                        request.getOrderStatus(),

                        request.getSerialSelections()
                );


        return ResponseEntity.ok(
                response
        );
    }

    @PutMapping("/{orderId}/delivery-expectations/{sellerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderDeliveryExpectationResponse> updateDeliveryExpectation(
            @PathVariable Long orderId, @PathVariable Long sellerId,
            @Valid @RequestBody OrderDeliveryExpectationRequest request,
            org.springframework.security.core.Authentication authentication) {
        if (authentication == null || authentication.getName() == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(deliveryExpectations.updateForAdmin(authentication.getName(), orderId, sellerId, request));
    }

    @PutMapping("/{id}/approve-seller-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> approveSellerStatus(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.approveSellerStatusRequest(id));
    }

    @PutMapping("/{id}/payment-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> updatePaymentStatus(@PathVariable Long id, @RequestBody PaymentUpdateRequest request) {
        if (request == null || request.getPaymentStatus() == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(orderService.updatePaymentStatus(id, request.getPaymentStatus()));
    }

    @GetMapping("/{id}/invoice")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long id) {
        OrderResponse order = orderService.getOrderById(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=ShivHub-Invoice-" + order.getOrderNumber() + ".pdf")
                .body(orderService.generateInvoice(id));
    }

    public static class PaymentUpdateRequest {
        private PaymentStatus paymentStatus;
        public PaymentStatus getPaymentStatus() { return paymentStatus; }
        public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
    }


    /*
     * =========================================================
     * GET TOTAL ORDER COUNT
     * =========================================================
     *
     * GET:
     *
     * /api/admin/orders/count
     *
     * =========================================================
     */

    @GetMapping("/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long>
            getOrderCount() {


        long count =
                orderService.getOrderCount();


        return ResponseEntity.ok(
                count
        );
    }


    /*
     * =========================================================
     * GET ORDER COUNT BY STATUS
     * =========================================================
     *
     * GET:
     *
     * /api/admin/orders/count/PENDING
     *
     * /api/admin/orders/count/CANCELLED
     *
     * =========================================================
     */

    @GetMapping("/count/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long>
            getOrderCountByStatus(

                    @PathVariable OrderStatus status) {


        long count =
                orderService.getOrderCountByStatus(
                        status
                );


        return ResponseEntity.ok(
                count
        );
    }
}
