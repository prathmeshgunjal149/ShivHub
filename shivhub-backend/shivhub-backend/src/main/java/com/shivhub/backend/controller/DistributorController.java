package com.shivhub.backend.controller;

import com.shivhub.backend.dto.DistributorRequest;
import com.shivhub.backend.dto.DistributorResponse;
import com.shivhub.backend.service.DistributorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/distributors")
@RequiredArgsConstructor
public class DistributorController {

    private final DistributorService distributorService;

    // =========================
    // CREATE DISTRIBUTOR
    // =========================
    @PostMapping
    public ResponseEntity<DistributorResponse> createDistributor(
            @Valid @RequestBody DistributorRequest request) {

        DistributorResponse response =
                distributorService.createDistributor(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // =========================
    // GET ALL DISTRIBUTORS
    // =========================
    @GetMapping
    public ResponseEntity<List<DistributorResponse>> getAllDistributors() {

        return ResponseEntity.ok(
                distributorService.getAllDistributors()
        );
    }

    // =========================
    // GET ACTIVE DISTRIBUTORS
    // =========================
    @GetMapping("/active")
    public ResponseEntity<List<DistributorResponse>> getActiveDistributors() {

        return ResponseEntity.ok(
                distributorService.getActiveDistributors()
        );
    }

    // =========================
    // GET DISTRIBUTOR BY ID
    // =========================
    @GetMapping("/{id}")
    public ResponseEntity<DistributorResponse> getDistributorById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                distributorService.getDistributorById(id)
        );
    }

    // =========================
    // SEARCH DISTRIBUTORS
    // =========================
    @GetMapping("/search")
    public ResponseEntity<List<DistributorResponse>> searchDistributors(
            @RequestParam String businessName) {

        return ResponseEntity.ok(
                distributorService.searchDistributors(businessName)
        );
    }

    // =========================
    // UPDATE DISTRIBUTOR
    // =========================
    @PutMapping("/{id}")
    public ResponseEntity<DistributorResponse> updateDistributor(
            @PathVariable Long id,
            @Valid @RequestBody DistributorRequest request) {

        return ResponseEntity.ok(
                distributorService.updateDistributor(id, request)
        );
    }

    // =========================
    // ACTIVATE DISTRIBUTOR
    // =========================
    @PatchMapping("/{id}/activate")
    public ResponseEntity<DistributorResponse> activateDistributor(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                distributorService.activateDistributor(id)
        );
    }

    // =========================
    // DEACTIVATE DISTRIBUTOR
    // =========================
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<DistributorResponse> deactivateDistributor(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                distributorService.deactivateDistributor(id)
        );
    }

    // =========================
    // DELETE DISTRIBUTOR
    // =========================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDistributor(
            @PathVariable Long id) {

        distributorService.deleteDistributor(id);

        return ResponseEntity.noContent().build();
    }
}