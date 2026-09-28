package com.shivhub.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CustomerMobileNormalizerTest {

    @Test
    void normalizesIndianPrefixSpacesAndHyphens() {
        assertEquals("9876543210", CustomerMobileNormalizer.normalizeIndianMobile("+91 98765-43210"));
        assertEquals("9876543210", CustomerMobileNormalizer.normalizeIndianMobile("919876543210"));
    }

    @Test
    void rejectsInvalidMobileNumbers() {
        assertThrows(IllegalArgumentException.class,
                () -> CustomerMobileNormalizer.normalizeIndianMobile("12345"));
    }
}
