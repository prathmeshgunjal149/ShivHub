package com.shivhub.backend.controller;
import com.shivhub.backend.service.DeliveryEstimateService;
import com.shivhub.backend.dto.DeliveryEstimateResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @PreAuthorize("hasRole('CUSTOMER')")
public class DeliveryEstimateController {
    private final DeliveryEstimateService service;
    public DeliveryEstimateController(DeliveryEstimateService service){this.service=service;}
    @GetMapping("/api/customer/products/{productId}/delivery-estimate")
    public DeliveryEstimateResponse estimate(@PathVariable Long productId,@RequestParam(required=false) Long addressId,Authentication auth){return service.estimate(auth.getName(),productId,addressId);}
}
