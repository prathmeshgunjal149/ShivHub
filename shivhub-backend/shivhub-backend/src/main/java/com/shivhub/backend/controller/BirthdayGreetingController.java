package com.shivhub.backend.controller;

import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.shivhub.backend.dto.BirthdayGreetingSettingRequest;
import com.shivhub.backend.entity.BirthdayGreetingSetting;
import com.shivhub.backend.service.BirthdayGreetingService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/birthday-settings")
@PreAuthorize("hasRole('ADMIN')")
public class BirthdayGreetingController {
    private final BirthdayGreetingService service;
    public BirthdayGreetingController(BirthdayGreetingService service) { this.service = service; }
    @GetMapping public BirthdayGreetingSetting get() { return service.current(); }
    @PutMapping public BirthdayGreetingSetting save(@Valid @RequestBody BirthdayGreetingSettingRequest request) { return service.save(request); }
    @PostMapping("/run-now") public Map<String, Object> runNow() { return service.sendToday(); }
}
