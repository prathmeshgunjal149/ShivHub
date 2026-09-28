package com.shivhub.backend.service;

import java.util.List;
import java.util.Optional;
import com.shivhub.backend.dto.VariantReturnDispositionRequest;
import com.shivhub.backend.entity.*;
import com.shivhub.backend.enums.*;
import com.shivhub.backend.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VariantReturnServiceTest {
    final UserRepository users = mock(UserRepository.class);
    final ServiceRequestRepository requests = mock(ServiceRequestRepository.class);
    final OrderItemRepository orders = mock(OrderItemRepository.class);
    final OfflineBillItemRepository bills = mock(OfflineBillItemRepository.class);
    final ProductRepository products = mock(ProductRepository.class);
    final ProductVariantRepository variants = mock(ProductVariantRepository.class);
    final AfterSalesStockMovementRepository ledger = mock(AfterSalesStockMovementRepository.class);
    final ServiceStatusHistoryRepository history = mock(ServiceStatusHistoryRepository.class);
    final EntityManager em = mock(EntityManager.class);
    final StockMovementService movements = mock(StockMovementService.class);
    final VariantReturnService service = new VariantReturnService(users, requests, orders, bills, products, variants, ledger, history, em, movements);
    final User seller = new User(); final ServiceRequest request = new ServiceRequest();
    final Product product = new Product(); final ProductVariant medium = new ProductVariant(); final ProductVariant large = new ProductVariant();
    VariantReturnServiceTest() {
        seller.setId(1L); seller.setRole(Role.SELLER);
        request.setId(7L); request.setProductId(2L); request.setSellerId(1L); request.setOrderItemId(3L);
        request.setRequestType(ServiceRequestType.RETURN); request.setStatus(ServiceRequestStatus.INSPECTION_PASSED);
        request.setInspectionFinding(AfterSalesInspectionFinding.NO_FAULT_FOUND);
        when(users.findByEmailIgnoreCase("seller@example.com")).thenReturn(Optional.of(seller));
        when(requests.findLockedById(7L)).thenReturn(Optional.of(request));
        OrderItem item = new OrderItem(); item.setProductId(2L); item.setSellerId(1L); item.setProductVariantId(4L); item.setQuantity(2);
        when(orders.findById(3L)).thenReturn(Optional.of(item));
        product.setId(2L); product.setSeller(seller); product.setVariantsEnabled(true); product.setStock(10);
        medium.setId(4L); medium.setStockQuantity(3); medium.setActive(true);
        large.setId(5L); large.setStockQuantity(7); large.setActive(true);
        when(products.findById(2L)).thenReturn(Optional.of(product));
        when(variants.findLocked(4L, 2L)).thenReturn(Optional.of(medium));
        when(variants.findByProductOrderByIdAsc(product)).thenReturn(List.of(medium, large));
        when(movements.recordMovement(any(), any(), anyString(), anyInt(), anyInt(), anyInt(), anyString(), anyLong(), anyString())).thenReturn(new StockMovement());
    }
    VariantReturnDispositionRequest input(VariantReturnDisposition disposition, int quantity) {
        var r = new VariantReturnDispositionRequest(); r.setDisposition(disposition); r.setQuantity(quantity); return r;
    }
    @Test void restoresOnlyInspectedOriginalVariant() {
        var result = service.dispose("seller@example.com", 7L, input(VariantReturnDisposition.AVAILABLE, 1));
        assertTrue(result.isStockRestored()); assertEquals(4, medium.getStockQuantity()); assertEquals(7, large.getStockQuantity());
        assertEquals(11, product.getStock()); assertEquals(4L, request.getProductVariantId()); verify(history).save(any());
    }
    @Test void defectiveReturnNeverAddsSellableStock() {
        var result = service.dispose("seller@example.com", 7L, input(VariantReturnDisposition.DEFECTIVE, 1));
        assertFalse(result.isStockRestored()); assertEquals(3, medium.getStockQuantity()); assertEquals(10, product.getStock());
        verifyNoInteractions(movements); verify(ledger).save(any());
    }
    @Test void repeatedDispositionCannotRestoreTwice() {
        when(ledger.existsByServiceRequestIdAndProductVariantId(7L, 4L)).thenReturn(true);
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> service.dispose("seller@example.com", 7L, input(VariantReturnDisposition.AVAILABLE, 1))).getStatusCode().value());
        verifyNoInteractions(movements); assertEquals(3, medium.getStockQuantity());
    }
    @Test void uninspectedAndOverReturnedStockAreRejected() {
        request.setStatus(ServiceRequestStatus.PRODUCT_RECEIVED);
        assertThrows(ResponseStatusException.class, () -> service.dispose("seller@example.com", 7L, input(VariantReturnDisposition.AVAILABLE, 1)));
        request.setStatus(ServiceRequestStatus.INSPECTION_PASSED);
        when(ledger.disposedQuantity(4L, 3L, null)).thenReturn(2L);
        assertThrows(ResponseStatusException.class, () -> service.dispose("seller@example.com", 7L, input(VariantReturnDisposition.AVAILABLE, 1)));
        verifyNoInteractions(movements);
    }
    @Test void anotherSellerCannotAccessReturn() {
        seller.setId(99L);
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> service.info("seller@example.com", 7L)).getStatusCode().value());
        verifyNoInteractions(orders, bills, products, variants, ledger);
    }
    @Test void offlineReturnResolvesVariantFromOriginalBillNotFrontend() {
        request.setOrderItemId(null); request.setOfflineBillItemId(8L);
        OfflineBill bill = new OfflineBill(); bill.setSellerId(1L);
        OfflineBillItem item = new OfflineBillItem(); item.setOfflineBill(bill); item.setProductId(2L); item.setProductVariantId(4L); item.setQuantity(1);
        when(bills.findById(8L)).thenReturn(Optional.of(item));
        var result = service.dispose("seller@example.com", 7L, input(VariantReturnDisposition.AVAILABLE, 1));
        assertEquals(4L, result.getVariantId()); assertEquals(4, medium.getStockQuantity()); verifyNoInteractions(orders);
    }
}
