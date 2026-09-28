package com.shivhub.backend.service;

import org.springframework.http.HttpStatus;

public class BillingScanException extends RuntimeException {
    private final HttpStatus status;

    public BillingScanException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }
}
