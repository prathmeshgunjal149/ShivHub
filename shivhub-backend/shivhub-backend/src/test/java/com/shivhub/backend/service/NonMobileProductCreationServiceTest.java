package com.shivhub.backend.service;

import java.math.BigDecimal;
import java.util.List;
import com.shivhub.backend.dto.*;
import com.shivhub.backend.entity.*;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NonMobileProductCreationServiceTest {
    final ProductService products = mock(ProductService.class);
    final PurchaseService purchases = mock(PurchaseService.class);
    final UserRepository users = mock(UserRepository.class);
    final CategoryRepository categories = mock(CategoryRepository.class);
    final PurchaseItemSerialRepository serials = mock(PurchaseItemSerialRepository.class);
    final NonMobileProductCreationService service = new NonMobileProductCreationService(products, purchases, users, categories, serials);

    InitialProductPurchaseRequest initial(List<PurchaseItemSerialRequest> rows) {
        return new InitialProductPurchaseRequest(1L, "REAL-INVOICE", null, new BigDecimal("1000"), new BigDecimal("18"), rows);
    }
    @Test void acceptsSerialOnlyStockAndTrimsIdentifiers() {
        var unit = new PurchaseItemSerialRequest(null, null, " TV-UNIT-001 ");
        service.validateInitialPurchase(initial(List.of(unit)), 1);
        assertEquals("TV-UNIT-001", unit.getSerialNumber());
    }
    @Test void requiresOneIdentifierPerPhysicalUnit() {
        assertThrows(ResponseStatusException.class, () -> service.validateInitialPurchase(initial(List.of()), 1));
        assertThrows(ResponseStatusException.class, () -> service.validateInitialPurchase(initial(List.of(new PurchaseItemSerialRequest())), 1));
    }
    @Test void rejectsCrossFieldDuplicatesAndInvalidImei() {
        var a = new PurchaseItemSerialRequest("123456789012345", null, null);
        var b = new PurchaseItemSerialRequest(null, null, "123456789012345");
        assertThrows(ResponseStatusException.class, () -> service.validateInitialPurchase(initial(List.of(a, b)), 2));
        assertThrows(ResponseStatusException.class, () -> service.validateInitialPurchase(initial(List.of(new PurchaseItemSerialRequest("123", null, null))), 1));
    }
    @Test void rejectsPreviouslyRegisteredIdentifierWithoutExposingOwner() {
        when(serials.findByExactScanCode("TV-001")).thenReturn(List.of(new PurchaseItemSerial()));
        var error = assertThrows(ResponseStatusException.class, () -> service.validateInitialPurchase(initial(List.of(new PurchaseItemSerialRequest(null, null, "TV-001"))), 1));
        assertEquals(409, error.getStatusCode().value());
    }
    @Test void refusesMobileCreationOnExtensionEndpoint() {
        User seller = new User(); seller.setRole(Role.SELLER); seller.setEnabled(true);
        Category mobile = new Category(); mobile.setName("Mobiles");
        when(users.findByEmail("seller@example.com")).thenReturn(java.util.Optional.of(seller));
        when(categories.findById(1L)).thenReturn(java.util.Optional.of(mobile));
        var request = new CreateProductRequest(); request.setCategoryId(1L);
        assertThrows(ResponseStatusException.class, () -> service.create("seller@example.com", request, null, List.of()));
        verifyNoInteractions(products, purchases);
    }
}
