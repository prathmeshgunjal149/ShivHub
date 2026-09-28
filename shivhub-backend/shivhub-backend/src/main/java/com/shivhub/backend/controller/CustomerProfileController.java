package com.shivhub.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import com.shivhub.backend.dto.CustomerProfileResponse;
import com.shivhub.backend.dto.UpdateCustomerProfileRequest;
import com.shivhub.backend.service.CustomerProfileService;

@RestController
@RequestMapping("/api/customer/profile")
public class CustomerProfileController {

    private final CustomerProfileService customerProfileService;


    public CustomerProfileController(
            CustomerProfileService customerProfileService
    ) {
        this.customerProfileService =
                customerProfileService;
    }


    /*
     * =========================================================
     * GET PROFILE
     * =========================================================
     *
     * GET
     * /api/customer/profile
     *
     * JWT → email → customer
     */

    @GetMapping
    public ResponseEntity<CustomerProfileResponse> getProfile(
            Authentication authentication
    ) {

        String email =
                authentication.getName();


        return ResponseEntity.ok(
                customerProfileService
                        .getProfile(email)
        );
    }


    /*
     * =========================================================
     * UPDATE PROFILE
     * =========================================================
     *
     * PUT
     * /api/customer/profile
     */

    @PutMapping
    public ResponseEntity<CustomerProfileResponse> updateProfile(

            Authentication authentication,

            @RequestBody
            UpdateCustomerProfileRequest request

    ) {

        String email =
                authentication.getName();


        return ResponseEntity.ok(
                customerProfileService
                        .updateProfile(
                                email,
                                request
                        )
        );
    }

    @PostMapping("/photo")
    public ResponseEntity<CustomerProfileResponse> uploadProfilePhoto(
            Authentication authentication,
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok(
                customerProfileService.uploadProfilePhoto(authentication.getName(), file)
        );
    }

    @DeleteMapping("/photo")
    public ResponseEntity<CustomerProfileResponse> removeProfilePhoto(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                customerProfileService.removeProfilePhoto(authentication.getName())
        );
    }

    @PutMapping("/marketing-preference")
    public ResponseEntity<CustomerProfileResponse> updateMarketingPreference(
            Authentication authentication,
            @RequestBody java.util.Map<String, Boolean> request
    ) {
        return ResponseEntity.ok(
                customerProfileService.updateMarketingPreference(
                        authentication.getName(),
                        Boolean.TRUE.equals(request.get("optOut"))
                )
        );
    }
}
