package com.shivhub.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shivhub.backend.dto.MobileSpecificationRequest;
import com.shivhub.backend.dto.MobileSpecificationResponse;
import com.shivhub.backend.service.MobileSpecificationService;

import jakarta.validation.Valid;

@RestController
public class MobileSpecificationController {

    private final MobileSpecificationService service;

    public MobileSpecificationController(MobileSpecificationService service) {
        this.service = service;
    }

    @GetMapping("/api/mobile-specs")
    public ResponseEntity<List<MobileSpecificationResponse>> sellerSearch(
            @RequestParam(required = false) String q
    ) {
        return ResponseEntity.ok(service.searchActive(q));
    }

    @GetMapping("/api/admin/mobile-specs")
    public ResponseEntity<List<MobileSpecificationResponse>> adminSearch(
            @RequestParam(required = false) String q
    ) {
        return ResponseEntity.ok(service.searchForAdmin(q));
    }

    @GetMapping("/api/admin/mobile-specs/fetch")
    public ResponseEntity<MobileSpecificationResponse> fetchExternal(
            @RequestParam String brand,
            @RequestParam String model
    ) {
        return ResponseEntity.ok(service.fetchExternalPreview(brand, model));
    }

    @PostMapping("/api/admin/mobile-specs")
    public ResponseEntity<MobileSpecificationResponse> create(
            @Valid @RequestBody MobileSpecificationRequest request
    ) {
        return ResponseEntity.status(201).body(service.create(request));
    }

    @PutMapping("/api/admin/mobile-specs/{id}")
    public ResponseEntity<MobileSpecificationResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody MobileSpecificationRequest request
    ) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PatchMapping("/api/admin/mobile-specs/{id}/activate")
    public ResponseEntity<MobileSpecificationResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(service.changeStatus(id, true));
    }

    @PatchMapping("/api/admin/mobile-specs/{id}/deactivate")
    public ResponseEntity<MobileSpecificationResponse> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(service.changeStatus(id, false));
    }
}
