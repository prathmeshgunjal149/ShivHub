package com.shivhub.backend.controller;
import java.time.LocalDate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.shivhub.backend.dto.SellerOperationsReportResponse;
import com.shivhub.backend.service.SellerOperationsReportService;
@RestController @RequestMapping("/api/seller/reports/operations") @PreAuthorize("hasRole('SELLER')")
public class SellerOperationsReportController { private final SellerOperationsReportService service; public SellerOperationsReportController(SellerOperationsReportService service){this.service=service;} @GetMapping public ResponseEntity<SellerOperationsReportResponse> get(Authentication auth,@RequestParam(required=false) LocalDate startDate,@RequestParam(required=false) LocalDate endDate){if(auth==null||auth.getName()==null||auth.getName().isBlank())throw new RuntimeException("Authentication required");return ResponseEntity.ok(service.get(auth.getName(),startDate,endDate));}}
