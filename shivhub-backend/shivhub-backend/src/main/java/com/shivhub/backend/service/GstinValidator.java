package com.shivhub.backend.service;

import java.util.Locale;

import org.springframework.stereotype.Component;

/** GSTIN syntax and checksum validation.  This is deliberately local: billing
 * does not claim to validate a GST registration against the GSTN portal. */
@Component
public class GstinValidator {
    private static final String CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public String normalizeAndValidate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String gstin = raw.trim().toUpperCase(Locale.ROOT);
        if (!gstin.matches("[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]")) {
            throw new IllegalArgumentException("Enter a valid 15-character GSTIN");
        }
        int factor = 2;
        int sum = 0;
        for (int index = gstin.length() - 2; index >= 0; index--) {
            int code = CHARS.indexOf(gstin.charAt(index));
            int product = code * factor;
            sum += product / 36 + product % 36;
            factor = factor == 2 ? 1 : 2;
        }
        int check = (36 - sum % 36) % 36;
        if (CHARS.charAt(check) != gstin.charAt(14)) throw new IllegalArgumentException("GSTIN checksum is invalid");
        return gstin;
    }

    public String stateCode(String gstin) { return gstin == null || gstin.length() < 2 ? null : gstin.substring(0, 2); }
}
