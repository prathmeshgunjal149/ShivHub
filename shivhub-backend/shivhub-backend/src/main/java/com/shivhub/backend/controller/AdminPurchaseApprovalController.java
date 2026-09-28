package com.shivhub.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shivhub.backend.dto.AdminPurchaseApprovalResponse;
import com.shivhub.backend.service.AdminPurchaseApprovalService;

/** Admin-only invoice approval queue, separate from direct product submissions. */
@RestController
@RequestMapping("/api/admin/purchase-approvals")
public class AdminPurchaseApprovalController {

    private final AdminPurchaseApprovalService service;

    public AdminPurchaseApprovalController(AdminPurchaseApprovalService service) {
        this.service = service;
    }

    @GetMapping
    public List<AdminPurchaseApprovalResponse> pendingApprovals() {
        return service.pendingApprovals();
    }

    @PostMapping("/{purchaseId}/approve")
    public ResponseEntity<Void> approve(
            @PathVariable Long purchaseId,
            @RequestBody(required = false) Map<String, String> body) {
        service.approve(purchaseId, body == null ? null : body.get("review"));
        return ResponseEntity.noContent().build();
    }
}
