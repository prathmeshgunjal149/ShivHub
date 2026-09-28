package com.shivhub.backend.service;

import java.util.EnumSet;
import com.shivhub.backend.dto.*;
import com.shivhub.backend.entity.*;
import com.shivhub.backend.enums.*;
import com.shivhub.backend.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Separate inspected-return extension; existing mobile/IMEI return processing is untouched. */
@Service @RequiredArgsConstructor
public class VariantReturnService {
    private final UserRepository users;
    private final ServiceRequestRepository requests;
    private final OrderItemRepository orders;
    private final OfflineBillItemRepository bills;
    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final AfterSalesStockMovementRepository ledger;
    private final ServiceStatusHistoryRepository history;
    private final EntityManager em;
    private final StockMovementService movements;

    public record ReturnInfo(boolean variantProduct, String selectedAttributes, int remainingQuantity,
            String disposition, boolean stockRestored) {}
    private record Sale(Long variantId, Long productId, Long sellerId, int quantity, String attributes) {}

    @Transactional public ReturnInfo info(String email, Long requestId) {
        ServiceRequest r = owned(email, requestId); Sale sale = sale(r);
        if (sale.variantId() == null) return new ReturnInfo(false, null, 0, null, false);
        var existing = ledger.findByServiceRequestIdAndProductVariantId(requestId, sale.variantId());
        long disposed = ledger.disposedQuantity(sale.variantId(), r.getOrderItemId(), r.getOfflineBillItemId());
        return new ReturnInfo(true, sale.attributes(), Math.max(0, sale.quantity() - (int) disposed),
                existing.map(AfterSalesStockMovement::getToStatus).orElse(null), existing.map(AfterSalesStockMovement::isStockRestored).orElse(false));
    }

    @Transactional public VariantReturnDispositionResponse dispose(String email, Long id, VariantReturnDispositionRequest input) {
        ServiceRequest r = owned(email, id); Sale sale = sale(r);
        if (sale.variantId() == null) fail("This sale uses its existing product/IMEI return flow");
        if (!EnumSet.of(ServiceRequestType.RETURN, ServiceRequestType.REFUND, ServiceRequestType.REPLACEMENT,
                ServiceRequestType.DOA_MANUFACTURING_DEFECT).contains(r.getRequestType())) fail("Only a physical return/replacement can change stock");
        boolean inspected = EnumSet.of(ServiceRequestStatus.INSPECTION_PASSED, ServiceRequestStatus.INSPECTION_FAILED,
                ServiceRequestStatus.RETURN_APPROVED, ServiceRequestStatus.REPLACEMENT_APPROVED,
                ServiceRequestStatus.REFUNDED, ServiceRequestStatus.REPLACED, ServiceRequestStatus.COMPLETED).contains(r.getStatus());
        if (!inspected || r.getInspectionFinding() == null) fail("Record product receiving and inspection before disposing returned stock");
        if (input.getDisposition() == VariantReturnDisposition.AVAILABLE && r.getStatus() == ServiceRequestStatus.INSPECTION_FAILED)
            fail("An inspection-failed unit cannot be made available");
        if (input.getQuantity() == null || input.getQuantity() < 1 || input.getDisposition() == null) fail("Valid disposition and quantity are required");
        if (ledger.existsByServiceRequestIdAndProductVariantId(id, sale.variantId()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This request already has a recorded variant disposition");
        long disposed = ledger.disposedQuantity(sale.variantId(), r.getOrderItemId(), r.getOfflineBillItemId());
        if (input.getQuantity() > sale.quantity() - disposed) fail("Return quantity exceeds the remaining original sale quantity");
        Product p = products.findById(sale.productId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (ProductConfigurationService.isMobile(p) || !p.isVariantsEnabled() || p.getSeller() == null
                || !p.getSeller().getId().equals(r.getSellerId())) fail("Original sale/variant ownership is inconsistent");
        em.refresh(p, LockModeType.PESSIMISTIC_WRITE);
        ProductVariant v = variants.findLocked(sale.variantId(), p.getId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Original sale variant was not found"));
        boolean restore = input.getDisposition() == VariantReturnDisposition.AVAILABLE;
        if (restore && !v.isActive()) fail("Activate the original variant before restoring sellable stock");
        if (restore) {
            int before = p.getStock(); v.setStockQuantity(Math.addExact(v.getStockQuantity(), input.getQuantity()));
            variants.flush(); var all = variants.findByProductOrderByIdAsc(p);
            p.setStock(all.stream().filter(ProductVariant::isActive).mapToInt(ProductVariant::getStockQuantity).sum());
            p.setReservedStock(all.stream().filter(ProductVariant::isActive).mapToInt(ProductVariant::getReservedQuantity).sum());
            StockMovement movement = movements.recordMovement(p, p.getSeller(), "RETURN", input.getQuantity(), before,
                    p.getStock(), "AFTER_SALES", id, "Inspected return of original variant " + v.getId());
            movement.setProductVariantId(v.getId());
        }
        r.setProductVariantId(v.getId());
        AfterSalesStockMovement entry = new AfterSalesStockMovement(); entry.setServiceRequest(r);
        entry.setProductVariantId(v.getId()); entry.setQuantity(input.getQuantity()); entry.setStockRestored(restore);
        entry.setFromStatus("RETURN_PENDING"); entry.setToStatus(input.getDisposition().name());
        User actor = users.findByEmailIgnoreCase(email).orElseThrow(); entry.setChangedByUserId(actor.getId());
        entry.setRemarks(input.getRemarks()); ledger.save(entry);
        ServiceStatusHistory audit = new ServiceStatusHistory(); audit.setServiceRequest(r);
        audit.setPreviousStatus(r.getStatus()); audit.setNewStatus(r.getStatus()); audit.setChangedByUserId(actor.getId());
        audit.setCustomerVisible(false); audit.setRemarks("Returned variant stock: " + input.getDisposition() + "; quantity " + input.getQuantity() + "; " + (input.getRemarks() == null ? "" : input.getRemarks())); history.save(audit);
        var response = new VariantReturnDispositionResponse(); response.setRequestId(id); response.setVariantId(v.getId());
        response.setDisposition(input.getDisposition()); response.setQuantity(input.getQuantity()); response.setStockRestored(restore);
        response.setAvailableStock(v.getAvailableStock()); return response;
    }
    private ServiceRequest owned(String email, Long id) {
        User u = users.findByEmailIgnoreCase(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (u.getRole() != Role.SELLER && u.getRole() != Role.ADMIN) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        ServiceRequest r = requests.findLockedById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (u.getRole() != Role.ADMIN && !u.getId().equals(r.getSellerId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This is another seller's request");
        return r;
    }
    private Sale sale(ServiceRequest r) {
        Sale sale;
        if (r.getOrderItemId() != null) {
            OrderItem item = orders.findById(r.getOrderItemId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            em.refresh(item, LockModeType.PESSIMISTIC_WRITE);
            sale = new Sale(item.getProductVariantId(), item.getProductId(), item.getSellerId(), item.getQuantity(), item.getSelectedAttributes());
        } else {
            if (r.getOfflineBillItemId() == null) fail("Request has no original sale item");
            OfflineBillItem item = bills.findById(r.getOfflineBillItemId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            em.refresh(item, LockModeType.PESSIMISTIC_WRITE);
            sale = new Sale(item.getProductVariantId(), item.getProductId(), item.getOfflineBill().getSellerId(), item.getQuantity(), item.getSelectedAttributes());
        }
        if (sale.productId() == null || sale.sellerId() == null || !sale.productId().equals(r.getProductId()) || !sale.sellerId().equals(r.getSellerId())) fail("Original sale ownership does not match the request");
        return sale;
    }
    private static void fail(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
