package com.shivhub.backend.service;

/** Immutable event published after an enquiry transaction commits. */
public record CustomerEnquiryNotificationEvent(
        Type type,
        String ticketReference,
        String customerName,
        String customerEmail,
        String category,
        String subject,
        String status,
        String customerResponse
) {
    public enum Type { RECEIVED, UPDATED }
}
