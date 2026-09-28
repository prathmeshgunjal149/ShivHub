package com.shivhub.backend.controller;

import com.shivhub.backend.dto.AdminDistributorAssignmentRequest;
import com.shivhub.backend.dto.SellerDistributorResponse;
import com.shivhub.backend.service.SellerDistributorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** Admin-only assignment directory. Sellers can view assignments but cannot create them. */
@RestController
@RequestMapping("/api/admin/distributor-assignments")
@RequiredArgsConstructor
public class AdminDistributorAssignmentController {
    private final SellerDistributorService sellerDistributorService;

    @GetMapping
    public ResponseEntity<List<SellerDistributorResponse>> getAll(Authentication authentication) {
        return ResponseEntity.ok(sellerDistributorService.getAllForAdmin(authentication.getName()));
    }

    @PostMapping
    public ResponseEntity<SellerDistributorResponse> assign(Authentication authentication,
            @Valid @RequestBody AdminDistributorAssignmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sellerDistributorService.assignByAdmin(authentication.getName(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(Authentication authentication, @PathVariable Long id) {
        sellerDistributorService.removeByAdmin(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
