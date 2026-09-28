package com.shivhub.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shivhub.backend.dto.ConfirmDistributorAdjustmentRequest;
import com.shivhub.backend.dto.CreateStockTransferAdjustmentRequest;
import com.shivhub.backend.dto.StockTransferAdjustmentResponse;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.repository.UserRepository;
import com.shivhub.backend.service.StockTransferAdjustmentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/stock-transfers")
@RequiredArgsConstructor
public class StockTransferAdjustmentController {

    private final StockTransferAdjustmentService service;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<StockTransferAdjustmentResponse>> list(
            Authentication authentication,
            @RequestParam(value = "status", required = false) String status) {

        return ResponseEntity.ok(service.list(seller(authentication), status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StockTransferAdjustmentResponse> get(
            Authentication authentication,
            @PathVariable Long id) {

        return ResponseEntity.ok(service.get(id, seller(authentication)));
    }

    @PostMapping
    public ResponseEntity<?> create(
            Authentication authentication,
            @Valid @RequestBody CreateStockTransferAdjustmentRequest request) {

        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(service.create(request, seller(authentication)));
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @PutMapping("/{id}/confirm-handover")
    public ResponseEntity<?> confirmHandover(
            Authentication authentication,
            @PathVariable Long id) {

        try {
            return ResponseEntity.ok(service.confirmHandover(id, seller(authentication)));
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @PutMapping("/{id}/confirm-adjustment")
    public ResponseEntity<?> confirmAdjustment(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody(required = false) ConfirmDistributorAdjustmentRequest request) {

        try {
            return ResponseEntity.ok(service.confirmAdjustment(id, request, seller(authentication)));
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    private User seller(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Authentication required");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Seller not found"));
    }
}
