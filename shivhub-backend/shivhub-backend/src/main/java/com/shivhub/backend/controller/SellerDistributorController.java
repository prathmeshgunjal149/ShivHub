package com.shivhub.backend.controller;

import com.shivhub.backend.dto.SellerDistributorRequest;
import com.shivhub.backend.dto.SellerDistributorResponse;
import com.shivhub.backend.service.SellerDistributorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seller-distributors")
@RequiredArgsConstructor
public class SellerDistributorController {

    private final SellerDistributorService sellerDistributorService;

    @PostMapping
    public ResponseEntity<SellerDistributorResponse> assignDistributor(
            Authentication authentication,
            @Valid @RequestBody SellerDistributorRequest request
    ) {

        String sellerEmail = authentication.getName();

        SellerDistributorResponse response =
                sellerDistributorService.assignDistributor(
                        sellerEmail,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<SellerDistributorResponse>> getMyDistributors(
            Authentication authentication
    ) {

        String sellerEmail = authentication.getName();

        return ResponseEntity.ok(
                sellerDistributorService.getMyDistributors(
                        sellerEmail
                )
        );
    }

    @GetMapping("/active")
    public ResponseEntity<List<SellerDistributorResponse>> getMyActiveDistributors(
            Authentication authentication
    ) {

        String sellerEmail = authentication.getName();

        return ResponseEntity.ok(
                sellerDistributorService.getMyActiveDistributors(
                        sellerEmail
                )
        );
    }

    @GetMapping("/brand")
    public ResponseEntity<List<SellerDistributorResponse>> getByBrand(
            Authentication authentication,
            @RequestParam String brand
    ) {

        String sellerEmail = authentication.getName();

        return ResponseEntity.ok(
                sellerDistributorService.getByBrand(
                        sellerEmail,
                        brand
                )
        );
    }

    @GetMapping("/brand/active")
    public ResponseEntity<List<SellerDistributorResponse>> getActiveByBrand(
            Authentication authentication,
            @RequestParam String brand
    ) {

        String sellerEmail = authentication.getName();

        return ResponseEntity.ok(
                sellerDistributorService.getActiveByBrand(
                        sellerEmail,
                        brand
                )
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<SellerDistributorResponse> updateAssignment(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody SellerDistributorRequest request
    ) {

        String sellerEmail = authentication.getName();

        return ResponseEntity.ok(
                sellerDistributorService.updateAssignment(
                        sellerEmail,
                        id,
                        request
                )
        );
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<SellerDistributorResponse> activate(
            Authentication authentication,
            @PathVariable Long id
    ) {

        String sellerEmail = authentication.getName();

        return ResponseEntity.ok(
                sellerDistributorService.activate(
                        sellerEmail,
                        id
                )
        );
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<SellerDistributorResponse> deactivate(
            Authentication authentication,
            @PathVariable Long id
    ) {

        String sellerEmail = authentication.getName();

        return ResponseEntity.ok(
                sellerDistributorService.deactivate(
                        sellerEmail,
                        id
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAssignment(
            Authentication authentication,
            @PathVariable Long id
    ) {

        String sellerEmail = authentication.getName();

        sellerDistributorService.deleteAssignment(
                sellerEmail,
                id
        );

        return ResponseEntity.noContent().build();
    }
}