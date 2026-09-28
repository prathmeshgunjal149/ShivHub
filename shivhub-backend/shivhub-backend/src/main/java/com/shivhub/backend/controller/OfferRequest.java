package com.shivhub.backend.controller;

import java.math.BigDecimal;

/** Payload for changing or removing a product-level discount. */
public class OfferRequest {

    private BigDecimal offerPercentage;

    public BigDecimal getOfferPercentage() {
        return offerPercentage;
    }

    public void setOfferPercentage(BigDecimal offerPercentage) {
        this.offerPercentage = offerPercentage;
    }
}
