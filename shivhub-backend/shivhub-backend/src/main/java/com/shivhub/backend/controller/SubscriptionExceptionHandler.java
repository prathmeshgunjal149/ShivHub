package com.shivhub.backend.controller;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.shivhub.backend.service.SubscriptionRequiredException;

@RestControllerAdvice
public class SubscriptionExceptionHandler {
    @ExceptionHandler(SubscriptionRequiredException.class)
    public ResponseEntity<Map<String, Object>> subscriptionRequired(SubscriptionRequiredException error) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "code", "SUBSCRIPTION_REQUIRED",
                "message", error.getMessage(),
                "subscriptionStatus", error.getStatus().name(),
                "renewalRequired", true));
    }
}
