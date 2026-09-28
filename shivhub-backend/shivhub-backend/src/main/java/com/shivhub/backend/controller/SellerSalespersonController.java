package com.shivhub.backend.controller;

import com.shivhub.backend.dto.SalespersonResponse;
import com.shivhub.backend.entity.ShopStaffAssignment;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.repository.ShopStaffAssignmentRepository;
import com.shivhub.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Supplies only the logged-in seller's active shop staff to the POS. */
@RestController
@RequestMapping("/api/seller/salespeople")
public class SellerSalespersonController {
    private final UserRepository users;
    private final ShopStaffAssignmentRepository assignments;

    public SellerSalespersonController(UserRepository users, ShopStaffAssignmentRepository assignments) {
        this.users = users;
        this.assignments = assignments;
    }

    @GetMapping
    public ResponseEntity<List<SalespersonResponse>> mine(Authentication auth) {
        User seller = users.findByEmail(auth.getName()).orElseThrow(() -> new RuntimeException("Seller not found"));
        if (seller.getRole() != Role.SELLER || !seller.isEnabled()) throw new RuntimeException("Only active sellers can view staff");
        return ResponseEntity.ok(assignments.findByShopOwnerAndActiveTrue(seller).stream()
                .filter(item -> item.getStaff() != null && item.getStaff().isEnabled())
                .map(item -> new SalespersonResponse(item.getStaff().getId(), item.getStaff().getName(),
                        item.getStaff().getMobile(), item.getShop().getName(), item.getAccessRole().name()))
                .toList());
    }
}
