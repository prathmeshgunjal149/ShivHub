package com.shivhub.backend.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.shivhub.backend.dto.CreateOrderRequest;
import com.shivhub.backend.dto.OrderResponse;
import com.shivhub.backend.service.OrderService;

import jakarta.validation.Valid;


/*
 * =========================================================
 * OrderController
 * =========================================================
 *
 * Customer Order APIs
 *
 * Handles:
 *
 * 1. Create Order
 * 2. Get My Orders
 * 3. Get Customer Orders
 * 4. Get Order Details
 * 5. Cancel Order
 *
 * =========================================================
 */

@RestController
@RequestMapping("/api/orders")
public class OrderController {


    private final OrderService orderService;


    /*
     * =========================================================
     * CONSTRUCTOR
     * =========================================================
     */

    public OrderController(
            OrderService orderService) {

        this.orderService = orderService;
    }


    /*
     * =========================================================
     * CREATE ORDER
     * =========================================================
     *
     * POST:
     *
     * /api/orders
     *
     * Customer is identified using JWT.
     *
     * Frontend does NOT send customerId.
     *
     * =========================================================
     */

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(

            @Valid
            @RequestBody
            CreateOrderRequest request,

            Authentication authentication) {


        /*
         * =====================================================
         * CHECK AUTHENTICATION
         * =====================================================
         */

        if (authentication == null ||
                authentication.getName() == null) {

            return ResponseEntity
                    .status(401)
                    .build();
        }


        /*
         * =====================================================
         * GET LOGGED-IN CUSTOMER EMAIL
         * =====================================================
         */

        String email =
                authentication.getName();


        /*
         * =====================================================
         * CREATE ORDER
         * =====================================================
         */

        OrderResponse response =
                orderService.createOrder(
                        request,
                        email
                );


        return ResponseEntity.ok(response);
    }


    /*
     * =========================================================
     * GET MY ORDERS
     * =========================================================
     *
     * GET:
     *
     * /api/orders/my
     *
     * Customer is identified using JWT.
     *
     * =========================================================
     */

    @GetMapping("/my")
    public ResponseEntity<List<OrderResponse>>
            getMyOrders(
                    Authentication authentication) {


        /*
         * =====================================================
         * CHECK AUTHENTICATION
         * =====================================================
         */

        if (authentication == null ||
                authentication.getName() == null) {

            return ResponseEntity
                    .status(401)
                    .build();
        }


        /*
         * =====================================================
         * GET EMAIL
         * =====================================================
         */

        String email =
                authentication.getName();


        /*
         * =====================================================
         * GET CUSTOMER ORDERS
         * =====================================================
         */

        List<OrderResponse> orders =
                orderService
                        .getCustomerOrdersByEmail(
                                email
                        );


        return ResponseEntity.ok(orders);
    }


    /*
     * =========================================================
     * GET CUSTOMER ORDERS BY CUSTOMER ID
     * =========================================================
     *
     * GET:
     *
     * /api/orders/customer/{customerId}
     *
     * Existing API kept for compatibility.
     *
     * =========================================================
     */

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderResponse>>
            getCustomerOrders(

                    @PathVariable Long customerId) {


        List<OrderResponse> orders =
                orderService.getCustomerOrders(
                        customerId
                );


        return ResponseEntity.ok(orders);
    }


    /*
     * =========================================================
     * GET ORDER DETAILS
     * =========================================================
     *
     * GET:
     *
     * /api/orders/{id}
     *
     * =========================================================
     */

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse>
            getOrderById(

                    @PathVariable Long id,

                    Authentication authentication) {

        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }


        OrderResponse order =
                orderService.getCustomerOrderById(id, authentication.getName());


        return ResponseEntity.ok(order);
    }

    /*
     * =========================================================
     * CUSTOMER INVOICE DOWNLOAD
     * =========================================================
     *
     * GET:
     *
     * /api/orders/{id}/invoice
     *
     * Customer can download only their own invoice.
     *
     * =========================================================
     */

    @GetMapping(value = "/{id}/invoice", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadMyInvoice(

            @PathVariable Long id,

            Authentication authentication) {

        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }

        OrderResponse order =
                orderService.getCustomerOrderById(id, authentication.getName());

        byte[] invoice =
                orderService.generateCustomerInvoice(id, authentication.getName());

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=ShivHub-Invoice-" + order.getOrderNumber() + ".pdf"
                )
                .contentType(MediaType.APPLICATION_PDF)
                .body(invoice);
    }


    /*
     * =========================================================
     * CANCEL ORDER
     * =========================================================
     *
     * POST:
     *
     * /api/orders/{id}/cancel
     *
     *
     * Customer sends:
     *
     * {
     *     "reason": "I ordered by mistake",
     *     "comment": "Optional comment"
     * }
     *
     *
     * Customer is identified using JWT.
     *
     * =========================================================
     */

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse>
            cancelOrder(

                    @PathVariable Long id,

                    @RequestBody
                    CancelOrderRequest request,

                    Authentication authentication) {


        /*
         * =====================================================
         * CHECK AUTHENTICATION
         * =====================================================
         */

        if (authentication == null ||
                authentication.getName() == null) {

            return ResponseEntity
                    .status(401)
                    .build();
        }


        /*
         * =====================================================
         * VALIDATE REQUEST
         * =====================================================
         */

        if (request == null ||
                request.getReason() == null ||
                request.getReason()
                        .trim()
                        .isEmpty()) {

            return ResponseEntity
                    .badRequest()
                    .build();
        }


        /*
         * =====================================================
         * GET CUSTOMER EMAIL
         * =====================================================
         */

        String email =
                authentication.getName();


        /*
         * =====================================================
         * CANCEL ORDER
         * =====================================================
         *
         * OrderService will verify:
         *
         * 1. Order exists
         * 2. Order belongs to customer
         * 3. Order is cancellable
         * 4. Cancellation reason is valid
         *
         * =====================================================
         */

        OrderResponse response =
                orderService.cancelOrder(

                        id,

                        email,

                        request.getReason(),

                        request.getComment()
                );


        return ResponseEntity.ok(response);
    }


    /*
     * =========================================================
     * CANCEL ORDER REQUEST DTO
     * =========================================================
     *
     * Used when customer cancels an order.
     *
     * =========================================================
     */

    public static class CancelOrderRequest {


        /*
         * =====================================================
         * CANCELLATION REASON
         * =====================================================
         *
         * Required.
         *
         * Examples:
         *
         * I ordered by mistake
         * Found a better price
         * Want to change the product
         * Delivery is taking too long
         *
         * =====================================================
         */

        private String reason;


        /*
         * =====================================================
         * OPTIONAL COMMENT
         * =====================================================
         */

        private String comment;


        /*
         * =====================================================
         * DEFAULT CONSTRUCTOR
         * =====================================================
         */

        public CancelOrderRequest() {
        }


        /*
         * =====================================================
         * GET REASON
         * =====================================================
         */

        public String getReason() {

            return reason;
        }


        /*
         * =====================================================
         * SET REASON
         * =====================================================
         */

        public void setReason(
                String reason) {

            this.reason = reason;
        }


        /*
         * =====================================================
         * GET COMMENT
         * =====================================================
         */

        public String getComment() {

            return comment;
        }


        /*
         * =====================================================
         * SET COMMENT
         * =====================================================
         */

        public void setComment(
                String comment) {

            this.comment = comment;
        }
    }
}
