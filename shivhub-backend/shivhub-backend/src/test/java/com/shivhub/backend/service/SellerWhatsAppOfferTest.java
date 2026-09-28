package com.shivhub.backend.service;

import com.shivhub.backend.dto.SellerWhatsAppOfferRequest;
import com.shivhub.backend.entity.*;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.repository.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SellerWhatsAppOfferTest {
    final UserRepository users = mock(UserRepository.class);
    final OfflineBillRepository bills = mock(OfflineBillRepository.class);
    final SellerCustomerMappingRepository mappings = mock(SellerCustomerMappingRepository.class);
    final WhatsAppNotificationService whatsapp = mock(WhatsAppNotificationService.class);
    final SellerCustomerService service = new SellerCustomerService(bills, users, mock(EmailService.class),
            whatsapp, mock(CustomerProfileRepository.class), mappings, mock(LoyaltyService.class), mock(CustomerReceivableService.class));
    final User seller = new User();
    SellerWhatsAppOfferTest() {
        seller.setId(7L); seller.setName("Shop"); seller.setRole(Role.SELLER); seller.setEnabled(true);
        when(users.findByEmail("seller@example.com")).thenReturn(Optional.of(seller));
        when(whatsapp.imageOffersConfigured()).thenReturn(true);
        when(bills.findBySellerIdOrderByCreatedAtDesc(7L)).thenReturn(List.of());
        ReflectionTestUtils.setField(service, "publicApiUrl", "https://shop.example.com");
    }
    SellerCustomerMapping customer(String mobile, boolean consent) {
        CustomerProfile p = new CustomerProfile(); p.setName("Customer"); p.setMobile(mobile); p.setWhatsappConsent(consent);
        SellerCustomerMapping m = new SellerCustomerMapping(); m.setSeller(seller); m.setCustomerProfile(p); return m;
    }
    @Test void allCustomersDeduplicatesAndSkipsNonConsentingWithoutRequiringEmail() {
        var first = customer("9876543210", true);
        when(mappings.findBySeller(seller)).thenReturn(List.of(first, first, customer("9876543211", false)));
        when(whatsapp.sendOptedInMobileImageOffer(anyString(), anyString(), anyString(), anyList(), anyString())).thenReturn(true);
        var result = service.sendWhatsAppOffer("seller@example.com", new SellerWhatsAppOfferRequest(true, null, "Offer", "Sale", "/uploads/campaigns/offer.png"));
        assertEquals(1, result.accepted()); assertEquals(1, result.skipped()); assertEquals(0, result.failed());
        verify(whatsapp).sendOptedInMobileImageOffer(eq("9876543210"), eq("Customer"), eq("seller:7:Offer"),
                eq(List.of("Shop", "Offer", "Sale")), eq("https://shop.example.com/uploads/campaigns/offer.png"));
    }
    @Test void cannotSendToAnotherSellersCustomer() {
        when(mappings.findBySeller(seller)).thenReturn(List.of(customer("9876543210", true)));
        var result = service.sendWhatsAppOffer("seller@example.com", new SellerWhatsAppOfferRequest(false, List.of("9876543212"), "Offer", "Sale", "https://cdn.example.com/image.png"));
        assertEquals(0, result.accepted()); assertEquals(1, result.skipped());
        verify(whatsapp, never()).sendOptedInMobileImageOffer(anyString(), anyString(), anyString(), anyList(), anyString());
    }
    @Test void unavailableProviderAndLocalImageAreRejected() {
        var request = new SellerWhatsAppOfferRequest(true, null, "Offer", "Sale", "http://localhost/image.png");
        assertThrows(IllegalArgumentException.class, () -> service.sendWhatsAppOffer("seller@example.com", request));
        when(whatsapp.imageOffersConfigured()).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> service.sendWhatsAppOffer("seller@example.com", request));
    }
    @Test void providerFailuresAreNotCountedAsAccepted() {
        when(mappings.findBySeller(seller)).thenReturn(List.of(customer("9876543210", true)));
        var result = service.sendWhatsAppOffer("seller@example.com", new SellerWhatsAppOfferRequest(true, null, "Offer", "Sale", "https://cdn.example.com/image.png"));
        assertEquals(0, result.accepted()); assertEquals(1, result.failed());
    }
}
