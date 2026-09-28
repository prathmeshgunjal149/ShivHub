package com.shivhub.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.shivhub.backend.dto.BillingScanResponse;
import com.shivhub.backend.entity.Product;
import com.shivhub.backend.entity.Purchase;
import com.shivhub.backend.entity.PurchaseItem;
import com.shivhub.backend.entity.PurchaseItemSerial;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.BillingScanType;
import com.shivhub.backend.enums.ProductStatus;
import com.shivhub.backend.enums.PurchaseStatus;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.repository.ProductRepository;
import com.shivhub.backend.repository.PurchaseItemSerialRepository;
import com.shivhub.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class BillingScanServiceTest {
    @Mock private UserRepository users;
    @Mock private ProductRepository products;
    @Mock private PurchaseItemSerialRepository serials;
    private BillingScanService service;
    private User seller;

    @BeforeEach
    void setUp() {
        service = new BillingScanService(users, products, serials);
        seller = new User();
        seller.setId(10L);
        seller.setEmail("seller@shivhub.test");
        seller.setRole(Role.SELLER);
        seller.setEnabled(true);
        when(users.findByEmail(seller.getEmail())).thenReturn(Optional.of(seller));
    }

    @Test
    void availableImeiResolvesToExactlyOneTrackedUnit() {
        Product product = sellableProduct(25L, true);
        PurchaseItemSerial serial = serial(product, "123456789012345", "123456789012346", null, "AVAILABLE");
        when(serials.findByExactScanCode("123456789012345")).thenReturn(List.of(serial));

        BillingScanResponse response = service.scanProductForBilling(seller.getEmail(), " 123456789012345 ");

        assertEquals(BillingScanType.IMEI, response.getScanType());
        assertEquals(25L, response.getProductId());
        assertEquals(serial.getId(), response.getSerialId());
        assertEquals(1, response.getStockAvailable());
    }

    @Test
    void soldImeiCannotBeScannedAgain() {
        Product product = sellableProduct(25L, true);
        PurchaseItemSerial serial = serial(product, "123456789012345", null, null, "SOLD");
        when(serials.findByExactScanCode("123456789012345")).thenReturn(List.of(serial));

        BillingScanException exception = assertThrows(BillingScanException.class,
                () -> service.scanProductForBilling(seller.getEmail(), "123456789012345"));

        assertEquals("This IMEI has already been sold.", exception.getMessage());
    }

    @Test
    void accessoryBarcodeUsesTheNormalQuantityProductFlow() {
        Product product = sellableProduct(50L, false);
        product.setBarcode("8901234567890");
        when(serials.findByExactScanCode("8901234567890")).thenReturn(List.of());
        when(products.findFirstBySellerAndBarcodeIgnoreCase(seller, "8901234567890")).thenReturn(Optional.of(product));
        when(serials.findAvailableBySellerAndProduct(seller.getId(), product.getId())).thenReturn(List.of());

        BillingScanResponse response = service.scanProductForBilling(seller.getEmail(), "8901234567890");

        assertEquals(BillingScanType.BARCODE, response.getScanType());
        assertEquals(50L, response.getProductId());
        assertEquals(false, response.isImeiTracked());
    }

    private Product sellableProduct(Long id, boolean serialTracking) {
        Product product = new Product();
        product.setId(id); product.setName("Test product"); product.setSeller(seller); product.setActive(true);
        product.setApprovalStatus(ProductStatus.APPROVED); product.setStock(1); product.setReservedStock(0);
        product.setPrice(new BigDecimal("100.00")); product.setGstRate(new BigDecimal("18.00"));
        product.setSerialTrackingRequired(serialTracking); product.setCategory(serialTracking ? "Mobiles" : "Accessories");
        return product;
    }

    private PurchaseItemSerial serial(Product product, String imei1, String imei2, String serialNumber, String status) {
        Purchase purchase = new Purchase(); purchase.setSeller(seller); purchase.setStatus(PurchaseStatus.COMPLETED);
        PurchaseItem item = new PurchaseItem(); item.setProduct(product); item.setPurchase(purchase);
        PurchaseItemSerial serial = new PurchaseItemSerial(); serial.setId(101L); serial.setPurchaseItem(item);
        serial.setImei1(imei1); serial.setImei2(imei2); serial.setSerialNumber(serialNumber); serial.setStatus(status);
        return serial;
    }
}
