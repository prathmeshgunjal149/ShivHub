package com.shivhub.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shivhub.backend.dto.OpeningBalanceRequest;
import com.shivhub.backend.entity.FinancialYear;
import com.shivhub.backend.entity.OpeningBalance;
import com.shivhub.backend.entity.Product;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.repository.FinancialYearRepository;
import com.shivhub.backend.repository.OpeningBalanceRepository;
import com.shivhub.backend.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OpeningBalanceService {

    private final OpeningBalanceRepository repository;
    private final FinancialYearRepository financialYearRepository;
    private final ProductRepository productRepository;

    @Transactional
    public OpeningBalance save(OpeningBalanceRequest request, User seller) {
        validateSeller(seller);
        if (request == null) throw new RuntimeException("Opening balance request is required");

        FinancialYear year = financialYearRepository.findById(request.getFinancialYearId())
                .orElseThrow(() -> new RuntimeException("Financial year not found"));
        if (year.getSeller() == null || !seller.getId().equals(year.getSeller().getId())) {
            throw new RuntimeException("Financial year does not belong to this seller");
        }
        if (year.isClosed()) throw new RuntimeException("Financial year is closed");

        Product product = null;
        if (request.getProductId() != null) {
            product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            if (product.getSeller() == null || !seller.getId().equals(product.getSeller().getId())) {
                throw new RuntimeException("Product does not belong to this seller");
            }
        }

        OpeningBalance balance = new OpeningBalance();
        balance.setSeller(seller);
        balance.setFinancialYear(year);
        balance.setBalanceType(request.getBalanceType());
        balance.setPartyType(blankToNull(request.getPartyType()));
        balance.setPartyId(request.getPartyId());
        balance.setProduct(product);
        balance.setAmount(amount(request.getAmount()));
        balance.setQuantity(request.getQuantity());
        balance.setUnitValue(request.getUnitValue() == null ? null : amount(request.getUnitValue()));
        balance.setNotes(request.getNotes());
        balance.setPosted(false);
        return repository.save(balance);
    }

    @Transactional(readOnly = true)
    public List<OpeningBalance> list(Long financialYearId, User seller) {
        validateSeller(seller);
        FinancialYear year = financialYearRepository.findById(financialYearId)
                .orElseThrow(() -> new RuntimeException("Financial year not found"));
        if (year.getSeller() == null || !seller.getId().equals(year.getSeller().getId())) {
            throw new RuntimeException("Financial year does not belong to this seller");
        }
        return repository.findBySellerAndFinancialYearOrderByIdAsc(seller, year);
    }

    private BigDecimal amount(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validateSeller(User seller) {
        if (seller == null || seller.getId() == null || seller.getRole() != Role.SELLER || !seller.isEnabled()) {
            throw new RuntimeException("Only active sellers can manage opening balances");
        }
    }
}
