package com.shivhub.backend.dto;

/** Deliberately small error body so a scanner never exposes another seller's inventory details. */
public record BillingScanErrorResponse(String message) { }
