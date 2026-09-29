package com.shivhub.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shivhub.backend.entity.WhatsAppCampaignMapping;
import com.shivhub.backend.entity.WhatsAppDeliveryLog;
import com.shivhub.backend.repository.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WhatsAppCampaignParameterTest {
    final WhatsAppCampaignMappingRepository mappings = mock(WhatsAppCampaignMappingRepository.class);
    final WhatsAppDeliveryLogRepository logs = mock(WhatsAppDeliveryLogRepository.class);
    final AiSensyWhatsAppProvider provider = mock(AiSensyWhatsAppProvider.class);
    final MockEnvironment environment = new MockEnvironment();
    final WhatsAppNotificationService service = new WhatsAppNotificationService(
            mock(UserRepository.class), logs, mappings, new ObjectMapper(), false,
            "", "", "", "", "", "en_US", "aisensy", provider, environment);

    WhatsAppCampaignParameterTest() {
        when(provider.isConfigured()).thenReturn(true);
        when(logs.save(any())).thenAnswer(call -> call.getArgument(0));
        when(provider.send(anyString(), anyString(), anyString(), anyList(), anyList(), isNull()))
                .thenReturn(AiSensyWhatsAppProvider.ProviderResult.accepted("ACCEPTED"));
    }
    void mapping(String event, Integer count, boolean enabled) {
        var mapping = new WhatsAppCampaignMapping();
        mapping.setEventKey(event); mapping.setCampaignName("Exact Live Campaign");
        mapping.setTemplateParameterCount(count); mapping.setEnabled(enabled);
        when(mappings.findByEventKey(event)).thenReturn(Optional.of(mapping));
    }
    boolean send(WhatsAppNotificationEvent event, List<String> parameters) {
        return service.sendOptedInMobileEvent("9876543210", event, "Customer", "invoice-1", parameters);
    }
    @Test void trimsOnlyTrailingBodyValues() {
        mapping("payment-success", 2, true);
        assertTrue(send(WhatsAppNotificationEvent.PAYMENT_SUCCESS, List.of("invoice", "amount", "extra")));
        verify(provider).send(eq("Exact Live Campaign"), anyString(), anyString(),
                eq(List.of("invoice", "amount")), eq(List.of()), isNull());
    }
    @Test void zeroBodyCountPreservesOtpButton() {
        mapping("otp-login", 0, true);
        assertTrue(send(WhatsAppNotificationEvent.OTP_LOGIN, List.of("123456")));
        verify(provider).send(anyString(), anyString(), anyString(), eq(List.of()), eq(List.of("123456")), isNull());
    }
    @Test void unspecifiedCountPreservesExistingValues() {
        mapping("payment-success", null, true);
        assertTrue(send(WhatsAppNotificationEvent.PAYMENT_SUCCESS, List.of("invoice", "amount")));
        verify(provider).send(anyString(), anyString(), anyString(), eq(List.of("invoice", "amount")), eq(List.of()), isNull());
    }
    @Test void insufficientValuesSkipWithoutCallingProvider() {
        mapping("payment-success", 3, true);
        assertFalse(send(WhatsAppNotificationEvent.PAYMENT_SUCCESS, List.of("invoice")));
        verify(provider, never()).send(anyString(), anyString(), anyString(), anyList(), anyList(), any());
        verify(logs, atLeastOnce()).save(argThat(log -> "SKIPPED".equals(log.getStatus())
                && log.getFailureReason().contains("supplied only 1")));
    }
    @Test void missingPaymentMappingCannotReuseSentLogOrInvoiceCampaign() {
        environment.setProperty("shivhub.whatsapp.aisensy.campaigns.invoice-created", "Invoice campaign");
        var prior = new WhatsAppDeliveryLog(); prior.setStatus("SENT");
        when(logs.findTopByRecipientAndEventKeyOrderByCreatedAtDesc(anyString(), anyString())).thenReturn(Optional.of(prior));
        assertFalse(send(WhatsAppNotificationEvent.PAYMENT_SUCCESS, List.of("invoice")));
        verify(provider, never()).send(anyString(), anyString(), anyString(), anyList(), anyList(), any());
        assertEquals("SKIPPED", prior.getStatus());
    }
    @Test void disabledAdminMappingOverridesEnvironmentCampaign() {
        mapping("payment-success", 1, false);
        environment.setProperty("shivhub.whatsapp.aisensy.campaigns.payment-success", "Environment campaign");
        assertFalse(send(WhatsAppNotificationEvent.PAYMENT_SUCCESS, List.of("invoice")));
        verify(provider, never()).send(anyString(), anyString(), anyString(), anyList(), anyList(), any());
    }
    @Test void providerFailureIsNeverSent() {
        mapping("payment-success", 1, true);
        when(provider.send(anyString(), anyString(), anyString(), anyList(), anyList(), isNull()))
                .thenReturn(AiSensyWhatsAppProvider.ProviderResult.failed("Rejected"));
        assertFalse(send(WhatsAppNotificationEvent.PAYMENT_SUCCESS, List.of("invoice")));
        verify(logs, atLeastOnce()).save(argThat(log -> "FAILED".equals(log.getStatus())));
    }
}
