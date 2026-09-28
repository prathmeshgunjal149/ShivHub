package com.shivhub.backend.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shivhub.backend.dto.FinancialYearRequest;
import com.shivhub.backend.entity.FinancialYear;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.repository.FinancialYearRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FinancialYearService {

    private final FinancialYearRepository repository;

    @Transactional
    public FinancialYear create(FinancialYearRequest request, User seller) {
        validateSeller(seller);
        if (request == null) throw new RuntimeException("Financial year request is required");
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new RuntimeException("Financial year end date cannot be before start date");
        }

        String yearCode = request.getYearCode();
        if (yearCode == null || yearCode.isBlank()) {
            yearCode = request.getStartDate().getYear() + "-" + String.valueOf(request.getEndDate().getYear()).substring(2);
        }
        if (repository.findBySellerAndYearCode(seller, yearCode).isPresent()) {
            throw new RuntimeException("Financial year already exists");
        }

        FinancialYear year = new FinancialYear();
        year.setSeller(seller);
        year.setYearCode(yearCode);
        year.setStartDate(request.getStartDate());
        year.setEndDate(request.getEndDate());
        return repository.save(year);
    }

    @Transactional
    public FinancialYear close(Long id, User seller) {
        validateSeller(seller);
        FinancialYear year = getSellerYear(id, seller);
        year.setClosed(true);
        year.setClosedAt(LocalDateTime.now());
        return repository.save(year);
    }

    @Transactional(readOnly = true)
    public List<FinancialYear> list(User seller) {
        validateSeller(seller);
        return repository.findBySellerOrderByStartDateDesc(seller);
    }

    FinancialYear getSellerYear(Long id, User seller) {
        if (id == null) throw new RuntimeException("Financial year is required");
        FinancialYear year = repository.findById(id).orElseThrow(() -> new RuntimeException("Financial year not found"));
        if (year.getSeller() == null || !seller.getId().equals(year.getSeller().getId())) {
            throw new RuntimeException("You are not authorized to access this financial year");
        }
        return year;
    }

    private void validateSeller(User seller) {
        if (seller == null || seller.getId() == null || seller.getRole() != Role.SELLER || !seller.isEnabled()) {
            throw new RuntimeException("Only active sellers can manage financial years");
        }
    }
}
