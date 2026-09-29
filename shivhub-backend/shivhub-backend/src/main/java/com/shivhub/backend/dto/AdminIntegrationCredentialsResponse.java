package com.shivhub.backend.dto;

/** Deliberately contains statuses and public identifiers only; never secrets. */
public record AdminIntegrationCredentialsResponse(
        boolean encryptionReady,
        boolean mailConfigured,
        String mailSource,
        String mailUsername,
        boolean razorpayConfigured,
        String razorpaySource,
        String razorpayKeyId) { }
