package com.shivhub.backend.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.shivhub.backend.enums.AfterSalesAttachmentType;
import com.shivhub.backend.dto.*;
import com.shivhub.backend.enums.ServiceRequestStatus;
import com.shivhub.backend.service.AfterSalesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/seller/after-sales")
@PreAuthorize("hasAnyRole('SELLER','ADMIN')")
@RequiredArgsConstructor
public class SellerAfterSalesController {
 private final AfterSalesService service;
 @GetMapping("/summary") public AfterSalesSummaryResponse summary(Authentication a){return service.sellerSummary(a.getName());}
 @GetMapping("/offline-returns/bill/{billNumber}") public SellerOfflineReturnLookupResponse returnBill(Authentication a,@PathVariable String billNumber){return service.sellerOfflineReturnLookup(a.getName(),billNumber);}
 @GetMapping("/offline-returns/lookup/{scanCode}") public SellerOfflineReturnLookupResponse lookupReturn(Authentication a,@PathVariable String scanCode){return service.sellerOfflineReturnLookupByScan(a.getName(),scanCode);}
 @PostMapping("/offline-returns") public AfterSalesRequestResponse createOfflineReturn(Authentication a,@Valid @RequestBody SellerOfflineReturnRequest r){return service.createSellerOfflineReturn(a.getName(),r);}
 @GetMapping("/requests") public List<AfterSalesRequestResponse> list(Authentication a,@RequestParam(required=false) ServiceRequestStatus status){return service.listSeller(a.getName(),status);}
 @GetMapping("/requests/{id}") public AfterSalesRequestResponse get(Authentication a,@PathVariable Long id){return service.getSeller(a.getName(),id);}
 @PostMapping("/requests/{id}/status") public AfterSalesRequestResponse status(Authentication a,@PathVariable Long id,@Valid @RequestBody ServiceStatusChangeRequest r){return service.changeSellerStatus(a.getName(),id,r);}
 @PutMapping("/requests/{id}/inspection") public AfterSalesRequestResponse inspection(Authentication a,@PathVariable Long id,@Valid @RequestBody ServiceInspectionRequest r){return service.inspect(a.getName(),id,r);}
 @PutMapping("/requests/{id}/estimate") public AfterSalesRequestResponse estimate(Authentication a,@PathVariable Long id,@Valid @RequestBody ServiceEstimateRequest r){return service.saveEstimate(a.getName(),id,r);}
 @PostMapping("/requests/{id}/receive") public AfterSalesRequestResponse receive(Authentication a,@PathVariable Long id,@Valid @RequestBody ServiceReceiveRequest r){return service.receive(a.getName(),id,r);}
 @PostMapping("/requests/{id}/technician") public AfterSalesRequestResponse technician(Authentication a,@PathVariable Long id,@Valid @RequestBody TechnicianAssignmentRequest r){return service.assignTechnician(a.getName(),id,r);}
 @PostMapping("/requests/{id}/dispatch") public AfterSalesRequestResponse dispatch(Authentication a,@PathVariable Long id,@Valid @RequestBody ServiceCenterDispatchRequest r){return service.dispatch(a.getName(),id,r);}
 @PostMapping("/requests/{id}/replacement") public AfterSalesRequestResponse replacement(Authentication a,@PathVariable Long id,@Valid @RequestBody ReplacementRequest r){return service.processReplacement(a.getName(),id,r);}
 @PostMapping("/requests/{id}/refunds") public AfterSalesRequestResponse refund(Authentication a,@PathVariable Long id,@Valid @RequestBody ServiceRefundRequest r){return service.initiateRefund(a.getName(),id,r);}
 @PostMapping("/requests/{id}/refunds/{refundId}/complete") public AfterSalesRequestResponse completeRefund(Authentication a,@PathVariable Long id,@PathVariable Long refundId,@Valid @RequestBody RefundCompletionRequest r){return service.completeRefund(a.getName(),id,refundId,r);}
 @PostMapping(path="/requests/{id}/attachments", consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public ServiceAttachmentResponse attachment(Authentication a,@PathVariable Long id,@RequestParam AfterSalesAttachmentType type,@RequestPart("file") MultipartFile file){return service.uploadSellerAttachment(a.getName(),id,type,file);}
 @GetMapping(value="/requests/{id}/documents/{type}", produces=MediaType.TEXT_HTML_VALUE) public String document(Authentication a,@PathVariable Long id,@PathVariable String type){return service.sellerDocument(a.getName(),id,type);}
 @GetMapping("/requests/{id}/attachments/{attachmentId}/file") public ResponseEntity<Resource> attachmentFile(Authentication a,@PathVariable Long id,@PathVariable Long attachmentId){return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(service.sellerAttachmentFile(a.getName(),id,attachmentId));}
}
