package com.shivhub.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shivhub.backend.dto.LedgerAccountRequest;
import com.shivhub.backend.entity.LedgerAccount;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.LedgerAccountType;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.repository.LedgerAccountRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LedgerAccountService {

    private final LedgerAccountRepository repository;

    private static final Object[][] DEFAULT_ACCOUNTS = {
            {"1000", "Cash", LedgerAccountType.ASSET},
            {"1010", "Bank", LedgerAccountType.ASSET},
            {"1100", "Customer Receivables", LedgerAccountType.ASSET},
            {"1200", "Inventory", LedgerAccountType.ASSET},
            {"1500", "Fixed Assets", LedgerAccountType.ASSET},
            {"2000", "Distributor Payables", LedgerAccountType.LIABILITY},
            {"2100", "GST Payable", LedgerAccountType.LIABILITY},
            {"2200", "Loans Payable", LedgerAccountType.LIABILITY},
            {"3000", "Owner Capital", LedgerAccountType.EQUITY},
            {"3100", "Owner Drawings", LedgerAccountType.EQUITY},
            {"4000", "Sales", LedgerAccountType.INCOME},
            {"4100", "Sales Returns", LedgerAccountType.INCOME},
            {"5000", "Cost of Goods Sold", LedgerAccountType.EXPENSE},
            {"5100", "Purchase Returns / Credit Notes", LedgerAccountType.EXPENSE},
            {"6000", "Operating Expenses", LedgerAccountType.EXPENSE},
            {"6100", "Depreciation", LedgerAccountType.EXPENSE},
            {"6200", "Interest Expense", LedgerAccountType.EXPENSE}
    };

    @Transactional
    public List<LedgerAccount> seedDefaults(User seller) {
        validateSeller(seller);
        for (Object[] row : DEFAULT_ACCOUNTS) {
            String code = (String) row[0];
            if (repository.existsBySellerAndCode(seller, code)) continue;
            LedgerAccount account = new LedgerAccount();
            account.setSeller(seller);
            account.setCode(code);
            account.setName((String) row[1]);
            account.setAccountType((LedgerAccountType) row[2]);
            account.setSystemAccount(true);
            repository.save(account);
        }
        return list(seller);
    }

    @Transactional
    public LedgerAccount create(LedgerAccountRequest request, User seller) {
        validateSeller(seller);
        if (request == null) throw new RuntimeException("Ledger account request is required");
        if (repository.existsBySellerAndCode(seller, request.getCode().trim())) {
            throw new RuntimeException("Ledger account code already exists");
        }
        LedgerAccount account = new LedgerAccount();
        account.setSeller(seller);
        account.setCode(request.getCode().trim());
        account.setName(request.getName().trim());
        account.setAccountType(request.getAccountType());
        return repository.save(account);
    }

    @Transactional(readOnly = true)
    public List<LedgerAccount> list(User seller) {
        validateSeller(seller);
        return repository.findBySellerOrderByCodeAsc(seller);
    }

    private void validateSeller(User seller) {
        if (seller == null || seller.getId() == null || seller.getRole() != Role.SELLER || !seller.isEnabled()) {
            throw new RuntimeException("Only active sellers can manage accounts");
        }
    }
}
