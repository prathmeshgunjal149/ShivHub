package com.shivhub.backend.dto;

/** Delivery result for a seller's offer/greeting email campaign. */
public record SellerEmailCampaignResponse(int sent, int skipped) {}
