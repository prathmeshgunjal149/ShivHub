package com.shivhub.backend.controller;

import java.util.List;
import com.shivhub.backend.dto.CreateProductRequest;
import com.shivhub.backend.service.NonMobileProductCreationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/seller/non-mobile-products")
@PreAuthorize("hasRole('SELLER')")
public class NonMobileProductController {
    private final NonMobileProductCreationService service;
    public NonMobileProductController(NonMobileProductCreationService service) { this.service = service; }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public NonMobileProductCreationService.CreatedProduct create(Authentication authentication,
            @RequestPart("product") CreateProductRequest product,
            @RequestPart(value = "images", required = false) MultipartFile[] images,
            @RequestPart(value = "imageUrls", required = false) List<String> imageUrls) {
        return service.create(authentication.getName(), product, images, imageUrls);
    }
}
