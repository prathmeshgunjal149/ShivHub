package com.shivhub.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shivhub.backend.dto.ConfirmDistributorAdjustmentRequest;
import com.shivhub.backend.dto.CreateStockTransferAdjustmentRequest;
import com.shivhub.backend.dto.DistributorCreditNoteRequest;
import com.shivhub.backend.dto.StockTransferAdjustmentResponse;
import com.shivhub.backend.entity.DistributorCreditNote;
import com.shivhub.backend.entity.Product;
import com.shivhub.backend.entity.Purchase;
import com.shivhub.backend.entity.PurchaseItem;
import com.shivhub.backend.entity.PurchaseItemSerial;
import com.shivhub.backend.entity.SellerDistributor;
import com.shivhub.backend.entity.StockTransferAdjustment;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.repository.ProductRepository;
import com.shivhub.backend.repository.PurchaseItemSerialRepository;
import com.shivhub.backend.repository.SellerDistributorRepository;
import com.shivhub.backend.repository.StockTransferAdjustmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockTransferAdjustmentService {

    private final StockTransferAdjustmentRepository repository;
    private final SellerDistributorRepository sellerDistributorRepository;
    private final PurchaseItemSerialRepository serialRepository;
    private final ProductRepository productRepository;
    private final DistributorCreditNoteService creditNoteService;
    private final StockMovementService stockMovementService;

    @Transactional
    public StockTransferAdjustmentResponse create(
            CreateStockTransferAdjustmentRequest request,
            User seller) {

        validateSeller(seller);
        if (request == null) throw new RuntimeException("Transfer request is required");
        if (request.getReceivingShopName() == null || request.getReceivingShopName().isBlank()) {
            throw new RuntimeException("Receiving shop name is required");
        }
        if (request.getAdjustmentAmount() == null || request.getAdjustmentAmount().signum() <= 0) {
            throw new RuntimeException("Adjustment amount must be greater than zero");
        }

        SellerDistributor sellerDistributor =
                sellerDistributorRepository.findById(request.getSellerDistributorId())
                        .orElseThrow(() -> new RuntimeException("Distributor assignment not found"));

        if (sellerDistributor.getSeller() == null
                || !seller.getId().equals(sellerDistributor.getSeller().getId())) {
            throw new RuntimeException("Selected distributor does not belong to this seller");
        }

        PurchaseItemSerial serial =
                serialRepository.findById(request.getSerialId())
                        .orElseThrow(() -> new RuntimeException("IMEI/serial not found"));

        validateTransferableSerial(serial, sellerDistributor, seller);

        PurchaseItem purchaseItem = serial.getPurchaseItem();
        Purchase purchase = purchaseItem.getPurchase();
        Product product = purchaseItem.getProduct();

        StockTransferAdjustment transfer = new StockTransferAdjustment();
        transfer.setSeller(seller);
        transfer.setSellerDistributor(sellerDistributor);
        transfer.setPurchase(purchase);
        transfer.setProduct(product);
        transfer.setSerial(serial);
        transfer.setReceivingShopName(request.getReceivingShopName().trim());
        transfer.setReceivingShopContact(blankToNull(request.getReceivingShopContact()));
        transfer.setReceivingShopAddress(blankToNull(request.getReceivingShopAddress()));
        transfer.setAdjustmentAmount(request.getAdjustmentAmount().setScale(2, RoundingMode.HALF_UP));
        transfer.setDistributorReference(blankToNull(request.getDistributorReference()));
        transfer.setInstructionAttachmentUrl(blankToNull(request.getInstructionAttachmentUrl()));
        transfer.setNotes(blankToNull(request.getNotes()));
        transfer.setStatus("DRAFT");

        return toResponse(repository.save(transfer));
    }

    @Transactional
    public StockTransferAdjustmentResponse confirmHandover(Long transferId, User seller) {
        validateSeller(seller);
        StockTransferAdjustment transfer = getSellerTransfer(transferId, seller);

        if (!"DRAFT".equals(transfer.getStatus())) {
            throw new RuntimeException("Only draft transfers can be handed over");
        }

        PurchaseItemSerial serial = transfer.getSerial();
        validateTransferableSerial(serial, transfer.getSellerDistributor(), seller);

        Product product = transfer.getProduct();
        int stockBefore = product.getStock() == null ? 0 : product.getStock();
        int stockAfter = stockBefore - 1;
        if (stockAfter < 0) throw new RuntimeException("Insufficient stock for transfer");

        product.setStock(stockAfter);
        productRepository.save(product);

        transfer.setStatus("HANDOVER_CONFIRMED");
        transfer.setHandoverConfirmedAt(LocalDateTime.now());
        StockTransferAdjustment saved = repository.save(transfer);

        serial.setStatus("TRANSFERRED_OUT");
        serial.setTransferAdjustment(saved);
        serialRepository.save(serial);

        stockMovementService.recordMovement(
                product,
                seller,
                "DISTRIBUTOR_DIRECTED_TRANSFER",
                -1,
                stockBefore,
                stockAfter,
                "STOCK_TRANSFER_ADJUSTMENT",
                saved.getId(),
                "Transferred to " + saved.getReceivingShopName()
                        + " under distributor instruction " + safe(saved.getDistributorReference())
        );

        return toResponse(saved);
    }

    @Transactional
    public StockTransferAdjustmentResponse confirmAdjustment(
            Long transferId,
            ConfirmDistributorAdjustmentRequest request,
            User seller) {

        validateSeller(seller);
        StockTransferAdjustment transfer = getSellerTransfer(transferId, seller);

        if (!"HANDOVER_CONFIRMED".equals(transfer.getStatus())) {
            throw new RuntimeException("Confirm handover before distributor adjustment");
        }
        if (transfer.getCreditNote() != null || transfer.getAdjustmentConfirmedAt() != null) {
            throw new RuntimeException("Distributor adjustment is already confirmed for this transfer");
        }

        String creditNoteNumber =
                request == null || request.getCreditNoteNumber() == null || request.getCreditNoteNumber().isBlank()
                        ? "ST-ADJ-" + transfer.getId()
                        : request.getCreditNoteNumber().trim();

        DistributorCreditNoteRequest creditNoteRequest = new DistributorCreditNoteRequest();
        creditNoteRequest.setPurchaseId(transfer.getPurchase().getId());
        creditNoteRequest.setCreditNoteNumber(creditNoteNumber);
        creditNoteRequest.setCreditNoteDate(
                request == null || request.getCreditNoteDate() == null
                        ? LocalDate.now()
                        : request.getCreditNoteDate()
        );
        creditNoteRequest.setAmount(transfer.getAdjustmentAmount());
        creditNoteRequest.setGstAmount(BigDecimal.ZERO);
        creditNoteRequest.setAttachmentUrl(request == null ? null : request.getAttachmentUrl());
        creditNoteRequest.setReason("Distributor-directed stock transfer to " + transfer.getReceivingShopName());
        creditNoteRequest.setRemarks(request == null ? transfer.getNotes() : request.getRemarks());

        DistributorCreditNote creditNote = creditNoteService.add(creditNoteRequest, seller);

        transfer.setCreditNote(creditNote);
        transfer.setCreditNoteNumber(creditNote.getCreditNoteNumber());
        transfer.setCreditNoteDate(creditNote.getCreditNoteDate());
        transfer.setCreditNoteAttachmentUrl(creditNote.getAttachmentUrl());
        transfer.setAdjustmentConfirmedAt(LocalDateTime.now());
        transfer.setStatus("ADJUSTMENT_CONFIRMED");

        return toResponse(repository.save(transfer));
    }

    @Transactional(readOnly = true)
    public List<StockTransferAdjustmentResponse> list(User seller, String status) {
        validateSeller(seller);
        List<StockTransferAdjustment> transfers =
                status == null || status.isBlank()
                        ? repository.findBySellerOrderByCreatedAtDesc(seller)
                        : repository.findBySellerAndStatusOrderByCreatedAtDesc(seller, status.trim().toUpperCase());
        return transfers.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public StockTransferAdjustmentResponse get(Long transferId, User seller) {
        validateSeller(seller);
        return toResponse(getSellerTransfer(transferId, seller));
    }

    private StockTransferAdjustment getSellerTransfer(Long transferId, User seller) {
        if (transferId == null) throw new RuntimeException("Transfer ID is required");
        StockTransferAdjustment transfer =
                repository.findById(transferId)
                        .orElseThrow(() -> new RuntimeException("Stock transfer not found"));
        if (transfer.getSeller() == null || !seller.getId().equals(transfer.getSeller().getId())) {
            throw new RuntimeException("You are not authorized to access this transfer");
        }
        return transfer;
    }

    private void validateTransferableSerial(
            PurchaseItemSerial serial,
            SellerDistributor sellerDistributor,
            User seller) {

        if (serial.getPurchaseItem() == null
                || serial.getPurchaseItem().getPurchase() == null
                || serial.getPurchaseItem().getProduct() == null) {
            throw new RuntimeException("IMEI/serial purchase history is incomplete");
        }

        Purchase purchase = serial.getPurchaseItem().getPurchase();
        if (purchase.getSeller() == null || !seller.getId().equals(purchase.getSeller().getId())) {
            throw new RuntimeException("IMEI/serial does not belong to this seller");
        }
        if (purchase.getDistributor() == null
                || !purchase.getDistributor().getId().equals(sellerDistributor.getId())) {
            throw new RuntimeException("IMEI/serial was not purchased from the selected distributor");
        }
        if (!serial.isAvailable()
                || serial.getSoldOfflineBillItem() != null
                || serial.getReservedOrderItem() != null
                || serial.getSoldOrderItem() != null
                || serial.getTransferAdjustment() != null) {
            throw new RuntimeException("Selected IMEI/serial is not available for transfer");
        }
    }

    private void validateSeller(User seller) {
        if (seller == null || seller.getId() == null) {
            throw new RuntimeException("Valid seller is required");
        }
        if (seller.getRole() != Role.SELLER || !seller.isEnabled()) {
            throw new RuntimeException("Only active sellers can manage stock transfers");
        }
    }

    private StockTransferAdjustmentResponse toResponse(StockTransferAdjustment transfer) {
        SellerDistributor sellerDistributor = transfer.getSellerDistributor();
        String distributorName =
                sellerDistributor != null && sellerDistributor.getDistributor() != null
                        ? sellerDistributor.getDistributor().getBusinessName()
                        : "Distributor";
        Purchase purchase = transfer.getPurchase();
        Product product = transfer.getProduct();
        PurchaseItemSerial serial = transfer.getSerial();

        return new StockTransferAdjustmentResponse(
                transfer.getId(),
                transfer.getStatus(),
                sellerDistributor == null ? null : sellerDistributor.getId(),
                distributorName,
                purchase == null ? null : purchase.getId(),
                purchase == null ? null : purchase.getInvoiceNumber(),
                product == null ? null : product.getId(),
                product == null ? null : product.getName(),
                serial == null ? null : serial.getId(),
                serial == null ? "-" : serialDisplay(serial),
                transfer.getReceivingShopName(),
                transfer.getReceivingShopContact(),
                transfer.getReceivingShopAddress(),
                transfer.getAdjustmentAmount(),
                transfer.getDistributorReference(),
                transfer.getInstructionAttachmentUrl(),
                transfer.getNotes(),
                transfer.getCreditNote() == null ? null : transfer.getCreditNote().getId(),
                transfer.getCreditNoteNumber(),
                transfer.getCreditNoteDate(),
                transfer.getCreditNoteAttachmentUrl(),
                transfer.getHandoverConfirmedAt(),
                transfer.getAdjustmentConfirmedAt(),
                transfer.getCreatedAt(),
                transfer.getUpdatedAt()
        );
    }

    private String serialDisplay(PurchaseItemSerial serial) {
        if (serial.getImei1() != null && !serial.getImei1().isBlank()) return serial.getImei1();
        if (serial.getSerialNumber() != null && !serial.getSerialNumber().isBlank()) return serial.getSerialNumber();
        if (serial.getImei2() != null && !serial.getImei2().isBlank()) return serial.getImei2();
        return String.valueOf(serial.getId());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
