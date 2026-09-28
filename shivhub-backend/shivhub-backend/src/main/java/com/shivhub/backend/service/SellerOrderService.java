package com.shivhub.backend.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.shivhub.backend.dto.OrderItemSerialSelectionRequest;
import com.shivhub.backend.dto.OrderDeliveryExpectationRequest;
import com.shivhub.backend.dto.OrderDeliveryExpectationResponse;
import com.shivhub.backend.dto.SellerOrderResponse;
import com.shivhub.backend.entity.Order;
import com.shivhub.backend.entity.OrderItem;
import com.shivhub.backend.entity.PurchaseItemSerial;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.enums.OrderStatus;
import com.shivhub.backend.enums.PaymentStatus;
import com.shivhub.backend.repository.OrderRepository;
import com.shivhub.backend.repository.OrderItemRepository;
import com.shivhub.backend.repository.PurchaseItemSerialRepository;
import com.shivhub.backend.repository.UserRepository;

/** Read-only seller order queue built from immutable seller snapshots on order items. */
@Service
public class SellerOrderService {
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final PurchaseItemSerialRepository serialRepository;
    @org.springframework.beans.factory.annotation.Autowired
    private OrderDeliveryExpectationService deliveryExpectations;

    public SellerOrderService(
            OrderItemRepository orderItemRepository,
            UserRepository userRepository,
            OrderRepository orderRepository,
            OrderService orderService,
            PurchaseItemSerialRepository serialRepository) {
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.serialRepository = serialRepository;
    }
    @Transactional(readOnly = true)
    public List<SellerOrderResponse> getMyOrders(String email) {
        User seller = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Seller not found"));
        if (seller.getRole() != Role.SELLER || !seller.isEnabled()) throw new RuntimeException("Only active sellers can view this order queue");
        var grouped = new LinkedHashMap<Long, List<OrderItem>>();
        for (OrderItem item : orderItemRepository.findSellerOrderItems(seller.getId()))
            grouped.computeIfAbsent(item.getOrder().getId(), ignored -> new java.util.ArrayList<>()).add(item);
        return grouped.values().stream().map(items -> {
            Order order = items.get(0).getOrder();
            User customer = order.getCustomerId() == null
                    ? null
                    : userRepository.findById(order.getCustomerId()).orElse(null);
            BigDecimal total = items.stream().map(OrderItem::getTotalPrice).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            List<SellerOrderResponse.Item> responseItems = items.stream()
                    .map(item -> {
                        List<SellerOrderResponse.SerialOption> selectedSerials =
                                serialRepository.findAssignedForOrderItem(item.getId())
                                        .stream()
                                        .map(this::serialOption)
                                        .toList();

                        List<SellerOrderResponse.SerialOption> availableSerials =
                                serialRepository.findAvailableBySellerAndProduct(seller.getId(), item.getProductId())
                                        .stream()
                                        .map(this::serialOption)
                                        .toList();

                        return new SellerOrderResponse.Item(
                                item.getId(),
                                item.getProductId(),
                                item.getProductName(),
                                item.getQuantity(),
                                item.getUnitPrice(),
                                item.getTotalPrice(),
                                item.getAssignedSerialSummary(),
                                availableSerials,
                                selectedSerials
                        );
                    })
                    .toList();

            OrderDeliveryExpectationResponse expectation = deliveryExpectations.forSeller(order.getId(), seller.getId());
            return new SellerOrderResponse(
                    order.getId(),
                    order.getOrderNumber(),
                    order.getCustomerId(),
                    customer == null ? null : customer.getName(),
                    customer == null ? null : customer.getEmail(),
                    customer == null ? null : customer.getMobile(),
                    order.getShippingAddress(),
                    order.getOrderStatus(),
                    order.getSellerRequestedStatus(),
                    order.getPaymentStatus(),
                    order.getCreatedAt(),
                    items.size(),
                    total,
                    order.getDeliveryPersonName(),
                    order.getDeliveryPersonMobile(),
                    expectation == null ? null : expectation.expectedDeliveryAt(),
                    expectation == null ? null : expectation.customerMessage(),
                    expectation == null ? null : expectation.updatedAt(),
                    order.getOrderStatus() != OrderStatus.CANCELLED,
                    responseItems
            );
        }).toList();
    }

    @Transactional
    public SellerOrderResponse updateDeliveryExpectation(String email, Long orderId, OrderDeliveryExpectationRequest request) {
        deliveryExpectations.updateForSeller(email, orderId, request);
        return getMyOrders(email).stream().filter(order -> order.id().equals(orderId)).findFirst()
                .orElseThrow(() -> new RuntimeException("Delivery time updated but order could not be reloaded"));
    }
    @Transactional
    public void requestStatusChange(String email, Long orderId, OrderStatus requestedStatus, String deliveryPersonName, String deliveryPersonMobile, List<OrderItemSerialSelectionRequest> serialSelections) {
        User seller = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Seller not found"));
        if (seller.getRole() != Role.SELLER || !seller.isEnabled()) throw new RuntimeException("Only active sellers can request a status change");
        if (requestedStatus == null || requestedStatus == OrderStatus.PENDING || requestedStatus == OrderStatus.CANCELLED) throw new RuntimeException("Choose a valid delivery status");
        if (requestedStatus == OrderStatus.OUT_FOR_DELIVERY && (deliveryPersonName == null || deliveryPersonName.isBlank() || deliveryPersonMobile == null || !deliveryPersonMobile.matches("^[6-9][0-9]{9}$"))) throw new RuntimeException("Delivery person name and valid 10-digit mobile are required for Out for Delivery");
        boolean ownsOrder = orderItemRepository.findSellerOrderItems(seller.getId()).stream().anyMatch(item -> item.getOrder().getId().equals(orderId));
        if (!ownsOrder) throw new RuntimeException("This order is not assigned to your shop");
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        if (order.getOrderStatus() == OrderStatus.DELIVERED || order.getOrderStatus() == OrderStatus.CANCELLED) throw new RuntimeException("Finalized orders cannot be changed");
        if (serialSelections != null) {
            for (OrderItemSerialSelectionRequest selection : serialSelections) {
                boolean ownsItem = order.getItems().stream()
                        .anyMatch(item -> item.getId().equals(selection.getOrderItemId())
                                && seller.getId().equals(item.getSellerId()));
                if (!ownsItem) throw new RuntimeException("IMEI/serial selection must belong to your order item");
            }
        }
        orderService.reserveOrderSerials(orderId, serialSelections);
        order.setSellerRequestedStatus(requestedStatus); order.setSellerStatusRequestedAt(java.time.LocalDateTime.now());
        if (requestedStatus == OrderStatus.OUT_FOR_DELIVERY) { order.setDeliveryPersonName(deliveryPersonName.trim()); order.setDeliveryPersonMobile(deliveryPersonMobile.trim()); }
        orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public byte[] generateSellerInvoice(String email, Long orderId) {
        User seller = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Seller not found"));
        if (seller.getRole() != Role.SELLER || !seller.isEnabled()) {
            throw new RuntimeException("Only active sellers can download this invoice");
        }
        boolean ownsOrder = orderItemRepository.findSellerOrderItems(seller.getId()).stream()
                .anyMatch(item -> item.getOrder().getId().equals(orderId));
        if (!ownsOrder) {
            throw new RuntimeException("This order is not assigned to your shop");
        }
        return orderService.generateInvoice(orderId);
    }

    @Transactional
    public SellerOrderResponse updatePaymentStatus(
            String email,
            Long orderId,
            PaymentStatus paymentStatus) {

        if (paymentStatus == null) {
            throw new RuntimeException("Payment status is required");
        }

        if (paymentStatus == PaymentStatus.FAILED) {
            throw new RuntimeException("Seller can mark only received/refunded payment status");
        }

        User seller = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        if (seller.getRole() != Role.SELLER || !seller.isEnabled()) {
            throw new RuntimeException("Only active sellers can update payment status");
        }

        boolean ownsOrder = orderItemRepository.findSellerOrderItems(seller.getId()).stream()
                .anyMatch(item -> item.getOrder().getId().equals(orderId));

        if (!ownsOrder) {
            throw new RuntimeException("This order is not assigned to your shop");
        }

        if (paymentStatus == PaymentStatus.REFUNDED) {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));
            if (order.getOrderStatus() != OrderStatus.CANCELLED) {
                throw new RuntimeException("Refunded payment status is allowed only for cancelled orders");
            }
        }

        orderService.updatePaymentStatus(orderId, paymentStatus);

        return getMyOrders(email).stream()
                .filter(order -> order.id().equals(orderId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Order updated but could not reload seller view"));
    }

    private SellerOrderResponse.SerialOption serialOption(PurchaseItemSerial serial) {
        return new SellerOrderResponse.SerialOption(
                serial.getId(),
                serial.getImei1(),
                serial.getImei2(),
                serial.getSerialNumber(),
                serialDisplay(serial),
                serial.getStatus()
        );
    }

    private String serialDisplay(PurchaseItemSerial serial) {
        if (serial.getImei1() != null && !serial.getImei1().isBlank()) return serial.getImei1();
        if (serial.getSerialNumber() != null && !serial.getSerialNumber().isBlank()) return serial.getSerialNumber();
        if (serial.getImei2() != null && !serial.getImei2().isBlank()) return serial.getImei2();
        return "#" + serial.getId();
    }
}
