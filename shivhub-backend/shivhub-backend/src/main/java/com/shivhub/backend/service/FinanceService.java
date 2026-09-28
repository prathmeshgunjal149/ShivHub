package com.shivhub.backend.service;

import com.shivhub.backend.dto.FinanceRequest;
import com.shivhub.backend.entity.*;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.repository.*;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class FinanceService {
    private final FinanceSaleRepository sales;
    private final FinanceCompanyRepository companies;
    private final FinanceSchemeRepository schemes;
    private final EmiInstallmentRepository installments;
    private final FinanceAuditRepository audits;
    private final UserRepository users;
    private final Validator validator;
    private final ApplicationEventPublisher events;

    public record Committed(Long saleId) { }
    public record Summary(Long id, Long billId, String invoice, String customer, String email, String mobile,
                          LocalDateTime saleDate, String company, String loanNumber, String scheme,
                          int tenureMonths, int advanceMonths, BigDecimal fullSaleAmount, BigDecimal downpayment,
                          BigDecimal expectedDisbursement, BigDecimal actualDisbursement, String settlementStatus,
                          LocalDate settlementDate, String settlementReference, boolean active, boolean summarySent,
                          BigDecimal processingCharges, BigDecimal dbdCharges, BigDecimal otherCharges,
                          BigDecimal deduction, BigDecimal adjustment, String remarks) { }
    public record EmiView(Long id, int installmentNumber, String monthLabel, BigDecimal amount,
                          LocalDate dueDate, String status, boolean reminderSent,
                          LocalDateTime reminderSentAt, String failureMessage) { }
    public record Detail(Summary sale, List<EmiView> installments) { }

    public void attach(OfflineBill bill, FinanceRequest request) {
        if (request == null) throw new IllegalArgumentException("Finance details are required");
        var violations = validator.validate(request);
        if (!violations.isEmpty()) throw new IllegalArgumentException(violations.iterator().next().getMessage());
        try { new jakarta.mail.internet.InternetAddress(bill.getCustomerEmail(), true).validate(); }
        catch (Exception ex) { throw new IllegalArgumentException("A valid customer email is required for EMI notifications"); }
        if (bill.getCustomerEmail() == null || !bill.getCustomerEmail().contains("@")) throw new IllegalArgumentException("Customer email is required");
        FinanceCompany company = companies.findById(request.companyId()).filter(FinanceCompany::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Choose an active admin-approved finance company"));
        int tenure = request.tenureMonths(), advance = request.advanceMonths();
        String schemeName = "Custom";
        if (request.schemeId() != null) {
            FinanceScheme scheme = schemes.findById(request.schemeId()).filter(FinanceScheme::isActive)
                    .orElseThrow(() -> new IllegalArgumentException("Scheme is unavailable"));
            if (scheme.getCompanyId() != null && !scheme.getCompanyId().equals(company.getId())) throw new IllegalArgumentException("Scheme belongs to another provider");
            tenure = scheme.getTenureMonths(); advance = scheme.getAdvanceMonths(); schemeName = scheme.getName();
        }
        if (advance > tenure || request.installments().size() != tenure) throw new IllegalArgumentException("EMI schedule must match the selected tenure");
        BigDecimal down = money(request.downpayment());
        if (down.compareTo(bill.getGrandTotal()) > 0) throw new IllegalArgumentException("Downpayment cannot exceed the invoice total; record charges separately");
        FinanceSale sale = new FinanceSale();
        sale.setBill(bill); sale.setCompany(company); sale.setCompanyName(company.getName());
        sale.setSchemeName(schemeName); sale.setTenureMonths(tenure); sale.setAdvanceMonths(advance);
        sale.setLoanNumber(request.loanNumber()); sale.setFullSaleAmount(bill.getGrandTotal()); sale.setDownpayment(down);
        sale.setProcessingCharges(money(request.processingCharges())); sale.setDbdCharges(money(request.dbdCharges()));
        sale.setOtherCharges(money(request.otherCharges())); sale.setDeduction(money(request.deduction()));
        sale.setAdjustment(money(request.adjustment())); sale.setRemarks(request.remarks());
        BigDecimal expected = bill.getGrandTotal().subtract(down).subtract(sale.getDeduction()).add(sale.getAdjustment());
        if (expected.signum() < 0) throw new IllegalArgumentException("Expected finance settlement cannot be negative");
        sale.setExpectedDisbursement(expected); sales.save(sale);
        LocalDate previous = null;
        for (int index = 0; index < request.installments().size(); index++) {
            var row = request.installments().get(index);
            if (previous != null && !row.dueDate().isAfter(previous)) throw new IllegalArgumentException("EMI due dates must be in increasing order");
            if (row.dueDate().isBefore(LocalDate.now(ZoneId.of("Asia/Kolkata")))) throw new IllegalArgumentException("A new EMI due date cannot be in the past");
            EmiInstallment emi = new EmiInstallment();
            emi.setSale(sale); emi.setInstallmentNumber(index + 1); emi.setMonthLabel(row.monthLabel().trim());
            emi.setAmount(money(row.amount())); emi.setDueDate(row.dueDate()); installments.save(emi); previous = row.dueDate();
        }
        bill.setFinanceProviderName(company.getName());
        audit(sale.getId(), bill.getSellerId(), "CREATED", "Finance schedule saved with full invoice value");
        events.publishEvent(new Committed(sale.getId()));
    }

    public User actor(String email) {
        return users.findByEmail(email).filter(u -> u.isEnabled() && (u.getRole() == Role.ADMIN || u.getRole() == Role.SELLER))
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Seller or admin access required"));
    }
    public void authorize(FinanceSale sale, User actor) {
        if (actor.getRole() != Role.ADMIN && !sale.getBill().getSellerId().equals(actor.getId())) throw new org.springframework.security.access.AccessDeniedException("Finance record belongs to another seller");
    }
    @Transactional(readOnly = true)
    public Page<Summary> search(String email, String query, Long company, String status, LocalDate from, LocalDate to, int page, int size) {
        User actor = actor(email); String term = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (term.length() > 150) throw new IllegalArgumentException("Search is too long");
        return sales.findAll((root, cq, cb) -> {
            List<Predicate> filters = new ArrayList<>(); var bill = root.join("bill");
            if (actor.getRole() != Role.ADMIN) filters.add(cb.equal(bill.get("sellerId"), actor.getId()));
            if (company != null) filters.add(cb.equal(root.get("company").get("id"), company));
            if (status != null && !status.isBlank()) filters.add(cb.equal(root.get("settlementStatus"), status));
            if (from != null) filters.add(cb.greaterThanOrEqualTo(bill.get("createdAt"), from.atStartOfDay()));
            if (to != null) filters.add(cb.lessThan(bill.get("createdAt"), to.plusDays(1).atStartOfDay()));
            if (!term.isEmpty()) {
                String pattern = "%" + term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                filters.add(cb.or(cb.like(cb.lower(bill.get("customerName")), pattern, '\\'), cb.like(cb.lower(bill.get("customerEmail")), pattern, '\\'), cb.like(bill.get("customerMobile"), pattern, '\\'), cb.like(cb.lower(bill.get("billNumber")), pattern, '\\'), cb.like(cb.lower(root.get("loanNumber")), pattern, '\\')));
            }
            return cb.and(filters.toArray(Predicate[]::new));
        }, PageRequest.of(Math.max(0, page), Math.max(1, Math.min(50, size)), Sort.by("id").descending())).map(this::summary);
    }
    @Transactional(readOnly = true)
    public Detail detail(String email, Long id) {
        FinanceSale sale = sales.findById(id).orElseThrow(); authorize(sale, actor(email));
        return new Detail(summary(sale), installments.findBySaleIdOrderByInstallmentNumber(id).stream().map(e -> new EmiView(e.getId(), e.getInstallmentNumber(), e.getMonthLabel(), e.getAmount(), e.getDueDate(), e.getStatus(), e.isReminderSent(), e.getReminderSentAt(), e.getFailureMessage())).toList());
    }
    public void settle(String email, Long id, BigDecimal amount, LocalDate date, String reference, String status, String reason) {
        User user = actor(email); FinanceSale sale = sales.lock(id).orElseThrow(); authorize(sale, user);
        if (!sale.isActive()) throw new IllegalArgumentException("Finance record is inactive");
        if (amount == null || amount.signum() < 0 || date == null || date.isAfter(LocalDate.now()) || reference == null || reference.isBlank() || reference.length() > 150) throw new IllegalArgumentException("Enter valid received amount, settlement date and reference");
        if (!Set.of("PENDING", "PARTIALLY_RECEIVED", "RECEIVED", "ADJUSTED").contains(status)) throw new IllegalArgumentException("Invalid settlement status");
        if (status.equals("RECEIVED") && amount.compareTo(sale.getExpectedDisbursement()) < 0) throw new IllegalArgumentException("Use Partially Received or Adjusted for a short settlement");
        if (status.equals("ADJUSTED") && (reason == null || reason.isBlank())) throw new IllegalArgumentException("Adjustment reason is required");
        audit(id, user.getId(), "SETTLEMENT", sale.getActualDisbursement() + " -> " + money(amount) + "; " + status + "; " + (reason == null ? "" : reason));
        sale.setActualDisbursement(money(amount)); sale.setSettlementStatus(status); sale.setSettlementDate(date); sale.setSettlementReference(reference.trim()); sale.setUpdatedBy(user.getId()); sale.setUpdatedAt(LocalDateTime.now());
    }
    public void stopForBill(Long billId, String reason) {
        sales.findByBillId(billId).ifPresent(sale -> { sale.setActive(false); for (var e : installments.findBySaleIdOrderByInstallmentNumber(sale.getId())) e.setStatus("CANCELLED"); audit(sale.getId(), null, "REMINDERS_STOPPED", reason); });
    }
    public void audit(Long sale, Long actor, String action, String details) {
        FinanceAudit row = new FinanceAudit(); row.setFinanceSaleId(sale); row.setActorId(actor); row.setAction(action); row.setDetails(details == null ? null : details.substring(0, Math.min(1000, details.length()))); audits.save(row);
    }
    private Summary summary(FinanceSale s) { var b = s.getBill(); return new Summary(s.getId(), b.getId(), b.getBillNumber(), b.getCustomerName(), b.getCustomerEmail(), b.getCustomerMobile(), b.getCreatedAt(), s.getCompanyName(), s.getLoanNumber(), s.getSchemeName(), s.getTenureMonths(), s.getAdvanceMonths(), s.getFullSaleAmount(), s.getDownpayment(), s.getExpectedDisbursement(), s.getActualDisbursement(), s.getSettlementStatus(), s.getSettlementDate(), s.getSettlementReference(), s.isActive(), s.isSummarySent(), s.getProcessingCharges(), s.getDbdCharges(), s.getOtherCharges(), s.getDeduction(), s.getAdjustment(), s.getRemarks()); }
    static BigDecimal money(BigDecimal value) { BigDecimal result = (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP); if (result.abs().compareTo(new BigDecimal("9999999999.99")) > 0) throw new IllegalArgumentException("Finance amount is too large"); return result; }
}
