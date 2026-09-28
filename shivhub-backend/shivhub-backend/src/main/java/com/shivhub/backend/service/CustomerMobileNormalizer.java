package com.shivhub.backend.service;

/** Canonical Indian mobile format used for POS lookup and new customer profiles. */
public final class CustomerMobileNormalizer {
    private CustomerMobileNormalizer() { }
    public static String normalizeIndianMobile(String value) {
        if (value == null) throw new IllegalArgumentException("Mobile number is required");
        String compact = value.trim().replaceAll("[\\s-]", "");
        if (compact.startsWith("+91")) compact = compact.substring(3);
        else if (compact.startsWith("91") && compact.length() == 12) compact = compact.substring(2);
        if (!compact.matches("[6-9][0-9]{9}")) throw new IllegalArgumentException("Enter a valid 10 digit Indian mobile number");
        return compact;
    }
}
