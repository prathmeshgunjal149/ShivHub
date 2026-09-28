package com.shivhub.backend.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;
import com.shivhub.backend.dto.*;
import com.shivhub.backend.service.FileStorageService;
import com.shivhub.backend.service.SellerCustomerService;
import jakarta.validation.Valid;

/** Seller customers originate from that seller's POS/offline bills only. */
@RestController
@RequestMapping("/api/seller/customers")
public class SellerCustomerController {
 private final SellerCustomerService service; private final FileStorageService storage;
 public SellerCustomerController(SellerCustomerService service,FileStorageService storage){this.service=service;this.storage=storage;}
 @GetMapping public ResponseEntity<List<SellerCustomerResponse>> list(Authentication auth){return ResponseEntity.ok(service.customers(email(auth)));}
 @GetMapping("/lookup") public ResponseEntity<SellerCustomerLookupResponse> lookup(@RequestParam String mobile, Authentication auth){return ResponseEntity.ok(service.lookup(email(auth),mobile));}
 @PostMapping("/register-or-update") public ResponseEntity<SellerCustomerLookupResponse> registerOrUpdate(Authentication auth,@Valid @RequestBody SellerCustomerUpsertRequest request){return ResponseEntity.ok(service.registerOrUpdate(email(auth),request));}
 @PostMapping("/email-campaign") public ResponseEntity<SellerEmailCampaignResponse> email(Authentication auth,@Valid @RequestBody SellerEmailCampaignRequest request){return ResponseEntity.ok(service.sendEmailCampaign(email(auth),request));}
 @PostMapping("/whatsapp-offer") @PreAuthorize("hasRole('SELLER')") public ResponseEntity<SellerWhatsAppOfferResponse> whatsappOffer(Authentication auth,@Valid @RequestBody SellerWhatsAppOfferRequest request){return ResponseEntity.ok(service.sendWhatsAppOffer(email(auth),request));}
 @PostMapping(path="/email-campaign/banner",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @PreAuthorize("hasRole('SELLER')") public ResponseEntity<Map<String,String>> uploadBanner(Authentication auth,@RequestPart("file") MultipartFile file){email(auth);return ResponseEntity.ok(Map.of("bannerUrl",storage.storeCampaignBanner(file)));}
 private String email(Authentication auth){if(auth==null||auth.getName()==null)throw new RuntimeException("Authentication required");return auth.getName();}
}
