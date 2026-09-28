package com.shivhub.backend.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shivhub.backend.dto.InstantMobileBillCancelRequest;
import com.shivhub.backend.dto.InstantMobileBillRequest;
import com.shivhub.backend.dto.InstantMobileBillResponse;
import com.shivhub.backend.service.SellerShopOperationsService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/seller/instant-bills")
@PreAuthorize("hasRole('SELLER')")
public class SellerInstantMobileBillController {
    private final SellerShopOperationsService service;
    public SellerInstantMobileBillController(SellerShopOperationsService service) { this.service = service; }
    @PostMapping public ResponseEntity<InstantMobileBillResponse> create(Authentication auth, @Valid @RequestBody InstantMobileBillRequest request) { return ResponseEntity.status(201).body(service.createInstantBill(auth.getName(), request)); }
    @GetMapping public List<InstantMobileBillResponse> list(Authentication auth, @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to, @RequestParam(required = false) String q) { return service.instantBills(auth.getName(), from, to, q); }
    @PostMapping("/{id}/cancel") public InstantMobileBillResponse cancel(Authentication auth, @PathVariable Long id, @Valid @RequestBody InstantMobileBillCancelRequest request) { return service.cancelInstantBill(auth.getName(), id, request); }
}
