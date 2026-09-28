package com.shivhub.backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shivhub.backend.dto.DeliveryEstimateResponse;
import com.shivhub.backend.dto.OrderDeliveryExpectationRequest;
import com.shivhub.backend.dto.OrderDeliveryExpectationResponse;
import com.shivhub.backend.entity.Order;
import com.shivhub.backend.entity.OrderDeliveryExpectation;
import com.shivhub.backend.entity.OrderItem;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.OrderStatus;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.repository.OrderDeliveryExpectationRepository;
import com.shivhub.backend.repository.OrderRepository;
import com.shivhub.backend.repository.UserRepository;

/** Owns seller-isolated online-order ETA promises and their customer-safe view. */
@Service
public class OrderDeliveryExpectationService {
    private static final Logger log = LoggerFactory.getLogger(OrderDeliveryExpectationService.class);
    private final OrderDeliveryExpectationRepository expectations;
    private final OrderRepository orders;
    private final UserRepository users;
    private final EmailService emailService;

    public OrderDeliveryExpectationService(OrderDeliveryExpectationRepository expectations, OrderRepository orders,
            UserRepository users, EmailService emailService) {
        this.expectations = expectations;
        this.orders = orders;
        this.users = users;
        this.emailService = emailService;
    }

    @Transactional
    public void initializeFromDistanceRules(Order order, Map<Long, DeliveryEstimateResponse> estimates) {
        if (order == null || order.getId() == null || estimates == null) return;
        LocalDateTime now = LocalDateTime.now();
        estimates.forEach((sellerId, estimate) -> {
            if (sellerId == null || sellerId <= 0 || estimate == null || estimate.estimatedMinutes() == null
                    || estimate.estimatedMinutes() < 1 || Boolean.FALSE.equals(estimate.serviceAvailable())) return;
            if (expectations.findByOrderIdAndSellerId(order.getId(), sellerId).isPresent()) return;
            OrderDeliveryExpectation expectation = new OrderDeliveryExpectation();
            expectation.setOrderId(order.getId());
            expectation.setSellerId(sellerId);
            expectation.setSellerName(sellerName(order, sellerId));
            expectation.setExpectedDeliveryAt(now.plusMinutes(estimate.estimatedMinutes()));
            expectation.setCustomerMessage(clean("Estimated from the selected delivery address."));
            expectation.setUpdatedByRole("SYSTEM");
            expectation.setUpdatedAt(now);
            expectations.save(expectation);
        });
    }

    @Transactional(readOnly = true)
    public List<OrderDeliveryExpectationResponse> forOrder(Long orderId) {
        return expectations.findByOrderIdOrderByExpectedDeliveryAtAsc(orderId).stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public OrderDeliveryExpectationResponse forSeller(Long orderId, Long sellerId) {
        return expectations.findByOrderIdAndSellerId(orderId, sellerId).map(this::response).orElse(null);
    }

    @Transactional
    public OrderDeliveryExpectationResponse updateForSeller(String email, Long orderId, OrderDeliveryExpectationRequest request) {
        User seller = users.findByEmailIgnoreCase(email)
                .filter(user -> user.getRole() == Role.SELLER && user.isEnabled())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Seller access is required"));
        Order order = order(orderId);
        if (!hasSellerItems(order, seller.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This order is not assigned to your shop");
        }
        return update(order, seller.getId(), sellerName(order, seller.getId()), request, "SELLER");
    }

    @Transactional
    public OrderDeliveryExpectationResponse updateForAdmin(String email, Long orderId, Long sellerId, OrderDeliveryExpectationRequest request) {
        users.findByEmailIgnoreCase(email).filter(user -> user.getRole() == Role.ADMIN && user.isEnabled())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access is required"));
        Order order = order(orderId);
        if (sellerId == null || !hasSellerItems(order, sellerId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select a seller assigned to this order");
        }
        return update(order, sellerId, sellerName(order, sellerId), request, "ADMIN");
    }

    private OrderDeliveryExpectationResponse update(Order order, Long sellerId, String sellerName,
            OrderDeliveryExpectationRequest request, String updatedByRole) {
        if (request == null || request.expectedDeliveryAt() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expected delivery date and time are required");
        }
        if (order.getOrderStatus() == OrderStatus.CANCELLED || order.getOrderStatus() == OrderStatus.DELIVERED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Finalized orders cannot have their delivery time changed");
        }
        if (request.expectedDeliveryAt().isBefore(LocalDateTime.now().minusMinutes(1))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expected delivery time cannot be in the past");
        }
        if (request.expectedDeliveryAt().isAfter(LocalDateTime.now().plusDays(365))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expected delivery time must be within one year");
        }
        OrderDeliveryExpectation expectation = expectations.findByOrderIdAndSellerId(order.getId(), sellerId)
                .orElseGet(OrderDeliveryExpectation::new);
        expectation.setOrderId(order.getId());
        expectation.setSellerId(sellerId);
        expectation.setSellerName(sellerName);
        expectation.setExpectedDeliveryAt(request.expectedDeliveryAt());
        expectation.setCustomerMessage(clean(request.customerMessage()));
        expectation.setUpdatedByRole(updatedByRole);
        expectation.setUpdatedAt(LocalDateTime.now());
        OrderDeliveryExpectation saved = expectations.save(expectation);
        notifyCustomerAboutDeliveryPromise(order, saved);
        return response(saved);
    }

    private Order order(Long id) {
        return orders.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    private boolean hasSellerItems(Order order, Long sellerId) {
        return order.getItems().stream().anyMatch(item -> Objects.equals(item.getSellerId(), sellerId));
    }

    private String sellerName(Order order, Long sellerId) {
        return order.getItems().stream().filter(item -> Objects.equals(item.getSellerId(), sellerId))
                .map(OrderItem::getSellerName).filter(name -> name != null && !name.isBlank()).findFirst().orElse("Seller");
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** Email failures are deliberately isolated from the saved delivery commitment. */
    private void notifyCustomerAboutDeliveryPromise(Order order, OrderDeliveryExpectation expectation) {
        if (order.getCustomerId() == null) return;
        users.findById(order.getCustomerId()).ifPresent(customer -> {
            if (customer.getEmail() == null || customer.getEmail().isBlank()) return;
            try {
                emailService.sendOrderDeliveryPromiseEmail(customer.getEmail(), customer.getName(), order.getOrderNumber(),
                        expectation.getExpectedDeliveryAt(), expectation.getCustomerMessage());
            } catch (Exception exception) {
                log.warn("Delivery promise email failed for order {}", order.getOrderNumber());
            }
        });
    }

    private OrderDeliveryExpectationResponse response(OrderDeliveryExpectation expectation) {
        return new OrderDeliveryExpectationResponse(expectation.getSellerId(), expectation.getSellerName(),
                expectation.getExpectedDeliveryAt(), expectation.getCustomerMessage(), expectation.getUpdatedByRole(), expectation.getUpdatedAt());
    }
}
