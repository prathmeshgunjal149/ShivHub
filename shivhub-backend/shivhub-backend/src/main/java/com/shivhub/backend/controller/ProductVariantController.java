package com.shivhub.backend.controller;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.shivhub.backend.dto.ProductVariantRequest;
import com.shivhub.backend.dto.ProductVariantResponse;
import com.shivhub.backend.service.ProductVariantService;
@RestController public class ProductVariantController {
 private final ProductVariantService service; public ProductVariantController(ProductVariantService service){this.service=service;}
 @GetMapping("/api/products/{productId}/mobile-options") public List<ProductVariantService.MobileOption> mobileOptions(@PathVariable Long productId){return service.mobileOptions(productId);}
 @GetMapping({"/api/customer/products/{productId}/variants", "/api/products/{productId}/variants"}) public List<ProductVariantResponse> customer(@PathVariable Long productId){return service.customerVariants(productId);}
 @GetMapping("/api/seller/products/{productId}/variants") @PreAuthorize("hasRole('SELLER')") public List<com.shivhub.backend.dto.SellerVariantResponse> seller(@PathVariable Long productId,Authentication a){return service.managementVariants(a.getName(),productId);}
 @GetMapping("/api/seller/products/{productId}/variant-configuration") @PreAuthorize("hasRole('SELLER')") public java.util.Map<String,Object> configuration(@PathVariable Long productId,Authentication a){return service.configuration(a.getName(),productId);}
 @PutMapping("/api/seller/products/{productId}/variants") @PreAuthorize("hasRole('SELLER')") public List<ProductVariantResponse> replace(@PathVariable Long productId,@Valid @RequestBody List<@Valid ProductVariantRequest> request,Authentication a){return service.replaceSellerVariants(a.getName(),productId,request);}
}
