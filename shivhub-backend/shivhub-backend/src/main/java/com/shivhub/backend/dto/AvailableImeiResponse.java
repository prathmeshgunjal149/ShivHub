package com.shivhub.backend.dto;

/** Safe POS choice for one unsold physical mobile unit. */
public record AvailableImeiResponse(Long serialId, String imei1, String imei2) {}
