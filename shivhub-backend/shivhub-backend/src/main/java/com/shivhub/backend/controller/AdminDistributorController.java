package com.shivhub.backend.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.shivhub.backend.dto.DistributorRequest;
import com.shivhub.backend.dto.DistributorResponse;
import com.shivhub.backend.service.DistributorService;
import jakarta.validation.Valid;

/** Admin-only distributor catalogue and seller-submission approval workflow. */
@RestController
@RequestMapping("/api/admin/distributors")
public class AdminDistributorController {
    private final DistributorService service;
    public AdminDistributorController(DistributorService service) { this.service = service; }
    @GetMapping public ResponseEntity<List<DistributorResponse>> all() { return ResponseEntity.ok(service.getAllDistributors()); }
    @GetMapping("/{id}/details") public ResponseEntity<Map<String,Object>> details(@PathVariable Long id) { return ResponseEntity.ok(service.getDistributorDetails(id)); }
    @GetMapping("/pending") public ResponseEntity<List<DistributorResponse>> pending() { return ResponseEntity.ok(service.getPendingRequests()); }
    @PostMapping public ResponseEntity<DistributorResponse> create(@Valid @RequestBody DistributorRequest request) { return ResponseEntity.status(201).body(service.createByAdmin(request)); }
    @PatchMapping("/{id}/approve") public ResponseEntity<DistributorResponse> approve(@PathVariable Long id, @RequestBody(required = false) Map<String,String> body) { return ResponseEntity.ok(service.approveRequest(id, body == null ? null : body.get("review"))); }
    @PatchMapping("/{id}/reject") public ResponseEntity<DistributorResponse> reject(@PathVariable Long id, @RequestBody(required = false) Map<String,String> body) { return ResponseEntity.ok(service.rejectRequest(id, body == null ? null : body.get("review"))); }
    @PatchMapping("/{id}/deactivate") public ResponseEntity<DistributorResponse> deactivate(@PathVariable Long id) { return ResponseEntity.ok(service.adminDeactivate(id)); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { service.adminDelete(id); return ResponseEntity.noContent().build(); }
}
