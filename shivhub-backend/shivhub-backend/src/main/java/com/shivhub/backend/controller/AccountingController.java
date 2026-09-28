package com.shivhub.backend.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shivhub.backend.dto.FinancialYearRequest;
import com.shivhub.backend.dto.JournalEntryRequest;
import com.shivhub.backend.dto.LedgerAccountRequest;
import com.shivhub.backend.dto.OpeningBalanceRequest;
import com.shivhub.backend.entity.FinancialYear;
import com.shivhub.backend.entity.JournalEntry;
import com.shivhub.backend.entity.LedgerAccount;
import com.shivhub.backend.entity.OpeningBalance;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.repository.UserRepository;
import com.shivhub.backend.service.FinancialYearService;
import com.shivhub.backend.service.JournalPostingService;
import com.shivhub.backend.service.LedgerAccountService;
import com.shivhub.backend.service.OpeningBalanceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/accounting")
@RequiredArgsConstructor
public class AccountingController {

    private final UserRepository userRepository;
    private final FinancialYearService financialYearService;
    private final LedgerAccountService ledgerAccountService;
    private final JournalPostingService journalPostingService;
    private final OpeningBalanceService openingBalanceService;

    @GetMapping("/financial-years")
    public List<FinancialYear> financialYears(Authentication authentication) {
        return financialYearService.list(seller(authentication));
    }

    @PostMapping("/financial-years")
    public ResponseEntity<FinancialYear> createFinancialYear(Authentication authentication, @Valid @RequestBody FinancialYearRequest request) {
        return ResponseEntity.status(201).body(financialYearService.create(request, seller(authentication)));
    }

    @PutMapping("/financial-years/{id}/close")
    public FinancialYear closeFinancialYear(Authentication authentication, @PathVariable Long id) {
        return financialYearService.close(id, seller(authentication));
    }

    @GetMapping("/ledger-accounts")
    public List<LedgerAccount> ledgerAccounts(Authentication authentication) {
        return ledgerAccountService.list(seller(authentication));
    }

    @PostMapping("/ledger-accounts/defaults")
    public List<LedgerAccount> seedDefaultLedgerAccounts(Authentication authentication) {
        return ledgerAccountService.seedDefaults(seller(authentication));
    }

    @PostMapping("/ledger-accounts")
    public ResponseEntity<LedgerAccount> createLedgerAccount(Authentication authentication, @Valid @RequestBody LedgerAccountRequest request) {
        return ResponseEntity.status(201).body(ledgerAccountService.create(request, seller(authentication)));
    }

    @GetMapping("/journal-entries")
    public List<JournalEntry> journalEntries(Authentication authentication,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return journalPostingService.list(seller(authentication), from, to);
    }

    @PostMapping("/journal-entries")
    public ResponseEntity<JournalEntry> postJournalEntry(Authentication authentication, @Valid @RequestBody JournalEntryRequest request) {
        return ResponseEntity.status(201).body(journalPostingService.post(request, seller(authentication)));
    }

    @GetMapping("/opening-balances/{financialYearId}")
    public List<OpeningBalance> openingBalances(Authentication authentication, @PathVariable Long financialYearId) {
        return openingBalanceService.list(financialYearId, seller(authentication));
    }

    @PostMapping("/opening-balances")
    public ResponseEntity<OpeningBalance> saveOpeningBalance(Authentication authentication, @Valid @RequestBody OpeningBalanceRequest request) {
        return ResponseEntity.status(201).body(openingBalanceService.save(request, seller(authentication)));
    }

    private User seller(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) throw new RuntimeException("Authentication required");
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Seller not found"));
    }
}
