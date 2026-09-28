package com.shivhub.backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shivhub.backend.dto.DistributorRequest;
import com.shivhub.backend.dto.DistributorResponse;
import com.shivhub.backend.entity.Distributor;
import com.shivhub.backend.entity.Purchase;
import com.shivhub.backend.entity.PurchasePayment;
import com.shivhub.backend.entity.SellerDistributor;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.repository.DistributorCreditNoteRepository;
import com.shivhub.backend.repository.DistributorRepository;
import com.shivhub.backend.repository.PurchasePaymentRepository;
import com.shivhub.backend.repository.PurchaseRepository;
import com.shivhub.backend.repository.SellerDistributorRepository;
import com.shivhub.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DistributorService {

    private final DistributorRepository distributorRepository;
    private final UserRepository userRepository;
    private final SellerDistributorRepository sellerDistributorRepository;
    private final PurchaseRepository purchaseRepository;
    private final PurchasePaymentRepository purchasePaymentRepository;
    private final DistributorCreditNoteRepository creditNoteRepository;
    private final EmailService emailService;


    // =========================================================
    // CREATE DISTRIBUTOR
    // =========================================================

    public DistributorResponse createDistributor(
            DistributorRequest request) {

        String gstin = normalize(request.getGstin());

        if (gstin != null
                && distributorRepository.existsByGstin(gstin)) {

            throw new RuntimeException(
                    "Distributor with this GSTIN already exists"
            );
        }

        Distributor distributor = new Distributor();

        distributor.setBusinessName(
                normalizeRequired(request.getBusinessName())
        );

        distributor.setContactPerson(
                normalize(request.getContactPerson())
        );

        distributor.setMobile(
                normalizeRequired(request.getMobile())
        );

        distributor.setEmail(
                normalize(request.getEmail())
        );

        applyExtendedDetails(distributor, request);

        distributor.setGstin(gstin);

        distributor.setAddress(
                normalize(request.getAddress())
        );

        distributor.setCity(
                normalize(request.getCity())
        );

        distributor.setState(
                normalize(request.getState())
        );

        distributor.setPincode(
                normalize(request.getPincode())
        );

        distributor.setBrands(normalize(request.getBrands()) == null ? "ALL_BRANDS" : normalize(request.getBrands()));

        String status = normalize(request.getStatus());

        if (status == null) {
            status = "ACTIVE";
        }

        distributor.setStatus(
                status.toUpperCase()
        );

        Distributor savedDistributor =
                distributorRepository.save(distributor);

        return mapToResponse(savedDistributor);
    }


    // =========================================================
    // SELLER SUBMIT DISTRIBUTOR FOR APPROVAL
    // =========================================================

    /**
     * Seller-created distributors wait for Admin approval.
     *
     * Flow:
     *
     * Seller
     *   ↓
     * Distributor created
     *   ↓
     * PENDING
     *   ↓
     * Admin approval required
     */
    public DistributorResponse submitForApproval(
            String sellerEmail,
            DistributorRequest request) {

        User seller = seller(sellerEmail);

        String gstin =
                normalize(request.getGstin());

        if (gstin != null
                && distributorRepository.existsByGstin(gstin)) {

            throw new RuntimeException(
                    "Distributor with this GSTIN already exists"
            );
        }

        Distributor distributor =
                fromRequest(
                        request,
                        "PENDING"
                );

        distributor.setSubmittedBySeller(
                seller
        );

        Distributor saved =
                distributorRepository.save(
                        distributor
                );

        try {

            emailService.sendDistributorWorkflowEmail(
                    seller.getEmail(),
                    "Distributor request received",
                    "Your request for "
                            + saved.getBusinessName()
                            + " is waiting for ShivHub Admin approval."
            );

        } catch (Exception ignored) {
        }

        return mapToResponse(saved);
    }


    // =========================================================
    // ADMIN CREATE DISTRIBUTOR
    // =========================================================

    /**
     * Distributor created directly by Admin.
     *
     * Admin-created distributors are immediately available
     * to all active sellers.
     */
    public DistributorResponse createByAdmin(
            DistributorRequest request) {

        DistributorResponse result =
                createDistributor(request);

        Distributor distributor =
                distributorRepository
                        .findById(result.getId())
                        .orElseThrow();

        distributor.setStatus("ACTIVE");

        distributor.setAdminReview(
                "Created by ShivHub Admin"
        );

        distributor.setReviewedAt(
                LocalDateTime.now()
        );

        distributor =
                distributorRepository.save(
                        distributor
                );

        /*
         * IMPORTANT:
         *
         * Automatically create SellerDistributor
         * relationship for every active seller.
         */
        makeAvailableToAllSellers(
                distributor
        );

        try {

            emailService.sendDistributorWorkflowEmail(
                    distributor.getEmail(),
                    "Distributor account activated",
                    "Your distributor profile is now active on ShivHub and visible to sellers."
            );

        } catch (Exception ignored) {
        }

        return mapToResponse(distributor);
    }


    // =========================================================
    // GET PENDING REQUESTS
    // =========================================================

    @Transactional(readOnly = true)
    public List<DistributorResponse> getPendingRequests() {

        return distributorRepository
                .findByStatus("PENDING")
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    // =========================================================
    // APPROVE REQUEST
    // =========================================================

    public DistributorResponse approveRequest(
            Long id,
            String review) {

        return reviewRequest(
                id,
                "ACTIVE",
                review,
                "Distributor request approved",
                "Your distributor request has been approved and is now visible to all ShivHub sellers."
        );
    }


    // =========================================================
    // REJECT REQUEST
    // =========================================================

    public DistributorResponse rejectRequest(
            Long id,
            String review) {

        String body =
                "Your distributor request was not approved."
                        + (
                        review == null || review.isBlank()
                                ? ""
                                : " Reason: " + review
                );

        return reviewRequest(
                id,
                "REJECTED",
                review,
                "Distributor request update",
                body
        );
    }


    // =========================================================
    // ADMIN DEACTIVATE
    // =========================================================

    public DistributorResponse adminDeactivate(
            Long id) {

        Distributor distributor =
                distributorRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Distributor not found"
                                )
                        );

        distributor.setStatus("INACTIVE");

        Distributor updatedDistributor =
                distributorRepository.save(
                        distributor
                );

        notifyDistributor(
                updatedDistributor,
                "Distributor deactivated",
                "Your distributor profile has been deactivated by ShivHub Admin."
        );

        return mapToResponse(
                updatedDistributor
        );
    }


    // =========================================================
    // ADMIN DELETE
    // =========================================================

    public void adminDelete(Long id) {

        Distributor distributor =
                distributorRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Distributor not found"
                                )
                        );

        notifyDistributor(
                distributor,
                "Distributor removed",
                "Your distributor profile has been removed by ShivHub Admin."
        );

        distributorRepository.delete(
                distributor
        );
    }


    // =========================================================
    // REVIEW REQUEST
    // =========================================================

    private DistributorResponse reviewRequest(
            Long id,
            String status,
            String review,
            String subject,
            String body) {

        Distributor distributor =
                distributorRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Distributor request not found"
                                )
                        );

        /*
         * Only PENDING requests can be reviewed.
         */
        if (!"PENDING".equalsIgnoreCase(
                distributor.getStatus())) {

            throw new RuntimeException(
                    "Only pending distributor requests can be reviewed"
            );
        }

        distributor.setStatus(
                status
        );

        distributor.setAdminReview(
                blank(review)
        );

        distributor.setReviewedAt(
                LocalDateTime.now()
        );

        distributor =
                distributorRepository.save(
                        distributor
                );


        /*
         * IMPORTANT:
         *
         * When Admin approves distributor,
         * automatically make it available to
         * all active sellers.
         */
        if ("ACTIVE".equalsIgnoreCase(status)) {

            makeAvailableToAllSellers(
                    distributor
            );
        }


        /*
         * Notify distributor.
         */
        notifyDistributor(
                distributor,
                subject,
                body
        );


        /*
         * Notify seller who submitted the distributor.
         */
        if (distributor.getSubmittedBySeller() != null) {

            try {

                emailService.sendDistributorWorkflowEmail(
                        distributor
                                .getSubmittedBySeller()
                                .getEmail(),
                        subject,
                        body
                );

            } catch (Exception ignored) {
            }
        }

        return mapToResponse(
                distributor
        );
    }


    // =========================================================
    // CREATE DISTRIBUTOR ENTITY FROM REQUEST
    // =========================================================

    private Distributor fromRequest(
            DistributorRequest request,
            String status) {

        Distributor distributor =
                new Distributor();

        distributor.setBusinessName(
                normalizeRequired(
                        request.getBusinessName()
                )
        );

        distributor.setContactPerson(
                normalize(
                        request.getContactPerson()
                )
        );

        distributor.setMobile(
                normalizeRequired(
                        request.getMobile()
                )
        );

        distributor.setEmail(
                normalize(
                        request.getEmail()
                )
        );

        applyExtendedDetails(distributor, request);

        distributor.setGstin(
                normalize(
                        request.getGstin()
                )
        );

        distributor.setAddress(
                normalize(
                        request.getAddress()
                )
        );

        distributor.setCity(
                normalize(
                        request.getCity()
                )
        );

        distributor.setState(
                normalize(
                        request.getState()
                )
        );

        distributor.setPincode(
                normalize(
                        request.getPincode()
                )
        );

        distributor.setBrands(normalize(request.getBrands()) == null ? "ALL_BRANDS" : normalize(request.getBrands()));

        distributor.setStatus(
                status
        );

        return distributor;
    }


    // =========================================================
    // GET SELLER
    // =========================================================

    private User seller(
            String email) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Seller not found"
                                )
                        );

        if (user.getRole() != Role.SELLER
                || !user.isEnabled()) {

            throw new RuntimeException(
                    "Only active sellers can submit a distributor request"
            );
        }

        return user;
    }


    // =========================================================
    // NOTIFY DISTRIBUTOR
    // =========================================================

    private void notifyDistributor(
            Distributor distributor,
            String subject,
            String body) {

        try {

            emailService.sendDistributorWorkflowEmail(
                    distributor.getEmail(),
                    subject,
                    body
            );

        } catch (Exception ignored) {
        }
    }


    // =========================================================
    // BLANK STRING
    // =========================================================

    private String blank(
            String value) {

        if (value == null
                || value.isBlank()) {

            return null;
        }

        return value.trim();
    }


    // =========================================================
    // MAKE DISTRIBUTOR AVAILABLE TO ALL ACTIVE SELLERS
    // =========================================================

    /**
     * Admin-approved distributor becomes available to
     * every active seller.
     *
     * Relationship:
     *
     * Seller
     *      ↓
     * SellerDistributor
     *      ↓
     * Distributor
     *
     * Brand:
     *
     * ALL_BRANDS
     *
     * This allows the distributor to supply products
     * of any brand.
     */
    private void makeAvailableToAllSellers(
            Distributor distributor) {
        // Deliberately no-op. Admin now chooses the exact seller, distributor and brand.
    }


    // =========================================================
    // GET ALL DISTRIBUTORS
    // =========================================================

    @Transactional(readOnly = true)
    public List<DistributorResponse> getAllDistributors() {

        return distributorRepository
                .findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    // =========================================================
    // GET ACTIVE DISTRIBUTORS
    // =========================================================

    @Transactional(readOnly = true)
    public List<DistributorResponse> getActiveDistributors() {

        return distributorRepository
                .findByStatus("ACTIVE")
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    // =========================================================
    // GET DISTRIBUTOR BY ID
    // =========================================================

    @Transactional(readOnly = true)
    public DistributorResponse getDistributorById(
            Long id) {

        Distributor distributor =
                distributorRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Distributor not found with id: "
                                                + id
                                )
                        );

        return mapToResponse(
                distributor
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDistributorDetails(
            Long id) {

        Distributor distributor =
                distributorRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Distributor not found"
                                )
                        );

        List<SellerDistributor> assignments =
                sellerDistributorRepository.findByDistributor(distributor);

        List<Purchase> purchases =
                assignments.isEmpty()
                        ? List.of()
                        : purchaseRepository.findByDistributorInOrderByPurchaseDateDesc(assignments);

        BigDecimal purchaseTotal = BigDecimal.ZERO;
        BigDecimal paidTotal = BigDecimal.ZERO;
        BigDecimal creditNoteTotal = BigDecimal.ZERO;

        List<Map<String, Object>> purchaseRows =
                new java.util.ArrayList<>();

        for (Purchase purchase : purchases) {
            BigDecimal purchaseAmount = safe(purchase.getGrandTotal());
            BigDecimal paid = purchasePaymentRepository.findByPurchaseOrderByPaymentDateDesc(purchase)
                    .stream()
                    .map(PurchasePayment::getAmount)
                    .map(this::safe)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal credit = safe(creditNoteRepository.totalForPurchase(purchase));
            BigDecimal pending = purchaseAmount.subtract(paid).subtract(credit).max(BigDecimal.ZERO);

            purchaseTotal = purchaseTotal.add(purchaseAmount);
            paidTotal = paidTotal.add(paid);
            creditNoteTotal = creditNoteTotal.add(credit);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("purchaseId", purchase.getId());
            row.put("invoiceNumber", purchase.getInvoiceNumber());
            row.put("purchaseDate", purchase.getPurchaseDate());
            row.put("sellerId", purchase.getSeller() == null ? null : purchase.getSeller().getId());
            row.put("sellerName", purchase.getSeller() == null ? null : purchase.getSeller().getBusinessName());
            row.put("sellerEmail", purchase.getSeller() == null ? null : purchase.getSeller().getEmail());
            row.put("brand", purchase.getDistributor() == null ? null : purchase.getDistributor().getBrand());
            row.put("grandTotal", purchaseAmount);
            row.put("paidAmount", paid);
            row.put("creditNoteAmount", credit);
            row.put("pendingAmount", pending);
            row.put("status", purchase.getStatus());
            purchaseRows.add(row);
        }

        List<Map<String, Object>> sellerRows =
                assignments.stream()
                        .map(assignment -> {
                            Map<String, Object> row = new LinkedHashMap<>();
                            row.put("assignmentId", assignment.getId());
                            row.put("sellerId", assignment.getSeller() == null ? null : assignment.getSeller().getId());
                            row.put("sellerName", assignment.getSeller() == null ? null : assignment.getSeller().getBusinessName());
                            row.put("sellerEmail", assignment.getSeller() == null ? null : assignment.getSeller().getEmail());
                            row.put("sellerMobile", assignment.getSeller() == null ? null : assignment.getSeller().getMobile());
                            row.put("brand", assignment.getBrand());
                            row.put("assignedBrands", assignment.getAssignedBrands());
                            row.put("paymentTerms", assignment.getPaymentTerms());
                            row.put("creditLimit", safe(assignment.getCreditLimit()));
                            row.put("openingBalance", safe(assignment.getOpeningBalance()));
                            row.put("active", assignment.getActive());
                            return row;
                        })
                        .toList();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("linkedSellers", assignments.size());
        summary.put("purchaseInvoices", purchases.size());
        summary.put("totalPurchaseAmount", purchaseTotal);
        summary.put("totalPaidAmount", paidTotal);
        summary.put("totalCreditNotes", creditNoteTotal);
        summary.put("pendingPayableAmount", purchaseTotal.subtract(paidTotal).subtract(creditNoteTotal).max(BigDecimal.ZERO));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("distributor", mapToResponse(distributor));
        response.put("summary", summary);
        response.put("linkedSellers", sellerRows);
        response.put("purchaseHistory", purchaseRows);
        return response;
    }


    // =========================================================
    // UPDATE DISTRIBUTOR
    // =========================================================

    public DistributorResponse updateDistributor(
            Long id,
            DistributorRequest request) {

        Distributor distributor =
                distributorRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Distributor not found"
                                )
                        );

        String gstin =
                normalize(
                        request.getGstin()
                );

        if (gstin != null) {

            distributorRepository
                    .findByGstin(gstin)
                    .ifPresent(existing -> {

                        if (!existing.getId().equals(id)) {

                            throw new RuntimeException(
                                    "Another distributor already uses this GSTIN"
                            );
                        }
                    });
        }


        distributor.setBusinessName(
                normalizeRequired(
                        request.getBusinessName()
                )
        );

        distributor.setContactPerson(
                normalize(
                        request.getContactPerson()
                )
        );

        distributor.setMobile(
                normalizeRequired(
                        request.getMobile()
                )
        );

        distributor.setEmail(
                normalize(
                        request.getEmail()
                )
        );

        applyExtendedDetails(distributor, request);

        distributor.setGstin(
                gstin
        );

        distributor.setAddress(
                normalize(
                        request.getAddress()
                )
        );

        distributor.setCity(
                normalize(
                        request.getCity()
                )
        );

        distributor.setState(
                normalize(
                        request.getState()
                )
        );

        distributor.setPincode(
                normalize(
                        request.getPincode()
                )
        );

        distributor.setBrands(normalize(request.getBrands()) == null ? "ALL_BRANDS" : normalize(request.getBrands()));


        String status =
                normalize(
                        request.getStatus()
                );

        if (status != null) {

            distributor.setStatus(
                    status.toUpperCase()
            );
        }


        Distributor updatedDistributor =
                distributorRepository.save(
                        distributor
                );


        /*
         * If distributor is ACTIVE after update,
         * make sure all active sellers have the
         * relationship.
         */
        if ("ACTIVE".equalsIgnoreCase(
                updatedDistributor.getStatus())) {

            makeAvailableToAllSellers(
                    updatedDistributor
            );
        }


        return mapToResponse(
                updatedDistributor
        );
    }


    // =========================================================
    // DEACTIVATE DISTRIBUTOR
    // =========================================================

    public DistributorResponse deactivateDistributor(
            Long id) {

        Distributor distributor =
                distributorRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Distributor not found with id: "
                                                + id
                                )
                        );

        distributor.setStatus(
                "INACTIVE"
        );

        Distributor updatedDistributor =
                distributorRepository.save(
                        distributor
                );

        return mapToResponse(
                updatedDistributor
        );
    }


    // =========================================================
    // ACTIVATE DISTRIBUTOR
    // =========================================================

    public DistributorResponse activateDistributor(
            Long id) {

        Distributor distributor =
                distributorRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Distributor not found with id: "
                                                + id
                                )
                        );

        distributor.setStatus(
                "ACTIVE"
        );

        Distributor updatedDistributor =
                distributorRepository.save(
                        distributor
                );


        /*
         * IMPORTANT FIX
         *
         * Earlier this method only changed the distributor
         * status.
         *
         * Now whenever Admin activates a distributor,
         * SellerDistributor relationships are automatically
         * created for all active sellers.
         */
        makeAvailableToAllSellers(
                updatedDistributor
        );


        return mapToResponse(
                updatedDistributor
        );
    }


    // =========================================================
    // DELETE DISTRIBUTOR
    // =========================================================

    public void deleteDistributor(
            Long id) {

        Distributor distributor =
                distributorRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Distributor not found with id: "
                                                + id
                                )
                        );

        distributorRepository.delete(
                distributor
        );
    }


    // =========================================================
    // SEARCH DISTRIBUTOR
    // =========================================================

    @Transactional(readOnly = true)
    public List<DistributorResponse> searchDistributors(
            String businessName) {

        if (businessName == null
                || businessName.isBlank()) {

            return getAllDistributors();
        }

        return distributorRepository
                .findByBusinessNameContainingIgnoreCase(
                        businessName.trim()
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    // =========================================================
    // ENTITY → RESPONSE
    // =========================================================

    private DistributorResponse mapToResponse(
            Distributor distributor) {

        DistributorResponse response = new DistributorResponse();
        response.setId(distributor.getId());
        response.setBusinessName(distributor.getBusinessName());
        response.setContactPerson(distributor.getContactPerson());
        response.setMobile(distributor.getMobile());
        response.setEmail(distributor.getEmail());
        response.setDistributorCode(distributor.getDistributorCode());
        response.setBusinessType(distributor.getBusinessType());
        response.setAlternateMobile(distributor.getAlternateMobile());
        response.setWhatsappNumber(distributor.getWhatsappNumber());
        response.setWebsite(distributor.getWebsite());
        response.setGstin(distributor.getGstin());
        response.setGstRegistrationStatus(distributor.getGstRegistrationStatus());
        response.setPan(distributor.getPan());
        response.setAddress(distributor.getAddress());
        response.setCity(distributor.getCity());
        response.setState(distributor.getState());
        response.setPincode(distributor.getPincode());
        response.setDistrict(distributor.getDistrict());
        response.setStateCode(distributor.getStateCode());
        response.setBillingAddress(distributor.getBillingAddress());
        response.setWarehouseAddress(distributor.getWarehouseAddress());
        response.setAccountsEmail(distributor.getAccountsEmail());
        response.setBrands(distributor.getBrands());
        response.setProductCategories(distributor.getProductCategories());
        response.setDistributorType(distributor.getDistributorType());
        response.setAuthorizationDetails(distributor.getAuthorizationDetails());
        response.setSalespersonName(distributor.getSalespersonName());
        response.setSalespersonMobile(distributor.getSalespersonMobile());
        response.setClaimsContact(distributor.getClaimsContact());
        response.setBankDetails(distributor.getBankDetails());
        response.setAccountHolderName(distributor.getAccountHolderName());
        response.setBankName(distributor.getBankName());
        response.setAccountNumber(distributor.getAccountNumber());
        response.setIfscCode(distributor.getIfscCode());
        response.setBranchName(distributor.getBranchName());
        response.setAccountType(distributor.getAccountType());
        response.setUpiId(distributor.getUpiId());
        response.setPaymentQrUrl(distributor.getPaymentQrUrl());
        response.setDocumentUrls(distributor.getDocumentUrls());
        response.setReturnPolicy(distributor.getReturnPolicy());
        response.setReplacementTerms(distributor.getReplacementTerms());
        response.setShortageReportingPeriod(distributor.getShortageReportingPeriod());
        response.setWarrantyClaimProcess(distributor.getWarrantyClaimProcess());
        response.setCreditNoteTerms(distributor.getCreditNoteTerms());
        response.setDeliveryTerms(distributor.getDeliveryTerms());
        response.setUsualDeliveryTime(distributor.getUsualDeliveryTime());
        response.setSchemeTerms(distributor.getSchemeTerms());
        response.setStatus(distributor.getStatus());
        response.setCreatedAt(distributor.getCreatedAt());
        response.setUpdatedAt(distributor.getUpdatedAt());
        response.setSubmittedBySellerId(distributor.getSubmittedBySeller() == null ? null : distributor.getSubmittedBySeller().getId());
        response.setAdminReview(distributor.getAdminReview());
        return response;
    }

    private void applyExtendedDetails(Distributor distributor, DistributorRequest request) {
        distributor.setDistributorCode(normalize(request.getDistributorCode()));
        distributor.setBusinessType(normalize(request.getBusinessType()));
        distributor.setAlternateMobile(normalize(request.getAlternateMobile()));
        distributor.setWhatsappNumber(normalize(request.getWhatsappNumber()));
        distributor.setWebsite(normalize(request.getWebsite()));
        distributor.setGstRegistrationStatus(normalize(request.getGstRegistrationStatus()));
        distributor.setPan(normalize(request.getPan()));
        distributor.setDistrict(normalize(request.getDistrict()));
        distributor.setStateCode(normalize(request.getStateCode()));
        distributor.setBillingAddress(normalize(request.getBillingAddress()));
        distributor.setWarehouseAddress(normalize(request.getWarehouseAddress()));
        distributor.setAccountsEmail(normalize(request.getAccountsEmail()));
        distributor.setProductCategories(normalize(request.getProductCategories()));
        distributor.setDistributorType(normalize(request.getDistributorType()));
        distributor.setAuthorizationDetails(normalize(request.getAuthorizationDetails()));
        distributor.setSalespersonName(normalize(request.getSalespersonName()));
        distributor.setSalespersonMobile(normalize(request.getSalespersonMobile()));
        distributor.setClaimsContact(normalize(request.getClaimsContact()));
        distributor.setBankDetails(normalize(request.getBankDetails()));
        distributor.setAccountHolderName(normalize(request.getAccountHolderName()));
        distributor.setBankName(normalize(request.getBankName()));
        distributor.setAccountNumber(normalize(request.getAccountNumber()));
        distributor.setIfscCode(normalize(request.getIfscCode()));
        distributor.setBranchName(normalize(request.getBranchName()));
        distributor.setAccountType(normalize(request.getAccountType()));
        distributor.setUpiId(normalize(request.getUpiId()));
        distributor.setPaymentQrUrl(normalize(request.getPaymentQrUrl()));
        distributor.setDocumentUrls(normalize(request.getDocumentUrls()));
        distributor.setReturnPolicy(normalize(request.getReturnPolicy()));
        distributor.setReplacementTerms(normalize(request.getReplacementTerms()));
        distributor.setShortageReportingPeriod(normalize(request.getShortageReportingPeriod()));
        distributor.setWarrantyClaimProcess(normalize(request.getWarrantyClaimProcess()));
        distributor.setCreditNoteTerms(normalize(request.getCreditNoteTerms()));
        distributor.setDeliveryTerms(normalize(request.getDeliveryTerms()));
        distributor.setUsualDeliveryTime(normalize(request.getUsualDeliveryTime()));
        distributor.setSchemeTerms(normalize(request.getSchemeTerms()));
    }


    // =========================================================
    // STRING HELPERS
    // =========================================================

    private String normalize(
            String value) {

        if (value == null
                || value.isBlank()) {

            return null;
        }

        return value.trim();
    }


    private String normalizeRequired(
            String value) {

        if (value == null
                || value.isBlank()) {

            throw new RuntimeException(
                    "Required field cannot be empty"
            );
        }

        return value.trim();
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
