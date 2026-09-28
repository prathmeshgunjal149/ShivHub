package com.shivhub.backend.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.core.io.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.shivhub.backend.enums.AfterSalesAttachmentType;
import com.shivhub.backend.dto.*;
import com.shivhub.backend.service.AfterSalesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/customer/after-sales")
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor
public class CustomerAfterSalesController {
    private final AfterSalesService afterSalesService;
    @GetMapping("/eligible-purchases") public List<EligibleAfterSalesPurchaseResponse> eligible(Authentication a) { return afterSalesService.eligiblePurchases(a.getName()); }
    @PostMapping("/requests") public ResponseEntity<AfterSalesRequestResponse> create(Authentication a,@Valid @RequestBody CreateAfterSalesRequest r){ return ResponseEntity.ok(afterSalesService.create(a.getName(),r)); }
    @GetMapping("/requests") public List<AfterSalesRequestResponse> list(Authentication a){return afterSalesService.listCustomer(a.getName());}
    @GetMapping("/requests/{id}") public AfterSalesRequestResponse get(Authentication a,@PathVariable Long id){return afterSalesService.getCustomer(a.getName(),id);}
    @PostMapping("/requests/{id}/estimate-decision") public AfterSalesRequestResponse estimate(Authentication a,@PathVariable Long id,@Valid @RequestBody EstimateDecisionRequest r){return afterSalesService.decideEstimate(a.getName(),id,r);}
    @PostMapping("/requests/{id}/cancel") public AfterSalesRequestResponse cancel(Authentication a,@PathVariable Long id,@RequestBody(required=false) java.util.Map<String,String> r){return afterSalesService.cancelCustomer(a.getName(),id,r==null?null:r.get("remarks"));}
    @PostMapping(path="/requests/{id}/attachments", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ServiceAttachmentResponse attachment(Authentication a,@PathVariable Long id,@RequestParam AfterSalesAttachmentType type,@RequestPart("file") MultipartFile file){return afterSalesService.uploadCustomerAttachment(a.getName(),id,type,file);}
    @GetMapping(value="/requests/{id}/documents/{type}", produces=MediaType.TEXT_HTML_VALUE)
    public String document(Authentication a,@PathVariable Long id,@PathVariable String type){return afterSalesService.customerDocument(a.getName(),id,type);}
    @GetMapping("/requests/{id}/attachments/{attachmentId}/file")
    public ResponseEntity<Resource> attachmentFile(Authentication a,@PathVariable Long id,@PathVariable Long attachmentId){return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(afterSalesService.customerAttachmentFile(a.getName(),id,attachmentId));}
}
