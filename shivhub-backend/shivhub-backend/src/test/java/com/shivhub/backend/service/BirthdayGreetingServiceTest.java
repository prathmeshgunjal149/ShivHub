package com.shivhub.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.shivhub.backend.entity.BirthdayGreetingSetting;
import com.shivhub.backend.entity.CustomerProfile;
import com.shivhub.backend.entity.MarketingCampaign;
import com.shivhub.backend.entity.MarketingDelivery;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.repository.BirthdayGreetingSettingRepository;
import com.shivhub.backend.repository.CustomerProfileRepository;
import com.shivhub.backend.repository.MarketingCampaignRepository;
import com.shivhub.backend.repository.MarketingDeliveryRepository;
import com.shivhub.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class BirthdayGreetingServiceTest {
    @Mock private BirthdayGreetingSettingRepository settings;
    @Mock private CustomerProfileRepository profiles;
    @Mock private MarketingCampaignRepository campaigns;
    @Mock private MarketingDeliveryRepository deliveries;
    @Mock private CampaignService campaignService;
    @Mock private UserRepository users;

    @Test
    void queuesSellerAndOnlineCustomerBirthdaysOnceEach() {
        BirthdayGreetingSetting setting = new BirthdayGreetingSetting();
        setting.setActive(true); setting.setEmailSubject("Happy birthday"); setting.setMessageContent("Have a great day");
        when(settings.findFirstByActiveTrueOrderByIdDesc()).thenReturn(Optional.of(setting));
        when(campaigns.findAll()).thenReturn(List.of());
        when(campaigns.save(any(MarketingCampaign.class))).thenAnswer(call -> {
            MarketingCampaign campaign = call.getArgument(0);
            if (campaign.getId() == null) campaign.setId(88L);
            return campaign;
        });
        CustomerProfile sellerCustomer = new CustomerProfile();
        sellerCustomer.setId(11L); sellerCustomer.setName("Seller buyer"); sellerCustomer.setEmail("seller-buyer@example.com"); sellerCustomer.setDateOfBirth(LocalDate.now()); sellerCustomer.setCommunicationConsent(true);
        when(profiles.findAll()).thenReturn(List.of(sellerCustomer));

        User shivhubCustomer = new User();
        shivhubCustomer.setId(12L); shivhubCustomer.setRole(Role.CUSTOMER); shivhubCustomer.setEnabled(true); shivhubCustomer.setName("ShivHub buyer"); shivhubCustomer.setEmail("online-buyer@example.com"); shivhubCustomer.setDateOfBirth(LocalDate.now());
        when(users.findByRole(Role.CUSTOMER)).thenReturn(List.of(shivhubCustomer));
        when(profiles.save(any(CustomerProfile.class))).thenAnswer(call -> {
            CustomerProfile profile = call.getArgument(0);
            if (profile.getId() == null) profile.setId(12L);
            return profile;
        });
        when(deliveries.findByCampaignIdOrderByCreatedAtDesc(88L)).thenReturn(List.of());
        when(deliveries.existsByCampaignIdAndCustomerProfileIdAndChannel(any(), any(), any())).thenReturn(false);
        when(deliveries.save(any(MarketingDelivery.class))).thenAnswer(call -> {
            MarketingDelivery delivery = call.getArgument(0);
            if (delivery.getId() == null) delivery.setId(delivery.getCustomerProfileId());
            return delivery;
        });

        BirthdayGreetingService service = new BirthdayGreetingService(settings, profiles, campaigns, deliveries, campaignService, users);
        Map<String, Object> result = service.sendToday();
        assertEquals(2, result.get("targeted"));
        assertEquals(2, result.get("queued"));

        ArgumentCaptor<Long> deliveryIds = ArgumentCaptor.forClass(Long.class);
        verify(campaignService, times(2)).dispatchDeliveryAfterCommit(deliveryIds.capture());
        assertEquals(List.of(11L, 12L), deliveryIds.getAllValues());
    }
}
