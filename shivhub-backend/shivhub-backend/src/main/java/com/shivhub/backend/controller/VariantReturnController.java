package com.shivhub.backend.controller;
import com.shivhub.backend.dto.*;
import com.shivhub.backend.service.VariantReturnService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/seller/after-sales/requests/{id}/variant-disposition")
@PreAuthorize("hasAnyRole('SELLER','ADMIN')")
public class VariantReturnController {
    private final VariantReturnService service;
    public VariantReturnController(VariantReturnService service) { this.service = service; }
    @GetMapping public VariantReturnService.ReturnInfo info(Authentication a, @PathVariable Long id) { return service.info(a.getName(), id); }
    @PostMapping public VariantReturnDispositionResponse dispose(Authentication a, @PathVariable Long id, @Valid @RequestBody VariantReturnDispositionRequest input) { return service.dispose(a.getName(), id, input); }
}
