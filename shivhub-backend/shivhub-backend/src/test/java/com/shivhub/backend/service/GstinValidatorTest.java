package com.shivhub.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class GstinValidatorTest {
    private final GstinValidator validator = new GstinValidator();

    @Test
    void acceptsAndNormalizesAValidGstin() {
        assertEquals("27AAPFU0939F1ZV", validator.normalizeAndValidate(" 27aapfu0939f1zv "));
    }

    @Test
    void rejectsAnInvalidChecksum() {
        assertThrows(IllegalArgumentException.class, () -> validator.normalizeAndValidate("27AAPFU0939F1ZA"));
    }
}
