package com.shivhub.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.transaction.annotation.Transactional;
import com.shivhub.backend.dto.*;
import com.shivhub.backend.entity.*;
import com.shivhub.backend.enums.*;
import com.shivhub.backend.repository.*;
import lombok.RequiredArgsConstructor;

/** Ownership, eligibility, timeline and estimate operations for after-sales requests. */
@Service
@RequiredArgsConstructor
public class AfterSalesService {
    private static final java.util.Set<ServiceRequestStatus> INACTIVE = EnumSet.of(
            ServiceRequestStatus.CLOSED, ServiceRequestStatus.CANCELLED, ServiceRequestStatus.COMPLETED,
            ServiceRequestStatus.REJECTED, ServiceRequestStatus.REFUNDED, ServiceRequestStatus.REPLACED);
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OfflineBillRepository offlineBillRepository;
    private final OfflineBillItemRepository offlineBillItemRepository;
    private final PurchaseItemSerialRepository serialRepository;
    private final ProductRepository productRepository;
    private final ServiceRequestRepository requestRepository;
    private final ServiceStatusHistoryRepository historyRepository;
    private final ServiceAttachmentRepository attachmentRepository;
    private final ServiceEstimateRepository estimateRepository;
    private final ServicePartRepository partRepository;
    private final ServiceCenterDispatchRepository dispatchRepository;
    private final ServiceRefundRepository refundRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ReturnReplacementRepository returnReplacementRepository;
    private final AfterSalesStockMovementRepository afterSalesStockRepository;
    private final AfterSalesCreditNoteRepository creditNoteRepository;
    private final StockMovementService stockMovementService;
    private final LoyaltyService loyaltyService;
    private final ApplicationEventPublisher eventPublisher;
    private final FileStorageService fileStorage;
    private final AfterSalesAuditLogRepository auditRepository;
    private final AfterSalesDocumentService documentService;
    private final AfterSalesPolicyRepository policyRepository;
    private final ServiceRequestTransitionService transitionService;
    /** Stops future customer EMI reminders only after the financed POS sale is actually refunded. */
    private final FinanceService financeService;

    @Transactional(readOnly = true)
    public List<EligibleAfterSalesPurchaseResponse> eligiblePurchases(String email) {
        User customer = customer(email);
        List<EligibleAfterSalesPurchaseResponse> result = new ArrayList<>();
        for (Order order : orderRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId())) {
            if (order.getOrderStatus() != OrderStatus.DELIVERED || order.getPaymentStatus() != PaymentStatus.PAID) continue;
            for (OrderItem item : orderItemRepository.findByOrderId(order.getId())) {
                result.addAll(toEligibleOrder(order, item));
            }
        }
        for (OfflineBill bill : offlineBillRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId())) {
            if (bill.getPaymentStatus() != PaymentStatus.PAID) continue;
            for (OfflineBillItem item : offlineBillItemRepository.findByOfflineBillId(bill.getId())) {
                result.addAll(toEligibleBill(bill, item));
            }
        }
        return result;
    }

    @Transactional
    public AfterSalesRequestResponse create(String email, CreateAfterSalesRequest input) {
        User customer = customer(email);
        SaleReference sale = resolveSale(customer, input);
        boolean duplicate = sale.orderItemId != null
                ? requestRepository.existsByOrderItemIdAndStatusNotIn(sale.orderItemId, INACTIVE)
                : requestRepository.existsByOfflineBillItemIdAndStatusNotIn(sale.billItemId, INACTIVE);
        if (duplicate) throw new IllegalArgumentException("An active after-sales request already exists for this sale item");
        PolicyEvaluation policy = policyFor(sale.productId, sale.saleDate);
        validateEligibility(input.getRequestType(), policy);
        ServiceRequest request = new ServiceRequest();
        request.setRequestNumber(nextRequestNumber()); request.setRequestType(input.getRequestType()); request.setCustomerId(customer.getId());
        request.setSellerId(sale.sellerId); request.setProductId(sale.productId); request.setOrderId(sale.orderId); request.setOrderItemId(sale.orderItemId);
        request.setOfflineBillId(sale.billId); request.setOfflineBillItemId(sale.billItemId); request.setPurchaseSerialId(sale.serial == null ? null : sale.serial.getId());
        if (sale.serial != null) { request.setImei1Snapshot(sale.serial.getImei1()); request.setImei2Snapshot(sale.serial.getImei2()); request.setSerialNumberSnapshot(sale.serial.getSerialNumber()); }
        request.setIssueCategory(blankToNull(input.getIssueCategory())); request.setCustomerIssue(input.getCustomerIssue().trim()); request.setPickupType(input.getPickupType());
        request.setWarrantyEligible(policy.warrantyActive); request.setWarrantyStartDate(sale.saleDate); request.setWarrantyEndDate(policy.warrantyEndDate); request.setReturnEligible(policy.returnEligible);
        ServiceRequest saved = requestRepository.save(request);
        history(saved, null, ServiceRequestStatus.REQUESTED, "Request created", customer.getId(), true);
        eventPublisher.publishEvent(new AfterSalesNotificationEvent(saved.getId(), AfterSalesNotificationType.REQUEST_CREATED, "Request created"));
        return customerResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AfterSalesRequestResponse> listCustomer(String email) {
        User user = customer(email); return requestRepository.findByCustomerIdOrderByCreatedAtDesc(user.getId()).stream().map(this::customerResponse).toList();
    }
    @Transactional(readOnly = true)
    public AfterSalesRequestResponse getCustomer(String email, Long id) { ServiceRequest r=get(id); requireCustomer(r, customer(email)); return customerResponse(r); }
    @Transactional
    public AfterSalesRequestResponse cancelCustomer(String email, Long id, String remarks) {
        User user=customer(email); ServiceRequest r=get(id); requireCustomer(r,user); move(r, ServiceRequestStatus.CANCELLED, remarks, user.getId(), true); return customerResponse(r);
    }
    @Transactional
    public AfterSalesRequestResponse decideEstimate(String email, Long id, EstimateDecisionRequest decision) {
        User user=customer(email); ServiceRequest r=get(id); requireCustomer(r,user);
        ServiceEstimate estimate=estimateRepository.findByServiceRequestId(id).orElseThrow(() -> new IllegalArgumentException("Repair estimate not found"));
        if (r.getStatus()!=ServiceRequestStatus.PAID_REPAIR_APPROVAL_REQUIRED || estimate.getCustomerApprovalStatus()!=EstimateApprovalStatus.PENDING) throw new IllegalArgumentException("This estimate cannot be decided now");
        estimate.setCustomerApprovalStatus(Boolean.TRUE.equals(decision.getApproved()) ? EstimateApprovalStatus.APPROVED : EstimateApprovalStatus.REJECTED); estimate.setApprovedAt(LocalDateTime.now()); estimateRepository.save(estimate);
        move(r, Boolean.TRUE.equals(decision.getApproved()) ? ServiceRequestStatus.ESTIMATE_APPROVED : ServiceRequestStatus.ESTIMATE_REJECTED, blankToNull(decision.getRemarks()), user.getId(), true);
        return customerResponse(r);
    }
    @Transactional
    public ServiceAttachmentResponse uploadCustomerAttachment(String email, Long id, AfterSalesAttachmentType type, MultipartFile file) {
        User user=customer(email); ServiceRequest request=get(id); requireCustomer(request,user);
        if (type != AfterSalesAttachmentType.IMAGE && type != AfterSalesAttachmentType.DOCUMENT && type != AfterSalesAttachmentType.VIDEO) throw new IllegalArgumentException("Customers may upload only evidence images, documents, or videos");
        return saveAttachment(request,user.getId(),type,file);
    }
    @Transactional(readOnly = true)
    public String customerDocument(String email, Long id, String type) { User user=customer(email);ServiceRequest r=get(id);requireCustomer(r,user);return documentService.render(r,type,false); }
    @Transactional(readOnly = true)
    public Resource customerAttachmentFile(String email, Long requestId, Long attachmentId) { User user=customer(email);ServiceRequest r=get(requestId);requireCustomer(r,user);return attachmentFile(r,attachmentId); }

    @Transactional(readOnly = true)
    public List<AfterSalesRequestResponse> listSeller(String email, ServiceRequestStatus status) {
        User seller=seller(email); return requestRepository.findSellerQueue(seller.getId(), status).stream().map(r -> response(r,true)).toList();
    }
    @Transactional(readOnly = true)
    public AfterSalesSummaryResponse sellerSummary(String email) {
        User seller=seller(email); List<ServiceRequest> all=requestRepository.findBySellerIdOrderByCreatedAtDesc(seller.getId());
        java.util.Map<String,Long> counts=new java.util.LinkedHashMap<>(); for(ServiceRequestStatus status:ServiceRequestStatus.values()) counts.put(status.name(),all.stream().filter(r->r.getStatus()==status).count());
        AfterSalesSummaryResponse out=new AfterSalesSummaryResponse();out.setNewRequests(counts.get(ServiceRequestStatus.REQUESTED.name()));out.setUnderReview(counts.get(ServiceRequestStatus.UNDER_REVIEW.name()));out.setProductsReceived(counts.get(ServiceRequestStatus.PRODUCT_RECEIVED.name()));out.setPendingInspection(counts.get(ServiceRequestStatus.INSPECTION_IN_PROGRESS.name()));out.setWaitingCustomerApproval(counts.get(ServiceRequestStatus.PAID_REPAIR_APPROVAL_REQUIRED.name()));out.setInRepair(counts.get(ServiceRequestStatus.REPAIR_IN_PROGRESS.name()));out.setReadyForDelivery(counts.get(ServiceRequestStatus.READY_FOR_DELIVERY.name()));out.setPendingRefunds(counts.get(ServiceRequestStatus.REFUND_INITIATED.name()));out.setOverdueServices(all.stream().filter(r->r.getExpectedCompletionDate()!=null&&r.getExpectedCompletionDate().isBefore(LocalDate.now())&&!INACTIVE.contains(r.getStatus())).count());out.setByStatus(counts);return out;
    }
    /** Bill-number lookup used only by a seller's POS return counter. */
    @Transactional(readOnly = true)
    public SellerOfflineReturnLookupResponse sellerOfflineReturnLookup(String email, String billNumber) {
        User actor = seller(email);
        if (billNumber == null || billNumber.isBlank()) throw new IllegalArgumentException("Bill number is required");
        OfflineBill bill = offlineBillRepository.findByBillNumberAndSellerId(billNumber.trim(), actor.getId())
                .orElseThrow(() -> new IllegalArgumentException("Offline bill was not found in your shop"));
        SellerOfflineReturnLookupResponse out = new SellerOfflineReturnLookupResponse();
        out.setBillId(bill.getId()); out.setBillNumber(bill.getBillNumber()); out.setCustomerName(bill.getCustomerName());
        out.setCustomerMobile(bill.getCustomerMobile()); out.setPaymentStatus(bill.getPaymentStatus()); out.setSaleDate(bill.getCreatedAt());
        for (OfflineBillItem item : offlineBillItemRepository.findByOfflineBillId(bill.getId())) {
            SellerOfflineReturnLookupResponse.Item row = new SellerOfflineReturnLookupResponse.Item();
            row.setBillItemId(item.getId()); row.setProductId(item.getProductId()); row.setProductName(item.getProductName());
            row.setQuantity(item.getQuantity()); row.setRefundableAmount(money(zero(item.getTotalPrice())));
            boolean active = requestRepository.existsByOfflineBillItemIdAndStatusNotIn(item.getId(), INACTIVE);
            PolicyEvaluation policy = policyFor(item.getProductId(), bill.getCreatedAt().toLocalDate());
            boolean paid = bill.getPaymentStatus() == PaymentStatus.PAID;
            row.setReturnEligible(paid && policy.returnEligible && !active);
            if (!paid) row.setIneligibleReason("Only paid bills can be returned");
            else if (active) row.setIneligibleReason("An active after-sales request already exists for this item");
            else if (!policy.returnEligible) row.setIneligibleReason("The configured return window has expired or this item is not returnable");
            for (PurchaseItemSerial serial : serialRepository.findSoldForOfflineBillItem(item.getId())) {
                SellerOfflineReturnLookupResponse.Serial serialRow = new SellerOfflineReturnLookupResponse.Serial();
                serialRow.setPurchaseSerialId(serial.getId()); serialRow.setImei1(serial.getImei1()); serialRow.setImei2(serial.getImei2()); serialRow.setSerialNumber(serial.getSerialNumber());
                row.getSerials().add(serialRow);
            }
            out.getItems().add(row);
        }
        return out;
    }

    /** Exact POS scanner/manual lookup for a physical mobile or serialized return. */
    @Transactional(readOnly = true)
    public SellerOfflineReturnLookupResponse sellerOfflineReturnLookupByScan(String email, String scanCode) {
        User actor = seller(email);
        String code = scanCode == null ? "" : scanCode.trim();
        if (code.isBlank()) throw new IllegalArgumentException("Invoice number, IMEI or serial number is required");
        for (PurchaseItemSerial serial : serialRepository.findByExactScanCode(code)) {
            OfflineBillItem sold = serial.getSoldOfflineBillItem();
            if (sold == null || sold.getOfflineBill() == null) continue;
            OfflineBill bill = sold.getOfflineBill();
            if (!actor.getId().equals(bill.getSellerId())) throw new SecurityException("This IMEI belongs to another seller");
            return sellerOfflineReturnLookup(email, bill.getBillNumber());
        }
        return sellerOfflineReturnLookup(email, code);
    }
    /** Creates physical return intake. The unit is deliberately moved to RETURN_PENDING, never sellable stock. */
    @Transactional
    public AfterSalesRequestResponse createSellerOfflineReturn(String email, SellerOfflineReturnRequest input) {
        User actor = seller(email);
        OfflineBill bill = offlineBillRepository.findById(input.getOfflineBillId()).orElseThrow(() -> new IllegalArgumentException("Offline bill not found"));
        if (!actor.getId().equals(bill.getSellerId())) throw new SecurityException("This bill does not belong to the authenticated seller");
        if (bill.getPaymentStatus() != PaymentStatus.PAID) throw new IllegalArgumentException("Only paid offline bills can be returned");
        OfflineBillItem item = offlineBillItemRepository.findById(input.getOfflineBillItemId()).orElseThrow(() -> new IllegalArgumentException("Offline bill item not found"));
        if (!item.getOfflineBill().getId().equals(bill.getId())) throw new SecurityException("Bill item does not belong to this bill");
        if (requestRepository.existsByOfflineBillItemIdAndStatusNotIn(item.getId(), INACTIVE)) throw new IllegalArgumentException("An active after-sales request already exists for this sale item");
        PolicyEvaluation policy = policyFor(item.getProductId(), bill.getCreatedAt().toLocalDate());
        validateEligibility(ServiceRequestType.RETURN, policy);
        List<PurchaseItemSerial> soldSerials = serialRepository.findSoldForOfflineBillItem(item.getId());
        PurchaseItemSerial serial = null;
        if (!soldSerials.isEmpty()) {
            if (input.getPurchaseSerialId() == null) throw new IllegalArgumentException("Select the returned IMEI/serial unit");
            serial = verifySerial(input.getPurchaseSerialId(), item.getProductId(), null, item.getId());
        } else if (item.getQuantity() != null && item.getQuantity() > 1) {
            throw new IllegalArgumentException("A multi-quantity non-serial item must be returned through a separate item-level request");
        }
        ServiceRequest request = new ServiceRequest();
        request.setRequestNumber(nextRequestNumber()); request.setRequestType(ServiceRequestType.RETURN); request.setStatus(ServiceRequestStatus.PRODUCT_RECEIVED);
        request.setCustomerId(bill.getCustomerId()); request.setSellerId(actor.getId()); request.setProductId(item.getProductId());
        request.setOfflineBillId(bill.getId()); request.setOfflineBillItemId(item.getId()); request.setPurchaseSerialId(serial == null ? null : serial.getId());
        if (serial != null) { request.setImei1Snapshot(serial.getImei1()); request.setImei2Snapshot(serial.getImei2()); request.setSerialNumberSnapshot(serial.getSerialNumber()); }
        request.setIssueCategory(input.getReturnReason().trim()); request.setCustomerIssue(input.getCustomerIssue().trim()); request.setReceivingCondition(input.getReceivingCondition().trim());
        request.setCustomerVisibleRemarks("Product received at seller counter for return inspection"); request.setWarrantyEligible(policy.warrantyActive); request.setWarrantyStartDate(bill.getCreatedAt().toLocalDate()); request.setWarrantyEndDate(policy.warrantyEndDate); request.setReturnEligible(true); request.setPickupType(ServicePickupType.SHOP_VISIT); request.setReceivedDate(LocalDateTime.now());
        ServiceRequest saved = requestRepository.save(request);
        if (serial != null) changeSerialCondition(saved, serial, "RETURN_PENDING", actor.getId(), "Seller return intake for " + bill.getBillNumber());
        history(saved, null, ServiceRequestStatus.PRODUCT_RECEIVED, "Seller created return intake from offline bill " + bill.getBillNumber(), actor.getId(), true);
        if (saved.getCustomerId() != null) eventPublisher.publishEvent(new AfterSalesNotificationEvent(saved.getId(), AfterSalesNotificationType.PRODUCT_RECEIVED, "Product received for return inspection"));
        return response(saved, true);
    }
    @Transactional(readOnly = true)
    public List<AfterSalesRequestResponse> listAdmin(String email, ServiceRequestStatus status, LocalDate from, LocalDate to) { admin(email); return requestRepository.findAdminQueue(status,from==null?null:from.atStartOfDay(),to==null?null:to.plusDays(1).atStartOfDay()).stream().map(r->response(r,true)).toList(); }
    @Transactional(readOnly = true)
    public List<AfterSalesPolicyResponse> policies(String email) { admin(email); return policyRepository.findAll().stream().map(this::policyResponse).toList(); }
    @Transactional
    public AfterSalesPolicyResponse savePolicy(String email, Long id, AfterSalesPolicyRequest input) { User actor=admin(email);AfterSalesPolicy p=id==null?new AfterSalesPolicy():policyRepository.findById(id).orElseThrow(()->new IllegalArgumentException("After-sales policy not found"));p.setCategoryId(input.getCategoryId());p.setProductId(input.getProductId());p.setWarrantyType(input.getWarrantyType().trim().toUpperCase(Locale.ROOT));p.setWarrantyMonths(input.getWarrantyMonths());p.setReturnable(input.isReturnable());p.setReturnWindowDays(input.getReturnWindowDays());p.setReplacementWindowDays(input.getReplacementWindowDays());p.setDoaWindowDays(input.getDoaWindowDays());p.setPhysicalDamageAllowed(input.isPhysicalDamageAllowed());p.setLiquidDamageAllowed(input.isLiquidDamageAllowed());p.setOpenedBoxReturnAllowed(input.isOpenedBoxReturnAllowed());p.setChangeOfMindAllowed(input.isChangeOfMindAllowed());p.setRequiredEvidence(blankToNull(input.getRequiredEvidence()));p.setPolicyTerms(blankToNull(input.getPolicyTerms()));p.setActive(input.isActive());if(p.getId()==null)p.setCreatedByUserId(actor.getId());p.setUpdatedByUserId(actor.getId());return policyResponse(policyRepository.save(p)); }
    @Transactional
    public void deactivatePolicy(String email, Long id) { User actor=admin(email);AfterSalesPolicy p=policyRepository.findById(id).orElseThrow(()->new IllegalArgumentException("After-sales policy not found"));p.setActive(false);p.setUpdatedByUserId(actor.getId());policyRepository.save(p); }
    @Transactional
    public AfterSalesRequestResponse adminOverride(String email, Long id, AdminAfterSalesOverrideRequest input) { User actor=admin(email);ServiceRequest r=get(id);ServiceRequestStatus before=r.getStatus();r.setStatus(input.getStatus());if(input.getStatus()==ServiceRequestStatus.REJECTED)r.setRejectionReason(input.getReason().trim());if(input.getStatus()==ServiceRequestStatus.CLOSED)r.setClosedDate(LocalDateTime.now());requestRepository.save(r);history(r,before,input.getStatus(),input.getReason().trim(),actor.getId(),input.isCustomerVisible());AfterSalesAuditLog log=new AfterSalesAuditLog();log.setServiceRequest(r);log.setActorUserId(actor.getId());log.setAction("ADMIN_STATUS_OVERRIDE");log.setReason(input.getReason().trim());auditRepository.save(log);AfterSalesNotificationType notification=notificationFor(input.getStatus());if(notification!=null)eventPublisher.publishEvent(new AfterSalesNotificationEvent(r.getId(),notification,input.getReason().trim()));return response(r,true); }
    @Transactional(readOnly = true)
    public java.util.Map<String,Object> adminReport(String email, LocalDate from, LocalDate to) { admin(email);List<ServiceRequest> rows=requestRepository.findAdminQueue(null,from==null?null:from.atStartOfDay(),to==null?null:to.plusDays(1).atStartOfDay());java.util.Map<String,Object> report=new java.util.LinkedHashMap<>();report.put("totalRequests",rows.size());report.put("byStatus",rows.stream().collect(java.util.stream.Collectors.groupingBy(r->r.getStatus().name(),java.util.LinkedHashMap::new,java.util.stream.Collectors.counting())));report.put("byType",rows.stream().collect(java.util.stream.Collectors.groupingBy(r->r.getRequestType().name(),java.util.LinkedHashMap::new,java.util.stream.Collectors.counting())));report.put("sellerWise",rows.stream().collect(java.util.stream.Collectors.groupingBy(ServiceRequest::getSellerId,java.util.LinkedHashMap::new,java.util.stream.Collectors.counting())));report.put("overdue",rows.stream().filter(r->r.getExpectedCompletionDate()!=null&&r.getExpectedCompletionDate().isBefore(LocalDate.now())&&!INACTIVE.contains(r.getStatus())).count());return report; }
    @Transactional(readOnly = true)
    public AfterSalesRequestResponse getSeller(String email, Long id) { User seller=seller(email); ServiceRequest r=get(id); requireSeller(r,seller); return response(r,true); }
    @Transactional
    public ServiceAttachmentResponse uploadSellerAttachment(String email, Long id, AfterSalesAttachmentType type, MultipartFile file) { User seller=seller(email); ServiceRequest request=get(id);requireSeller(request,seller);return saveAttachment(request,seller.getId(),type,file); }
    @Transactional(readOnly = true)
    public String sellerDocument(String email, Long id, String type) { User seller=seller(email);ServiceRequest r=get(id);requireSeller(r,seller);return documentService.render(r,type,true); }
    @Transactional(readOnly = true)
    public Resource sellerAttachmentFile(String email, Long requestId, Long attachmentId) { User seller=seller(email);ServiceRequest r=get(requestId);requireSeller(r,seller);return attachmentFile(r,attachmentId); }
    @Transactional
    public AfterSalesRequestResponse changeSellerStatus(String email, Long id, ServiceStatusChangeRequest input) {
        User seller=seller(email); ServiceRequest r=get(id); requireSeller(r,seller);
        if (input.getStatus()==ServiceRequestStatus.REJECTED && (input.getRemarks()==null || input.getRemarks().isBlank())) throw new IllegalArgumentException("A rejection reason is required");
        if (input.getStatus()==ServiceRequestStatus.REJECTED) r.setRejectionReason(input.getRemarks().trim());
        move(r,input.getStatus(),blankToNull(input.getRemarks()),seller.getId(),input.isCustomerVisible()); return response(r,true);
    }
    @Transactional
    public AfterSalesRequestResponse inspect(String email, Long id, ServiceInspectionRequest input) {
        User seller=seller(email); ServiceRequest r=get(id); requireSeller(r,seller);
        if (r.getStatus()!=ServiceRequestStatus.INSPECTION_IN_PROGRESS) throw new IllegalArgumentException("Product must be in inspection before recording findings");
        r.setInspectionFinding(input.getFinding()); r.setSellerDiagnosis(blankToNull(input.getSellerDiagnosis())); r.setInternalNotes(blankToNull(input.getInternalNotes())); r.setCustomerVisibleRemarks(blankToNull(input.getCustomerVisibleRemarks()));
        if (input.getWarrantyEligible()!=null) r.setWarrantyEligible(input.getWarrantyEligible());
        if (Boolean.FALSE.equals(input.getWarrantyEligible()) && r.getRequestType()==ServiceRequestType.WARRANTY_CLAIM && (input.getRejectionReason()==null || input.getRejectionReason().isBlank())) throw new IllegalArgumentException("A warranty rejection reason is required");
        if (Boolean.FALSE.equals(input.getWarrantyEligible())) r.setRejectionReason(blankToNull(input.getRejectionReason()));
        requestRepository.save(r); return response(r,true);
    }
    @Transactional
    public AfterSalesRequestResponse saveEstimate(String email, Long id, ServiceEstimateRequest input) {
        User seller=seller(email); ServiceRequest r=get(id); requireSeller(r,seller);
        if (r.getStatus()!=ServiceRequestStatus.PAID_REPAIR_APPROVAL_REQUIRED) throw new IllegalArgumentException("A repair estimate is allowed only after paid-repair approval is requested");
        ServiceEstimate estimate=estimateRepository.findByServiceRequestId(id).orElseGet(ServiceEstimate::new); estimate.setServiceRequest(r);
        partRepository.deleteByServiceRequestId(id);
        BigDecimal parts=BigDecimal.ZERO;
        if (input.getParts()!=null) for (ServicePartRequest p : input.getParts()) { ServicePart entity=new ServicePart(); entity.setServiceRequest(r); entity.setPartName(p.getPartName().trim()); entity.setPartNumber(blankToNull(p.getPartNumber())); entity.setQuantity(p.getQuantity()); entity.setUnitPrice(money(p.getUnitPrice())); entity.setGstRate(money(p.getGstRate())); entity.setWarrantyMonths(p.getWarrantyMonths()); entity.setTotalAmount(money(entity.getUnitPrice().multiply(BigDecimal.valueOf(entity.getQuantity())))); parts=parts.add(entity.getTotalAmount()); partRepository.save(entity); }
        BigDecimal base=money(zero(input.getInspectionCharge()).add(parts).add(zero(input.getLabourAmount())).subtract(zero(input.getDiscount()))); if (base.signum()<0) throw new IllegalArgumentException("Discount cannot exceed repair charges");
        BigDecimal tax=money(base.multiply(zero(input.getGstRate())).divide(BigDecimal.valueOf(100),2,RoundingMode.HALF_UP));
        BigDecimal advance=money(zero(input.getAdvanceAmount())); BigDecimal grand=base.add(tax); if (advance.compareTo(grand)>0) throw new IllegalArgumentException("Advance cannot exceed estimate total");
        estimate.setInspectionCharge(money(zero(input.getInspectionCharge()))); estimate.setPartsAmount(money(parts)); estimate.setLabourAmount(money(zero(input.getLabourAmount()))); estimate.setDiscount(money(zero(input.getDiscount()))); estimate.setTaxableAmount(base); estimate.setCgst(money(tax.divide(BigDecimal.valueOf(2),2,RoundingMode.HALF_UP))); estimate.setSgst(money(tax.subtract(estimate.getCgst()))); estimate.setIgst(BigDecimal.ZERO); estimate.setGrandTotal(money(grand)); estimate.setAdvanceAmount(advance); estimate.setRemainingAmount(money(grand.subtract(advance))); estimate.setCustomerApprovalStatus(EstimateApprovalStatus.PENDING); estimate.setApprovedAt(null); estimateRepository.save(estimate);
        return response(r,true);
    }
    @Transactional
    public AfterSalesRequestResponse receive(String email, Long id, ServiceReceiveRequest input) {
        User seller=seller(email); ServiceRequest r=get(id); requireSeller(r,seller);
        if (r.getStatus()!=ServiceRequestStatus.APPROVED && r.getStatus()!=ServiceRequestStatus.ELIGIBILITY_CHECK) throw new IllegalArgumentException("Only an approved request can be received");
        r.setReceivingCondition(input.getReceivingCondition().trim()); r.setCustomerVisibleRemarks(blankToNull(input.getCustomerVisibleRemarks()));
        if (r.getPurchaseSerialId() != null && (r.getRequestType() == ServiceRequestType.RETURN || r.getRequestType() == ServiceRequestType.REFUND || r.getRequestType() == ServiceRequestType.REPLACEMENT || r.getRequestType() == ServiceRequestType.DOA_MANUFACTURING_DEFECT)) {
            changeSerialCondition(r, serialRepository.findById(r.getPurchaseSerialId()).orElseThrow(), "RETURN_PENDING", seller.getId(), "Received for return/replacement inspection");
        }
        move(r,ServiceRequestStatus.PRODUCT_RECEIVED,"Product received and condition recorded",seller.getId(),true); return response(r,true);
    }
    @Transactional
    public AfterSalesRequestResponse assignTechnician(String email, Long id, TechnicianAssignmentRequest input) {
        User seller=seller(email); ServiceRequest r=get(id); requireSeller(r,seller);
        r.setAssignedTechnicianId(input.getTechnicianId()); requestRepository.save(r);
        history(r,r.getStatus(),r.getStatus(),"Technician assigned: "+input.getTechnicianId()+(blankToNull(input.getRemarks())==null?"":"; "+input.getRemarks().trim()),seller.getId(),false); return response(r,true);
    }
    @Transactional
    public AfterSalesRequestResponse dispatch(String email, Long id, ServiceCenterDispatchRequest input) {
        User seller=seller(email); ServiceRequest r=get(id); requireSeller(r,seller);
        if (r.getStatus()!=ServiceRequestStatus.WARRANTY_CONFIRMED && r.getStatus()!=ServiceRequestStatus.INSPECTION_IN_PROGRESS) throw new IllegalArgumentException("Only a confirmed warranty request may be dispatched");
        ServiceCenterDispatch d=dispatchRepository.findByServiceRequestId(id).orElseGet(ServiceCenterDispatch::new); d.setServiceRequest(r); d.setDistributorId(input.getDistributorId());d.setServiceCenterName(input.getServiceCenterName().trim());d.setDispatchDate(input.getDispatchDate());d.setCourierName(blankToNull(input.getCourierName()));d.setTrackingNumber(blankToNull(input.getTrackingNumber()));d.setExpectedReturnDate(input.getExpectedReturnDate());d.setClaimNumber(blankToNull(input.getClaimNumber()));d.setClaimStatus(blankToNull(input.getClaimStatus()));d.setClaimAmount(money(zero(input.getClaimAmount())));d.setRemarks(blankToNull(input.getRemarks()));dispatchRepository.save(d);
        move(r,ServiceRequestStatus.SENT_TO_SERVICE_CENTER,"Dispatched to "+d.getServiceCenterName(),seller.getId(),true); return response(r,true);
    }
    @Transactional
    public AfterSalesRequestResponse processReplacement(String email, Long id, ReplacementRequest input) {
        User actor=seller(email); ServiceRequest r=get(id); requireSeller(r,actor);
        if (r.getStatus()!=ServiceRequestStatus.REPLACEMENT_APPROVED) throw new IllegalArgumentException("Replacement must be approved after inspection first");
        PurchaseItemSerial replacement=serialRepository.findById(input.getReplacementPurchaseSerialId()).orElseThrow(()->new IllegalArgumentException("Replacement serial not found"));
        if (r.getPurchaseSerialId()!=null && r.getPurchaseSerialId().equals(replacement.getId())) throw new IllegalArgumentException("Replacement serial must be a different unit");
        if (!"AVAILABLE".equalsIgnoreCase(replacement.getStatus()) || replacement.getSoldOrderItem()!=null || replacement.getSoldOfflineBillItem()!=null || replacement.getReservedOrderItem()!=null || replacement.getReservedOfflineBillItem()!=null) throw new IllegalArgumentException("Replacement serial is not available");
        if (!replacement.getPurchaseItem().getProduct().getId().equals(r.getProductId()) || replacement.getPurchaseItem().getPurchase().getSeller()==null || !replacement.getPurchaseItem().getPurchase().getSeller().getId().equals(r.getSellerId()) || replacement.getPurchaseItem().getPurchase().getStatus()!=PurchaseStatus.COMPLETED) throw new SecurityException("Replacement unit is not an available completed-purchase unit of this seller and product");
        ReturnReplacement record=new ReturnReplacement();record.setServiceRequest(r);record.setActionType(ReturnReplacementActionType.REPLACEMENT);record.setOldPurchaseSerialId(r.getPurchaseSerialId());record.setReplacementPurchaseSerialId(replacement.getId());record.setAdditionalPayment(money(zero(input.getAdditionalPayment())));record.setPriceDifference(record.getAdditionalPayment());record.setRefundableAmount(BigDecimal.ZERO);record.setInspectionResult(blankToNull(input.getInspectionResult()));returnReplacementRepository.save(record);
        if(r.getPurchaseSerialId()!=null){PurchaseItemSerial old=serialRepository.findById(r.getPurchaseSerialId()).orElseThrow();changeSerialCondition(r,old,r.getInspectionFinding()==AfterSalesInspectionFinding.MANUFACTURING_DEFECT?"DEFECTIVE":"RETURNED",actor.getId(),"Replaced by serial "+replacement.getId());}
        if(r.getOrderItemId()!=null) replacement.setSoldOrderItem(orderItemRepository.findById(r.getOrderItemId()).orElseThrow()); else replacement.setSoldOfflineBillItem(offlineBillItemRepository.findById(r.getOfflineBillItemId()).orElseThrow()); replacement.setStatus("SOLD");replacement.setSoldAt(LocalDateTime.now());serialRepository.save(replacement);
        Product product=productRepository.findById(r.getProductId()).orElseThrow();int before=product.getStock()==null?0:product.getStock();if(before<1)throw new IllegalStateException("Product stock is inconsistent with replacement serial availability");product.setStock(before-1);productRepository.save(product);User owner=userRepository.findById(r.getSellerId()).orElseThrow(()->new IllegalArgumentException("Seller not found"));stockMovementService.recordMovement(product,owner,"AFTER_SALES_REPLACEMENT",-1,before,before-1,"AFTER_SALES_REPLACEMENT",record.getId(),"Replacement for "+r.getRequestNumber());
        record.setCompletedAt(LocalDateTime.now());returnReplacementRepository.save(record);move(r,ServiceRequestStatus.REPLACED,"Replacement completed",actor.getId(),true);return response(r,true);
    }
    @Transactional
    public AfterSalesRequestResponse initiateRefund(String email, Long id, ServiceRefundRequest input) {
        User actor=seller(email);ServiceRequest r=get(id);requireSeller(r,actor);if(r.getStatus()!=ServiceRequestStatus.RETURN_APPROVED&&r.getStatus()!=ServiceRequestStatus.INSPECTION_PASSED&&r.getStatus()!=ServiceRequestStatus.APPROVED)throw new IllegalArgumentException("Refund must be approved after eligibility and inspection");
        BigDecimal original=itemAmount(r);BigDecimal already=r.getOrderItemId()!=null?zero(refundRepository.completedForOrderItem(r.getOrderItemId())):zero(refundRepository.completedForOfflineBillItem(r.getOfflineBillItemId()));BigDecimal remaining=original.subtract(already);BigDecimal requested=money(input.getRefundAmount());if(requested.compareTo(remaining)>0)throw new IllegalArgumentException("Refund amount exceeds the remaining refundable amount");
        PaymentTransaction payment=null;if(input.getPaymentTransactionId()!=null){payment=paymentTransactionRepository.findById(input.getPaymentTransactionId()).orElseThrow(()->new IllegalArgumentException("Original payment not found"));boolean owner=r.getOrderId()!=null&&r.getOrderId().equals(payment.getOrderId())||r.getOfflineBillId()!=null&&r.getOfflineBillId().equals(payment.getOfflineBillId());if(!owner||payment.getPaymentStatus()!=PaymentStatus.PAID)throw new SecurityException("Payment does not belong to this completed sale");}
        ServiceRefund refund=new ServiceRefund();refund.setServiceRequest(r);refund.setPaymentTransactionId(payment==null?null:payment.getId());refund.setRefundAmount(requested);refund.setRefundMethod(input.getRefundMethod());refund.setTransactionReference(blankToNull(input.getTransactionReference()));refund.setRefundStatus(ServiceRefundStatus.INITIATED);refundRepository.save(refund);move(r,ServiceRequestStatus.REFUND_INITIATED,"Refund initiated",actor.getId(),true);return response(r,true);
    }
    @Transactional
    public AfterSalesRequestResponse completeRefund(String email, Long id, Long refundId, RefundCompletionRequest input) {
        User actor=seller(email);ServiceRequest r=get(id);requireSeller(r,actor);if(r.getStatus()!=ServiceRequestStatus.REFUND_INITIATED)throw new IllegalArgumentException("Refund is not awaiting completion");ServiceRefund refund=refundRepository.findById(refundId).orElseThrow(()->new IllegalArgumentException("Refund not found"));if(!refund.getServiceRequest().getId().equals(id)||refund.getRefundStatus()!=ServiceRefundStatus.INITIATED)throw new IllegalArgumentException("Refund cannot be completed");refund.setTransactionReference(input.getTransactionReference().trim());refund.setRefundStatus(ServiceRefundStatus.COMPLETED);refund.setCompletedAt(LocalDateTime.now());refundRepository.save(refund);
        if(r.getPurchaseSerialId()!=null){PurchaseItemSerial serial=serialRepository.findById(r.getPurchaseSerialId()).orElseThrow();changeSerialCondition(r,serial,r.getInspectionFinding()==AfterSalesInspectionFinding.MANUFACTURING_DEFECT?"DEFECTIVE":"RETURNED",actor.getId(),"Refund completed");}
        generateCreditNote(r,refund.getRefundAmount());loyaltyService.reverseForAfterSalesRefund(r,refund.getRefundAmount(),itemAmount(r));
        if (r.getOfflineBillId() != null) financeService.stopForBill(r.getOfflineBillId(), "Offline bill refunded through after-sales request " + r.getRequestNumber());
        move(r,ServiceRequestStatus.REFUNDED,"Refund completed: "+refund.getTransactionReference(),actor.getId(),true);return response(r,true);
    }

    private Resource attachmentFile(ServiceRequest request, Long attachmentId) { ServiceAttachment attachment=attachmentRepository.findById(attachmentId).orElseThrow(()->new IllegalArgumentException("After-sales attachment not found"));if(!attachment.getServiceRequest().getId().equals(request.getId()))throw new SecurityException("Attachment does not belong to this request");return fileStorage.loadAfterSalesAttachment(attachment.getFileUrl()); }
    private ServiceAttachmentResponse saveAttachment(ServiceRequest request, Long actor, AfterSalesAttachmentType type, MultipartFile file) { ServiceAttachment attachment=new ServiceAttachment();attachment.setServiceRequest(request);attachment.setAttachmentType(type);attachment.setUploadedByUserId(actor);attachment.setFileUrl(fileStorage.storeAfterSalesAttachment(request.getId(),file));attachmentRepository.save(attachment);ServiceAttachmentResponse response=new ServiceAttachmentResponse();response.setId(attachment.getId());response.setAttachmentType(attachment.getAttachmentType());response.setFileUrl(attachment.getFileUrl());response.setUploadedAt(attachment.getUploadedAt());return response; }
    private void changeSerialCondition(ServiceRequest request, PurchaseItemSerial serial, String target, Long actor, String remarks) { String from=serial.getStatus(); if(target.equalsIgnoreCase(from))return;serial.setStatus(target);serialRepository.save(serial);AfterSalesStockMovement movement=new AfterSalesStockMovement();movement.setServiceRequest(request);movement.setPurchaseSerialId(serial.getId());movement.setFromStatus(from);movement.setToStatus(target);movement.setChangedByUserId(actor);movement.setRemarks(remarks);afterSalesStockRepository.save(movement); }
    private BigDecimal itemAmount(ServiceRequest r) { if(r.getOrderItemId()!=null)return money(orderItemRepository.findById(r.getOrderItemId()).orElseThrow(()->new IllegalArgumentException("Order item not found")).getTotalPrice()); return money(offlineBillItemRepository.findById(r.getOfflineBillItemId()).orElseThrow(()->new IllegalArgumentException("Offline bill item not found")).getTotalPrice()); }
    private void generateCreditNote(ServiceRequest r, BigDecimal amount) { if(creditNoteRepository.findByServiceRequestId(r.getId()).isPresent())return;AfterSalesCreditNote n=new AfterSalesCreditNote();n.setServiceRequest(r);n.setCreditNoteNumber("SH-CN-"+r.getRequestNumber());n.setGrandTotal(money(amount));if(r.getOfflineBillItemId()!=null){OfflineBillItem i=offlineBillItemRepository.findById(r.getOfflineBillItemId()).orElseThrow();BigDecimal ratio=amount.divide(i.getTotalPrice(),8,RoundingMode.HALF_UP);n.setTaxableAmount(money(i.getTaxableAmount().multiply(ratio)));n.setCgst(money(i.getCgst().multiply(ratio)));n.setSgst(money(i.getSgst().multiply(ratio)));n.setIgst(money(i.getIgst().multiply(ratio)));n.setOriginalReference(i.getOfflineBill().getBillNumber());}else{Order o=orderRepository.findById(r.getOrderId()).orElseThrow();BigDecimal tax=o.getGrandTotal().signum()==0?BigDecimal.ZERO:money(o.getTax().multiply(amount).divide(o.getGrandTotal(),2,RoundingMode.HALF_UP));n.setTaxableAmount(money(amount.subtract(tax)));n.setCgst(money(tax.divide(BigDecimal.valueOf(2),2,RoundingMode.HALF_UP)));n.setSgst(money(tax.subtract(n.getCgst())));n.setIgst(BigDecimal.ZERO);n.setOriginalReference(o.getOrderNumber());}creditNoteRepository.save(n); }
    private List<EligibleAfterSalesPurchaseResponse> toEligibleOrder(Order order, OrderItem item) { return eligibilityRows("ONLINE_ORDER",order.getId(),item.getId(),null,null,item.getProductId(),item.getProductName(),order.getOrderNumber(),item.getSellerId(),order.getCreatedAt().toLocalDate(),serialRepository.findAssignedForOrderItem(item.getId())); }
    private List<EligibleAfterSalesPurchaseResponse> toEligibleBill(OfflineBill bill, OfflineBillItem item) { return eligibilityRows("OFFLINE_BILL",null,null,bill.getId(),item.getId(),item.getProductId(),item.getProductName(),bill.getBillNumber(),bill.getSellerId(),bill.getCreatedAt().toLocalDate(),serialRepository.findSoldForOfflineBillItem(item.getId())); }
    private List<EligibleAfterSalesPurchaseResponse> eligibilityRows(String source,Long orderId,Long orderItemId,Long billId,Long billItemId,Long productId,String name,String invoice,Long sellerId,LocalDate sold,List<PurchaseItemSerial> serials) {
        PolicyEvaluation policy=policyFor(productId,sold); List<EligibleAfterSalesPurchaseResponse> rows=new ArrayList<>(); if (serials.isEmpty()) serials=List.of((PurchaseItemSerial)null);
        String image=productRepository.findById(productId).map(Product::getImageUrl).orElse(null);for(PurchaseItemSerial serial:serials){ EligibleAfterSalesPurchaseResponse r=new EligibleAfterSalesPurchaseResponse(); r.setSource(source);r.setOrderId(orderId);r.setOrderItemId(orderItemId);r.setOfflineBillId(billId);r.setOfflineBillItemId(billItemId);r.setProductId(productId);r.setProductName(name);r.setProductImageUrl(image);r.setInvoiceNumber(invoice);r.setSellerId(sellerId);r.setPurchaseDate(sold);r.setPurchaseSerialId(serial==null?null:serial.getId());r.setMaskedSerial(mask(serial));r.setWarrantyActive(policy.warrantyActive);r.setWarrantyEndDate(policy.warrantyEndDate);r.setReturnEligible(policy.returnEligible);r.setReplacementEligible(policy.replacementEligible);r.setServiceEligible(true);rows.add(r); } return rows;
    }
    private SaleReference resolveSale(User customer, CreateAfterSalesRequest in) {
        boolean online=in.getOrderId()!=null || in.getOrderItemId()!=null, offline=in.getOfflineBillId()!=null || in.getOfflineBillItemId()!=null; if (online==offline) throw new IllegalArgumentException("Select exactly one order item or offline bill item");
        if (online) { if(in.getOrderId()==null||in.getOrderItemId()==null) throw new IllegalArgumentException("Order and order item are required"); Order o=orderRepository.findById(in.getOrderId()).orElseThrow(() -> new IllegalArgumentException("Order not found")); if(!o.getCustomerId().equals(customer.getId())) throw new SecurityException("Order does not belong to the authenticated customer"); if(o.getOrderStatus()!=OrderStatus.DELIVERED||o.getPaymentStatus()!=PaymentStatus.PAID) throw new IllegalArgumentException("Only delivered, paid orders are eligible"); OrderItem item=orderItemRepository.findById(in.getOrderItemId()).orElseThrow(() -> new IllegalArgumentException("Order item not found")); if(!item.getOrder().getId().equals(o.getId())) throw new SecurityException("Order item does not belong to this order"); PurchaseItemSerial serial=verifySerial(in.getPurchaseSerialId(),item.getProductId(),item.getId(),null); return new SaleReference(o.getId(),item.getId(),null,null,item.getProductId(),item.getSellerId(),o.getCreatedAt().toLocalDate(),serial); }
        if(in.getOfflineBillId()==null||in.getOfflineBillItemId()==null) throw new IllegalArgumentException("Offline bill and bill item are required"); OfflineBill bill=offlineBillRepository.findById(in.getOfflineBillId()).orElseThrow(() -> new IllegalArgumentException("Offline bill not found")); if(!offlineOwned(bill,customer)) throw new SecurityException("Offline bill does not belong to the authenticated customer"); if(bill.getPaymentStatus()!=PaymentStatus.PAID) throw new IllegalArgumentException("Only paid offline bills are eligible"); OfflineBillItem item=offlineBillItemRepository.findById(in.getOfflineBillItemId()).orElseThrow(() -> new IllegalArgumentException("Offline bill item not found")); if(!item.getOfflineBill().getId().equals(bill.getId())) throw new SecurityException("Bill item does not belong to this bill"); PurchaseItemSerial serial=verifySerial(in.getPurchaseSerialId(),item.getProductId(),null,item.getId()); return new SaleReference(null,null,bill.getId(),item.getId(),item.getProductId(),bill.getSellerId(),bill.getCreatedAt().toLocalDate(),serial);
    }
    private PurchaseItemSerial verifySerial(Long id,Long productId,Long orderItemId,Long billItemId){ if(id==null)return null; PurchaseItemSerial s=serialRepository.findById(id).orElseThrow(()->new IllegalArgumentException("Serial/IMEI not found")); if(!s.getPurchaseItem().getProduct().getId().equals(productId)) throw new SecurityException("Serial does not match the purchased product"); boolean match=orderItemId!=null?s.getSoldOrderItem()!=null&&s.getSoldOrderItem().getId().equals(orderItemId):s.getSoldOfflineBillItem()!=null&&s.getSoldOfflineBillItem().getId().equals(billItemId); if(!match) throw new SecurityException("Serial does not belong to this sale item"); return s; }
    private boolean offlineOwned(OfflineBill b,User u){ return (b.getCustomerId()!=null&&b.getCustomerId().equals(u.getId())) || (u.getMobile()!=null&&u.getMobile().equals(b.getCustomerMobile())) || (u.getEmail()!=null&&u.getEmail().equalsIgnoreCase(b.getCustomerEmail())); }
    private PolicyEvaluation policyFor(Long productId,LocalDate saleDate){ Product p=productRepository.findById(productId).orElseThrow(()->new IllegalArgumentException("Product not found")); Long cat=p.getCategoryEntity()==null?null:p.getCategoryEntity().getId(); AfterSalesPolicy policy=policyRepository.findFirstByProductIdAndActiveTrueOrderByIdDesc(productId).or(()->cat==null?java.util.Optional.empty():policyRepository.findFirstByCategoryIdAndProductIdIsNullAndActiveTrueOrderByIdDesc(cat)).or(()->policyRepository.findFirstByCategoryIdIsNullAndProductIdIsNullAndActiveTrueOrderByIdDesc()).orElse(null); if(policy==null)return new PolicyEvaluation(false,null,false,false,false); LocalDate today=LocalDate.now(); LocalDate end=policy.getWarrantyMonths()>0?saleDate.plusMonths(policy.getWarrantyMonths()):null; return new PolicyEvaluation(end!=null&&!today.isAfter(end),end,policy.isReturnable()&&today.compareTo(saleDate.plusDays(policy.getReturnWindowDays()))<=0,today.compareTo(saleDate.plusDays(policy.getReplacementWindowDays()))<=0,today.compareTo(saleDate.plusDays(policy.getDoaWindowDays()))<=0); }
    private void validateEligibility(ServiceRequestType type,PolicyEvaluation p){ if(type==ServiceRequestType.WARRANTY_CLAIM&&!p.warrantyActive)throw new IllegalArgumentException("Warranty has expired or is unavailable"); if((type==ServiceRequestType.RETURN||type==ServiceRequestType.REFUND)&&!p.returnEligible)throw new IllegalArgumentException("The return window has expired or this product is not returnable"); if(type==ServiceRequestType.REPLACEMENT&&!p.replacementEligible)throw new IllegalArgumentException("The replacement window has expired"); if(type==ServiceRequestType.DOA_MANUFACTURING_DEFECT&&!p.doaEligible)throw new IllegalArgumentException("The DOA window has expired"); }
    private void move(ServiceRequest r,ServiceRequestStatus status,String remarks,Long actor,boolean visible){ transitionService.validate(r.getStatus(),status); ServiceRequestStatus before=r.getStatus();r.setStatus(status);if(status==ServiceRequestStatus.PRODUCT_RECEIVED)r.setReceivedDate(LocalDateTime.now());if(status==ServiceRequestStatus.CLOSED||status==ServiceRequestStatus.CANCELLED)r.setClosedDate(LocalDateTime.now());if(status==ServiceRequestStatus.COMPLETED||status==ServiceRequestStatus.DELIVERED||status==ServiceRequestStatus.REFUNDED||status==ServiceRequestStatus.REPLACED)r.setCompletedDate(LocalDateTime.now());requestRepository.save(r);history(r,before,status,remarks,actor,visible);AfterSalesNotificationType notification=notificationFor(status);eventPublisher.publishEvent(new AfterSalesNotificationEvent(r.getId(),notification==null?AfterSalesNotificationType.STATUS_UPDATED:notification,visible?remarks:null)); }
    private AfterSalesNotificationType notificationFor(ServiceRequestStatus status){return switch(status){case APPROVED->AfterSalesNotificationType.REQUEST_APPROVED;case REJECTED->AfterSalesNotificationType.REQUEST_REJECTED;case PRODUCT_RECEIVED->AfterSalesNotificationType.PRODUCT_RECEIVED;case PAID_REPAIR_APPROVAL_REQUIRED->AfterSalesNotificationType.ESTIMATE_READY;case ESTIMATE_APPROVED->AfterSalesNotificationType.ESTIMATE_APPROVED;case ESTIMATE_REJECTED->AfterSalesNotificationType.ESTIMATE_REJECTED;case SENT_TO_SERVICE_CENTER->AfterSalesNotificationType.SENT_TO_SERVICE_CENTER;case READY_FOR_DELIVERY->AfterSalesNotificationType.READY_FOR_PICKUP;case REFUND_INITIATED->AfterSalesNotificationType.REFUND_INITIATED;case REFUNDED->AfterSalesNotificationType.REFUND_COMPLETED;case REPLACED->AfterSalesNotificationType.REPLACEMENT_COMPLETED;case CLOSED,COMPLETED->AfterSalesNotificationType.REQUEST_CLOSED;default->null;};}
    private void history(ServiceRequest r,ServiceRequestStatus from,ServiceRequestStatus to,String remarks,Long actor,boolean visible){ ServiceStatusHistory h=new ServiceStatusHistory();h.setServiceRequest(r);h.setPreviousStatus(from);h.setNewStatus(to);h.setRemarks(blankToNull(remarks));h.setChangedByUserId(actor);h.setCustomerVisible(visible);historyRepository.save(h); }
    private AfterSalesRequestResponse customerResponse(ServiceRequest request) {
        AfterSalesRequestResponse response = response(request, false);
        // Diagnosis and internal notes are seller-only. Customers receive the separately managed visible remarks.
        response.setSellerDiagnosis(null);
        return response;
    }

    private AfterSalesRequestResponse response(ServiceRequest r,boolean internal){ AfterSalesRequestResponse out=new AfterSalesRequestResponse();out.setId(r.getId());out.setRequestNumber(r.getRequestNumber());out.setRequestType(r.getRequestType());out.setStatus(r.getStatus());out.setProductId(r.getProductId());out.setSellerId(r.getSellerId());out.setOrderId(r.getOrderId());out.setOrderItemId(r.getOrderItemId());out.setOfflineBillId(r.getOfflineBillId());out.setOfflineBillItemId(r.getOfflineBillItemId());out.setMaskedSerial(mask(r.getImei1Snapshot(),r.getImei2Snapshot(),r.getSerialNumberSnapshot()));out.setIssueCategory(r.getIssueCategory());out.setCustomerIssue(r.getCustomerIssue());out.setSellerDiagnosis(r.getSellerDiagnosis());out.setCustomerVisibleRemarks(r.getCustomerVisibleRemarks());out.setWarrantyEligible(r.isWarrantyEligible());out.setWarrantyEndDate(r.getWarrantyEndDate());out.setReturnEligible(r.isReturnEligible());out.setRequestDate(r.getRequestDate());out.setReceivedDate(r.getReceivedDate());out.setExpectedCompletionDate(r.getExpectedCompletionDate());out.setCompletedDate(r.getCompletedDate());out.setClosedDate(r.getClosedDate());out.setPickupType(r.getPickupType());out.setRejectionReason(r.getRejectionReason());out.setProductName(productRepository.findById(r.getProductId()).map(Product::getName).orElse("Product"));out.setHistory(historyRepository.findByServiceRequestIdOrderByChangedAtAsc(r.getId()).stream().filter(h->internal||h.isCustomerVisible()).map(h->{ServiceHistoryResponse x=new ServiceHistoryResponse();x.setPreviousStatus(h.getPreviousStatus());x.setNewStatus(h.getNewStatus());x.setRemarks(h.getRemarks());x.setChangedAt(h.getChangedAt());x.setCustomerVisible(h.isCustomerVisible());return x;}).toList());String attachmentBase=internal?"/api/seller/after-sales/requests/":"/api/customer/after-sales/requests/";out.setAttachments(attachmentRepository.findByServiceRequestIdOrderByUploadedAtAsc(r.getId()).stream().map(a->{ServiceAttachmentResponse x=new ServiceAttachmentResponse();x.setId(a.getId());x.setAttachmentType(a.getAttachmentType());x.setFileUrl(attachmentBase+r.getId()+"/attachments/"+a.getId()+"/file");x.setUploadedAt(a.getUploadedAt());return x;}).toList());out.setRefunds(refundRepository.findByServiceRequestIdOrderByInitiatedAtDesc(r.getId()).stream().map(f->{ServiceRefundResponse x=new ServiceRefundResponse();x.setId(f.getId());x.setRefundAmount(f.getRefundAmount());x.setRefundMethod(f.getRefundMethod());x.setRefundStatus(f.getRefundStatus());x.setTransactionReference(f.getTransactionReference());x.setInitiatedAt(f.getInitiatedAt());x.setCompletedAt(f.getCompletedAt());return x;}).toList());estimateRepository.findByServiceRequestId(r.getId()).ifPresent(e->out.setEstimate(mapEstimate(e)));out.setCustomerCanCancel(!internal&&(r.getStatus()==ServiceRequestStatus.REQUESTED||r.getStatus()==ServiceRequestStatus.UNDER_REVIEW||r.getStatus()==ServiceRequestStatus.ELIGIBILITY_CHECK));out.setCustomerCanDecideEstimate(!internal&&r.getStatus()==ServiceRequestStatus.PAID_REPAIR_APPROVAL_REQUIRED);return out; }
    private ServiceEstimateResponse mapEstimate(ServiceEstimate e){ServiceEstimateResponse out=new ServiceEstimateResponse();out.setInspectionCharge(e.getInspectionCharge());out.setPartsAmount(e.getPartsAmount());out.setLabourAmount(e.getLabourAmount());out.setDiscount(e.getDiscount());out.setTaxableAmount(e.getTaxableAmount());out.setCgst(e.getCgst());out.setSgst(e.getSgst());out.setIgst(e.getIgst());out.setGrandTotal(e.getGrandTotal());out.setAdvanceAmount(e.getAdvanceAmount());out.setRemainingAmount(e.getRemainingAmount());out.setCustomerApprovalStatus(e.getCustomerApprovalStatus());out.setApprovedAt(e.getApprovedAt());out.setParts(partRepository.findByServiceRequestId(e.getServiceRequest().getId()).stream().map(p->{ServicePartRequest x=new ServicePartRequest();x.setPartName(p.getPartName());x.setPartNumber(p.getPartNumber());x.setQuantity(p.getQuantity());x.setUnitPrice(p.getUnitPrice());x.setGstRate(p.getGstRate());x.setWarrantyMonths(p.getWarrantyMonths());return x;}).toList());return out;}
    private AfterSalesPolicyResponse policyResponse(AfterSalesPolicy p){AfterSalesPolicyResponse r=new AfterSalesPolicyResponse();r.setId(p.getId());r.setCategoryId(p.getCategoryId());r.setProductId(p.getProductId());r.setWarrantyType(p.getWarrantyType());r.setWarrantyMonths(p.getWarrantyMonths());r.setReturnable(p.isReturnable());r.setReturnWindowDays(p.getReturnWindowDays());r.setReplacementWindowDays(p.getReplacementWindowDays());r.setDoaWindowDays(p.getDoaWindowDays());r.setPhysicalDamageAllowed(p.isPhysicalDamageAllowed());r.setLiquidDamageAllowed(p.isLiquidDamageAllowed());r.setOpenedBoxReturnAllowed(p.isOpenedBoxReturnAllowed());r.setChangeOfMindAllowed(p.isChangeOfMindAllowed());r.setRequiredEvidence(p.getRequiredEvidence());r.setPolicyTerms(p.getPolicyTerms());r.setActive(p.isActive());return r;}
    private ServiceRequest get(Long id){return requestRepository.findById(id).orElseThrow(()->new IllegalArgumentException("After-sales request not found"));} private User customer(String email){User u=user(email);if(u.getRole()!=Role.CUSTOMER)throw new SecurityException("Customer access is required");return u;}private User seller(String email){User u=user(email);if(u.getRole()!=Role.SELLER&&u.getRole()!=Role.ADMIN)throw new SecurityException("Seller access is required");return u;}private User admin(String email){User u=user(email);if(u.getRole()!=Role.ADMIN)throw new SecurityException("Admin access is required");return u;}private User user(String email){return userRepository.findByEmailIgnoreCase(email).orElseThrow(()->new SecurityException("Authenticated user not found"));}private void requireCustomer(ServiceRequest r,User u){if(!r.getCustomerId().equals(u.getId()))throw new SecurityException("This request does not belong to the authenticated customer");}private void requireSeller(ServiceRequest r,User u){if(u.getRole()!=Role.ADMIN&&!u.getId().equals(r.getSellerId()))throw new SecurityException("This request does not belong to the authenticated seller");}
    private String nextRequestNumber(){return "SH-AS-"+DateTimeFormatter.ofPattern("yyyyMMddHHmmss",Locale.ROOT).format(LocalDateTime.now())+"-"+UUID.randomUUID().toString().substring(0,6).toUpperCase(Locale.ROOT);} private String blankToNull(String s){return s==null||s.isBlank()?null:s.trim();}private BigDecimal zero(BigDecimal n){return n==null?BigDecimal.ZERO:n;}private BigDecimal money(BigDecimal n){return n.setScale(2,RoundingMode.HALF_UP);}private String mask(PurchaseItemSerial s){return s==null?null:mask(s.getImei1(),s.getImei2(),s.getSerialNumber());}private String mask(String i1,String i2,String serial){String raw=i1!=null?i1:(i2!=null?i2:serial);if(raw==null||raw.isBlank())return null;return raw.length()<=4?"****":"••••"+raw.substring(raw.length()-4);} private record SaleReference(Long orderId,Long orderItemId,Long billId,Long billItemId,Long productId,Long sellerId,LocalDate saleDate,PurchaseItemSerial serial){} private record PolicyEvaluation(boolean warrantyActive,LocalDate warrantyEndDate,boolean returnEligible,boolean replacementEligible,boolean doaEligible){}
}
