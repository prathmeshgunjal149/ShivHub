package com.shivhub.backend.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.shivhub.backend.dto.DistributorRequest;
import com.shivhub.backend.dto.DistributorResponse;
import com.shivhub.backend.service.DistributorService;
import com.shivhub.backend.service.SellerDistributorService;
import jakarta.validation.Valid;

/** Seller submits a new distributor; it cannot be used until Admin approval. */
@RestController
@RequestMapping("/api/seller/distributor-requests")
public class SellerDistributorRequestController {
    private final DistributorService service;
    private final SellerDistributorService sellerDistributorService;
    public SellerDistributorRequestController(DistributorService service, SellerDistributorService sellerDistributorService) {
        this.service = service;
        this.sellerDistributorService = sellerDistributorService;
    }
    @PostMapping public ResponseEntity<DistributorResponse> submit(Authentication auth, @Valid @RequestBody DistributorRequest request) { if (auth == null) throw new RuntimeException("Authentication required"); return ResponseEntity.status(201).body(service.submitForApproval(auth.getName(), request)); }
    /** Seller cannot browse the global catalogue; return only Admin-assigned distributors. */
    @GetMapping("/available") public ResponseEntity<?> available(Authentication auth) {
        if (auth == null) throw new RuntimeException("Authentication required");
        return ResponseEntity.ok(sellerDistributorService.getMyActiveDistributors(auth.getName()));
    }
}
