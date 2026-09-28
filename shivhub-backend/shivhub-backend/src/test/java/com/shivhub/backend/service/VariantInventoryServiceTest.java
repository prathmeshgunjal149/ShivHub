package com.shivhub.backend.service;

import java.util.List;
import java.util.Optional;
import com.shivhub.backend.entity.*;
import com.shivhub.backend.repository.ProductVariantRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VariantInventoryServiceTest {
    final ProductVariantRepository variants = mock(ProductVariantRepository.class);
    final EntityManager em = mock(EntityManager.class);
    final StockMovementService movements = mock(StockMovementService.class);
    final VariantInventoryService service = new VariantInventoryService(variants, em, movements);
    final Product product = new Product();
    final ProductVariant medium = new ProductVariant();
    final ProductVariant large = new ProductVariant();
    VariantInventoryServiceTest() {
        product.setId(1L); product.setVariantsEnabled(true); product.setStock(12);
        medium.setId(10L); medium.setProduct(product); medium.setStockQuantity(5); medium.setActive(true);
        large.setId(11L); large.setProduct(product); large.setStockQuantity(7); large.setActive(true);
        when(variants.findLocked(10L, 1L)).thenReturn(Optional.of(medium));
        when(variants.findByProductOrderByIdAsc(product)).thenReturn(List.of(medium, large));
        when(movements.recordMovement(any(), any(), anyString(), anyInt(), anyInt(), anyInt(), anyString(), anyLong(), anyString())).thenReturn(new StockMovement());
    }
    @Test void saleDeductsOnlySelectedSizeAndLocksParentFirst() {
        service.sell(product, 10L, 1, false, "ORDER", 99L);
        assertEquals(4, medium.getStockQuantity()); assertEquals(7, large.getStockQuantity()); assertEquals(11, product.getStock());
        var order = inOrder(em, variants); order.verify(em).flush();
        order.verify(em).refresh(product, LockModeType.PESSIMISTIC_WRITE); order.verify(variants).findLocked(10L, 1L);
    }
    @Test void reservationReleaseDoesNotDeductPhysicalStock() {
        service.reserve(product, 10L, 2); assertEquals(3, medium.getAvailableStock()); assertEquals(5, medium.getStockQuantity());
        service.release(product, 10L, 2); assertEquals(5, medium.getAvailableStock()); assertEquals(0, product.getReservedStock());
    }
    @Test void rejectsOversellingAndWrongProductCombination() {
        assertThrows(ResponseStatusException.class, () -> service.sell(product, 10L, 6, false, "ORDER", 99L));
        assertEquals(5, medium.getStockQuantity()); verifyNoInteractions(movements);
        assertThrows(ResponseStatusException.class, () -> service.reserve(product, 11L, 1));
    }
    @Test void rejectsMissingOptionsAndProductsWithoutVariants() {
        assertThrows(ResponseStatusException.class, () -> service.reserve(product, null, 1));
        product.setVariantsEnabled(false);
        assertThrows(ResponseStatusException.class, () -> service.reserve(product, 10L, 1));
        verify(variants, never()).findLocked(anyLong(), anyLong());
    }
    @Test void mobileVariantReservationTracksSelectedCombination() {
        // Mobile dispatch separately requires assigned IMEIs in OrderService.
        product.setCategory("Mobiles");
        service.reserve(product, 10L, 2);
        assertEquals(2, medium.getReservedQuantity());
        assertEquals(0, large.getReservedQuantity());
        assertEquals(2, product.getReservedStock());
        assertEquals(12, product.getStock());
        service.sell(product, 10L, 2, true, "ONLINE_VARIANT_ORDER_ITEM", 99L);
        assertEquals(3, medium.getStockQuantity());
        assertEquals(7, large.getStockQuantity());
        assertEquals(10, product.getStock());
        assertEquals(0, product.getReservedStock());
    }
}
