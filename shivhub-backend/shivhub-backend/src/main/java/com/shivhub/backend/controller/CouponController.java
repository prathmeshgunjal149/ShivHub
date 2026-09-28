package com.shivhub.backend.controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shivhub.backend.dto.CouponRequest;
import com.shivhub.backend.dto.CouponResponse;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.repository.UserRepository;
import com.shivhub.backend.service.CouponService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class CouponController {
    private final CouponService coupons;
    private final UserRepository users;

    public CouponController(CouponService coupons, UserRepository users) {
        this.coupons = coupons;
        this.users = users;
    }

    @GetMapping("/coupons")
    public List<CouponResponse> available(Principal principal, @RequestParam(required = false) BigDecimal subtotal) {
        return coupons.availableFor(customer(principal).getId(), subtotal);
    }

    @GetMapping("/coupons/validate")
    public CouponResponse validate(Principal principal, @RequestParam String code, @RequestParam BigDecimal subtotal) {
        return coupons.validate(code, customer(principal).getId(), subtotal);
    }

    @GetMapping("/admin/coupons")
    public List<CouponResponse> list() { return coupons.list(); }

    @PostMapping("/admin/coupons")
    public CouponResponse create(@Valid @RequestBody CouponRequest request) { return coupons.create(request); }

    @PutMapping("/admin/coupons/{id}")
    public CouponResponse update(@PathVariable Long id, @Valid @RequestBody CouponRequest request) { return coupons.update(id, request); }

    @DeleteMapping("/admin/coupons/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { coupons.delete(id); return ResponseEntity.noContent().build(); }

    private User customer(Principal principal) {
        User user = users.findByEmail(principal.getName()).orElseThrow(() -> new RuntimeException("Customer not found"));
        if (user.getRole() != Role.CUSTOMER) throw new RuntimeException("Only customers can use coupons");
        return user;
    }
}
