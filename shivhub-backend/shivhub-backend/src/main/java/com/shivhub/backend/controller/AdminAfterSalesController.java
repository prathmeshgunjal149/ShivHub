package com.shivhub.backend.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.shivhub.backend.dto.*;
import com.shivhub.backend.enums.ServiceRequestStatus;
import com.shivhub.backend.service.AfterSalesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/after-sales")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminAfterSalesController {
 private final AfterSalesService service;
 @GetMapping("/requests") public List<AfterSalesRequestResponse> requests(Authentication a,@RequestParam(required=false) ServiceRequestStatus status,@RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to){return service.listAdmin(a.getName(),status,from,to);}
 @PostMapping("/requests/{id}/override") public AfterSalesRequestResponse override(Authentication a,@PathVariable Long id,@Valid @RequestBody AdminAfterSalesOverrideRequest r){return service.adminOverride(a.getName(),id,r);}
 @GetMapping("/policies") public List<AfterSalesPolicyResponse> policies(Authentication a){return service.policies(a.getName());}
 @PostMapping("/policies") public AfterSalesPolicyResponse createPolicy(Authentication a,@Valid @RequestBody AfterSalesPolicyRequest r){return service.savePolicy(a.getName(),null,r);}
 @PutMapping("/policies/{id}") public AfterSalesPolicyResponse updatePolicy(Authentication a,@PathVariable Long id,@Valid @RequestBody AfterSalesPolicyRequest r){return service.savePolicy(a.getName(),id,r);}
 @DeleteMapping("/policies/{id}") public void deactivatePolicy(Authentication a,@PathVariable Long id){service.deactivatePolicy(a.getName(),id);}
 @GetMapping("/reports") public Map<String,Object> report(Authentication a,@RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to){return service.adminReport(a.getName(),from,to);}
}
