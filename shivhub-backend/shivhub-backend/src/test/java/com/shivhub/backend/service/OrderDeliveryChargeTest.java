package com.shivhub.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.shivhub.backend.dto.CreateOrderRequest;
import com.shivhub.backend.dto.DeliveryEstimateResponse;
import com.shivhub.backend.dto.OrderItemRequest;
import com.shivhub.backend.entity.Category;
import com.shivhub.backend.entity.CustomerAddress;
import com.shivhub.backend.entity.Order;
import com.shivhub.backend.entity.Product;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.PaymentMethod;
import com.shivhub.backend.enums.ProductStatus;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.repository.CouponUsageRepository;
import com.shivhub.backend.repository.OrderItemRepository;
import com.shivhub.backend.repository.OrderRepository;
import com.shivhub.backend.repository.ProductRepository;
import com.shivhub.backend.repository.PurchaseItemSerialRepository;
import com.shivhub.backend.repository.UserRepository;

import static org.mockito.Mockito.mock;

class OrderDeliveryChargeTest {
    private final OrderRepository orders = mock(OrderRepository.class);
    private final OrderItemRepository orderItems = mock(OrderItemRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final PurchaseItemSerialRepository serials = mock(PurchaseItemSerialRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final CouponService coupons = mock(CouponService.class);
    private final CouponUsageRepository couponUsage = mock(CouponUsageRepository.class);
    private final ReferralService referrals = mock(ReferralService.class);
    private final CustomerAddressService addresses = mock(CustomerAddressService.class);
    private final LoyaltyService loyalty = mock(LoyaltyService.class);
    private final EmailService emails = mock(EmailService.class);
    private final InvoiceService invoices = mock(InvoiceService.class);
    private final DeliveryEstimateService delivery = mock(DeliveryEstimateService.class);
    private final OrderDeliveryExpectationService expectations = mock(OrderDeliveryExpectationService.class);

    @Test
    void preservesConfiguredMobileDeliveryChargeInTheFinalOrder() {
        OrderService service = new OrderService(orders, orderItems, products, serials, users, coupons,
                couponUsage, referrals, addresses, loyalty, emails, invoices);
        ReflectionTestUtils.setField(service, "deliveryEstimates", delivery);
        ReflectionTestUtils.setField(service, "deliveryExpectations", expectations);

        User customer = new User();
        customer.setId(1L); customer.setName("Customer"); customer.setEmail("customer@example.com");
        customer.setRole(Role.CUSTOMER); customer.setEnabled(true);
        User seller = new User(); seller.setId(2L); seller.setName("Seller"); seller.setBusinessName("Shiv Mobile");
        Category mobiles = new Category(); mobiles.setId(7L); mobiles.setName("Mobiles");
        Product mobile = new Product();
        mobile.setId(10L); mobile.setName("Mobile"); mobile.setCategoryEntity(mobiles); mobile.setSeller(seller);
        mobile.setActive(true); mobile.setApprovalStatus(ProductStatus.APPROVED); mobile.setPrice(new BigDecimal("1000"));
        mobile.setStock(1); mobile.setReservedStock(0);
        CustomerAddress address = address();

        when(users.findByEmail("customer@example.com")).thenReturn(Optional.of(customer));
        when(users.findById(1L)).thenReturn(Optional.of(customer));
        when(products.findById(10L)).thenReturn(Optional.of(mobile));
        when(addresses.resolveForOrder(customer, 5L, null)).thenReturn(address);
        when(delivery.forOrder(customer, mobile, 5L)).thenReturn(new DeliveryEstimateResponse(
                true, true, true, "Road distance", "Delivery within 1 hour", new BigDecimal("8.40"),
                "ROAD", true, "Shiv Mobile", 2L, 20L, 60, new BigDecimal("49.00")));
        when(orders.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(expectations.forOrder(org.mockito.ArgumentMatchers.anyLong())).thenReturn(List.of());

        CreateOrderRequest request = new CreateOrderRequest();
        request.setDeliveryAddressId(5L); request.setPaymentMethod(PaymentMethod.CASH);
        request.setItems(List.of(new OrderItemRequest(null, 10L, 1)));

        var response = service.createOrder(request, "customer@example.com");

        assertEquals(new BigDecimal("49.00"), response.getDeliveryCharge());
        assertEquals(new BigDecimal("1049.00"), response.getGrandTotal());
        assertEquals("Delivery within 1 hour", response.getItems().get(0).getDeliveryEstimateText());
    }

    @Test
    void hidesZeroDeliveryLineInInvoicePdf() {
        InvoiceService service = new InvoiceService(serials, products);
        Order order = new Order();
        order.setOrderNumber("SH-1001");
        order.setCustomerId(1L);
        order.setSubtotal(new BigDecimal("1499.00"));
        order.setDiscount(BigDecimal.ZERO);
        order.setTax(BigDecimal.ZERO);
        order.setDeliveryCharge(BigDecimal.ZERO);
        order.setGrandTotal(new BigDecimal("1499.00"));
        order.setPaymentStatus(com.shivhub.backend.enums.PaymentStatus.PENDING);
        order.setOrderStatus(com.shivhub.backend.enums.OrderStatus.PENDING);

        User customer = new User();
        customer.setName("Swapnil");
        customer.setEmail("swapnil@example.com");
        customer.setMobile("9876543210");

        byte[] pdfBytes = service.generateInvoicePdf(order, customer);
        String pdfText = new String(pdfBytes, StandardCharsets.ISO_8859_1);

        assertFalse(pdfText.contains("Delivery"));
    }

    private CustomerAddress address() {
        CustomerAddress address = new CustomerAddress();
        address.setId(5L); address.setRecipientName("Customer"); address.setMobileNumber("9876543210");
        address.setAddressLine1("1 Main Road"); address.setAddressLine2("Near Station");
        address.setCity("Pune"); address.setDistrict("Pune"); address.setState("Maharashtra"); address.setPincode("411001");
        return address;
    }
}
