package com.shivhub.backend.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shivhub.backend.dto.ShopRegisterAuditResponse;
import com.shivhub.backend.dto.ShopRegisterEntryRequest;
import com.shivhub.backend.dto.ShopRegisterEntryResponse;
import com.shivhub.backend.dto.ShopRegisterHistoryResponse;
import com.shivhub.backend.dto.ShopRegisterOpeningCashRequest;
import com.shivhub.backend.dto.ShopRegisterSummaryResponse;
import com.shivhub.backend.service.SellerShopOperationsService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/seller/shop-register")
@PreAuthorize("hasRole('SELLER')")
public class SellerShopRegisterController {
    private final SellerShopOperationsService service;
    public SellerShopRegisterController(SellerShopOperationsService service) { this.service = service; }

    @GetMapping("/summary") public ShopRegisterSummaryResponse summary(Authentication auth, @RequestParam(required = false) LocalDate date) { return service.summary(auth.getName(), date); }
    @GetMapping("/entries") public List<ShopRegisterEntryResponse> entries(Authentication auth, @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to, @RequestParam(required = false) String q) { return service.listEntries(auth.getName(), from, to, q); }
    @PostMapping("/entries") public ResponseEntity<ShopRegisterEntryResponse> add(Authentication auth, @Valid @RequestBody ShopRegisterEntryRequest request) { return ResponseEntity.status(201).body(service.addEntry(auth.getName(), request)); }
    @PutMapping("/entries/{id}") public ShopRegisterEntryResponse update(Authentication auth, @PathVariable Long id, @Valid @RequestBody ShopRegisterEntryRequest request) { return service.updateEntry(auth.getName(), id, request); }
    @DeleteMapping("/entries/{id}") public ResponseEntity<Void> delete(Authentication auth, @PathVariable Long id, @RequestParam String reason) { service.deleteEntry(auth.getName(), id, reason); return ResponseEntity.noContent().build(); }
    @PostMapping("/opening-cash") public ShopRegisterSummaryResponse opening(Authentication auth, @Valid @RequestBody ShopRegisterOpeningCashRequest request) { return service.saveOpeningCash(auth.getName(), request); }
    @GetMapping("/history") public List<ShopRegisterHistoryResponse> history(Authentication auth, @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to) { return service.history(auth.getName(), from, to); }
    @GetMapping("/entries/{id}/audits") public List<ShopRegisterAuditResponse> audits(Authentication auth, @PathVariable Long id) { return service.audits(auth.getName(), id); }
    @GetMapping(value = "/report.csv", produces = "text/csv") public ResponseEntity<byte[]> csv(Authentication auth, @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to, @RequestParam(required = false) String q) { return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=non-gst-shop-register.csv").contentType(MediaType.parseMediaType("text/csv")).body(service.csv(auth.getName(), from, to, q)); }
}
