package com.shivhub.backend.controller;

import com.shivhub.backend.dto.*;
import com.shivhub.backend.entity.*;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.repository.*;
import com.shivhub.backend.service.FinanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.List;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SELLER','ADMIN')")
@Transactional
public class FinanceController {
    private final FinanceService service;
    private final FinanceCompanyRepository companies;
    private final FinanceSchemeRepository schemes;
    private final FinanceCompanyRequestRepository requests;
    private final FinanceSaleRepository sales;
    private final FinanceAuditRepository audits;

    @GetMapping("/companies") public List<FinanceCompany> companies() { return companies.findByActiveTrueOrderByNameAsc(); }
    @GetMapping("/schemes") public List<FinanceScheme> schemes() { return schemes.findByActiveTrueOrderByNameAsc(); }
    @GetMapping("/admin/companies") @PreAuthorize("hasRole('ADMIN')") public Page<FinanceCompany> allCompanies(@RequestParam(defaultValue = "0") int page) { return companies.findAll(PageRequest.of(Math.max(0, page), 50, Sort.by("name"))); }
    @GetMapping("/admin/schemes") @PreAuthorize("hasRole('ADMIN')") public Page<FinanceScheme> allSchemes(@RequestParam(defaultValue = "0") int page) { return schemes.findAll(PageRequest.of(Math.max(0, page), 50, Sort.by("name"))); }
    @PostMapping("/admin/companies") @PreAuthorize("hasRole('ADMIN')") public FinanceCompany create(@Valid @RequestBody FinanceCompanyInput input, Authentication auth) { return saveCompany(null, input, auth); }
    @PutMapping("/admin/companies/{id}") @PreAuthorize("hasRole('ADMIN')") public FinanceCompany update(@PathVariable Long id, @Valid @RequestBody FinanceCompanyInput input, Authentication auth) { return saveCompany(id, input, auth); }
    private FinanceCompany saveCompany(Long id, FinanceCompanyInput input, Authentication auth) {
        var row = id == null ? new FinanceCompany() : companies.findById(id).orElseThrow();
        String name = input.name().trim().replaceAll("\\s+", " ");
        if ((id == null || !name.equalsIgnoreCase(row.getName())) && companies.existsByNameIgnoreCase(name)) throw new IllegalArgumentException("Finance company already exists");
        row.setName(name); row.setContact(input.contact()); row.setActive(input.active()); row.setApprovedBy(service.actor(auth.getName()).getId()); return companies.save(row);
    }
    @PostMapping("/admin/schemes") @PreAuthorize("hasRole('ADMIN')") public FinanceScheme createScheme(@Valid @RequestBody FinanceSchemeInput input) { return saveScheme(null, input); }
    @PutMapping("/admin/schemes/{id}") @PreAuthorize("hasRole('ADMIN')") public FinanceScheme updateScheme(@PathVariable Long id, @Valid @RequestBody FinanceSchemeInput input) { return saveScheme(id, input); }
    private FinanceScheme saveScheme(Long id, FinanceSchemeInput input) {
        if (input.advanceMonths() > input.tenureMonths()) throw new IllegalArgumentException("Advance months exceed tenure");
        if (input.companyId() != null && !companies.existsById(input.companyId())) throw new IllegalArgumentException("Unknown finance company");
        var row = id == null ? new FinanceScheme() : schemes.findById(id).orElseThrow();
        row.setName(input.name().trim()); row.setCompanyId(input.companyId()); row.setTenureMonths(input.tenureMonths()); row.setAdvanceMonths(input.advanceMonths()); row.setActive(input.active()); return schemes.save(row);
    }
    @PostMapping("/company-requests") public FinanceCompanyRequest request(@Valid @RequestBody FinanceCompanyRequestInput input, Authentication auth) {
        String name = input.name().trim().replaceAll("\\s+", " ");
        if (companies.existsByNameIgnoreCase(name)) throw new IllegalArgumentException("Company already exists; contact admin if it is inactive");
        FinanceCompanyRequest row = new FinanceCompanyRequest(); row.setName(name); row.setContact(input.contact()); row.setRemarks(input.remarks()); row.setRequestedBy(service.actor(auth.getName()).getId()); return requests.save(row);
    }
    @GetMapping("/company-requests") public Page<FinanceCompanyRequest> requests(Authentication auth, @RequestParam(defaultValue = "0") int page) {
        var user = service.actor(auth.getName()); var pageable = PageRequest.of(Math.max(0, page), 25, Sort.by("id").descending());
        return user.getRole() == Role.ADMIN ? requests.findAll(pageable) : requests.findByRequestedBy(user.getId(), pageable);
    }
    @PatchMapping("/admin/company-requests/{id}") @PreAuthorize("hasRole('ADMIN')") public FinanceCompanyRequest review(@PathVariable Long id, @Valid @RequestBody FinanceCompanyReviewRequest input, Authentication auth) {
        var row = requests.lock(id).orElseThrow(); if (!"PENDING".equals(row.getStatus())) throw new IllegalArgumentException("Request was already reviewed");
        Long actor = service.actor(auth.getName()).getId();
        if (input.approve()) { if (companies.existsByNameIgnoreCase(row.getName())) throw new IllegalArgumentException("Company already exists; reject duplicate request"); FinanceCompany company = new FinanceCompany(); company.setName(row.getName()); company.setContact(row.getContact()); company.setApprovedBy(actor); companies.save(company); }
        row.setStatus(input.approve() ? "APPROVED" : "REJECTED"); row.setReviewedBy(actor); row.setReviewedAt(LocalDateTime.now()); row.setReviewReason(input.reason()); return row;
    }
    @GetMapping("/sales") public Page<FinanceService.Summary> search(Authentication auth, @RequestParam(required = false) String q, @RequestParam(required = false) Long companyId, @RequestParam(required = false) String status, @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) { return service.search(auth.getName(), q, companyId, status, from, to, page, size); }
    @GetMapping("/sales/{id}") public FinanceService.Detail detail(@PathVariable Long id, Authentication auth) { return service.detail(auth.getName(), id); }
    @PatchMapping("/sales/{id}/settlement") public void settle(@PathVariable Long id, @Valid @RequestBody FinanceSettlementRequest input, Authentication auth) { service.settle(auth.getName(), id, input.amount(), input.date(), input.reference(), input.status(), input.reason()); }
    @GetMapping("/sales/{id}/audit") public Page<FinanceAudit> audit(@PathVariable Long id, Authentication auth, @RequestParam(defaultValue = "0") int page) { service.authorize(sales.findById(id).orElseThrow(), service.actor(auth.getName())); return audits.findByFinanceSaleId(id, PageRequest.of(Math.max(0, page), 25, Sort.by("id").descending())); }
}
