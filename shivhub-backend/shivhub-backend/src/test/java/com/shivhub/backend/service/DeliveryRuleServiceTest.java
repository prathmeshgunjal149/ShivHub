package com.shivhub.backend.service;

import java.math.BigDecimal;
import java.util.List;
import com.shivhub.backend.entity.DeliveryDistanceRule;
import com.shivhub.backend.repository.DeliveryDistanceRuleRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeliveryRuleServiceTest {
    final DeliveryDistanceRuleRepository repository = mock(DeliveryDistanceRuleRepository.class);
    final DeliveryRuleService service = new DeliveryRuleService(repository, mock(EntityManager.class));
    DeliveryDistanceRule rule(String min, String max, int minutes, boolean available) {
        var r = new DeliveryDistanceRule(); r.setMinimumDistanceKm(new BigDecimal(min));
        r.setMaximumDistanceKm(max == null ? null : new BigDecimal(max)); r.setEstimatedMinutes(minutes);
        r.setServiceAvailable(available); return r;
    }
    void rules() {
        when(repository.findByProductScopeAndActiveTrueOrderByPriorityAscMinimumDistanceKmAsc("MOBILE_ONLY"))
            .thenReturn(List.of(rule("0", "5", 30, true), rule("5", "15", 60, true), rule("15", "30", 120, true), rule("30", null, 0, false)));
    }
    @Test void eightAndTwentyFiveKilometersUseActualConfiguredRules() {
        rules(); assertEquals(60, service.match(new BigDecimal("8")).estimatedMinutes());
        assertEquals(120, service.match(new BigDecimal("25")).estimatedMinutes());
    }
    @Test void boundaryAndOutsideAreaAreUnambiguous() {
        rules(); assertEquals(60, service.match(new BigDecimal("5")).estimatedMinutes());
        assertFalse(service.match(new BigDecimal("30")).serviceAvailable());
        assertFalse(service.match(new BigDecimal("100")).serviceAvailable());
    }
    @Test void touchingRangesDoNotOverlapButIntersectingRangesDo() {
        assertFalse(DeliveryRuleService.overlaps(BigDecimal.ZERO, new BigDecimal("5"), new BigDecimal("5"), new BigDecimal("15")));
        assertTrue(DeliveryRuleService.overlaps(BigDecimal.ZERO, new BigDecimal("6"), new BigDecimal("5"), null));
    }
    @Test void missingOrNegativeDistanceNeverPromisesDelivery() {
        assertThrows(ResponseStatusException.class, () -> service.match(null));
        assertThrows(ResponseStatusException.class, () -> service.match(BigDecimal.ONE.negate()));
        assertNull(service.match(BigDecimal.ZERO));
    }
}
