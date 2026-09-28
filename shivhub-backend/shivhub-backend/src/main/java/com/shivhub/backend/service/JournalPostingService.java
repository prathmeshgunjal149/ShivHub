package com.shivhub.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shivhub.backend.dto.JournalEntryRequest;
import com.shivhub.backend.dto.JournalLineRequest;
import com.shivhub.backend.entity.FinancialYear;
import com.shivhub.backend.entity.JournalEntry;
import com.shivhub.backend.entity.JournalLine;
import com.shivhub.backend.entity.LedgerAccount;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.JournalEntryStatus;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.repository.FinancialYearRepository;
import com.shivhub.backend.repository.JournalEntryRepository;
import com.shivhub.backend.repository.LedgerAccountRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JournalPostingService {

    private final JournalEntryRepository journalEntryRepository;
    private final LedgerAccountRepository ledgerAccountRepository;
    private final FinancialYearRepository financialYearRepository;

    @Transactional
    public JournalEntry post(JournalEntryRequest request, User seller) {
        validateSeller(seller);
        if (request == null || request.getLines() == null || request.getLines().size() < 2) {
            throw new RuntimeException("A journal entry needs at least two lines");
        }

        String sourceType = normalize(request.getSourceType());
        String eventType = normalize(request.getEventType());
        if (journalEntryRepository.existsBySellerAndSourceTypeAndSourceIdAndEventType(
                seller, sourceType, request.getSourceId(), eventType)) {
            throw new RuntimeException("Journal entry already posted for this source event");
        }

        FinancialYear year = financialYearRepository.findForDate(seller, request.getEntryDate())
                .orElseThrow(() -> new RuntimeException("Financial year not configured for entry date"));
        if (year.isClosed()) throw new RuntimeException("Financial year is closed");

        BigDecimal debitTotal = BigDecimal.ZERO;
        BigDecimal creditTotal = BigDecimal.ZERO;
        JournalEntry entry = new JournalEntry();
        entry.setSeller(seller);
        entry.setFinancialYear(year);
        entry.setEntryDate(request.getEntryDate());
        entry.setSourceType(sourceType);
        entry.setSourceId(request.getSourceId());
        entry.setEventType(eventType);
        entry.setStatus(JournalEntryStatus.POSTED);
        entry.setDescription(request.getDescription());
        entry.setCreatedBy(request.getCreatedBy());

        for (JournalLineRequest lineRequest : request.getLines()) {
            LedgerAccount account = ledgerAccountRepository.findById(lineRequest.getLedgerAccountId())
                    .orElseThrow(() -> new RuntimeException("Ledger account not found"));
            if (account.getSeller() == null || !seller.getId().equals(account.getSeller().getId())) {
                throw new RuntimeException("Ledger account does not belong to this seller");
            }

            BigDecimal debit = amount(lineRequest.getDebitAmount());
            BigDecimal credit = amount(lineRequest.getCreditAmount());
            if (debit.signum() > 0 && credit.signum() > 0) {
                throw new RuntimeException("One journal line cannot have both debit and credit");
            }
            if (debit.signum() == 0 && credit.signum() == 0) {
                throw new RuntimeException("Journal line amount cannot be zero");
            }

            JournalLine line = new JournalLine();
            line.setLedgerAccount(account);
            line.setPartyType(blankToNull(lineRequest.getPartyType()));
            line.setPartyId(lineRequest.getPartyId());
            line.setDebitAmount(debit);
            line.setCreditAmount(credit);
            line.setNarration(lineRequest.getNarration());
            entry.addLine(line);
            debitTotal = debitTotal.add(debit);
            creditTotal = creditTotal.add(credit);
        }

        if (debitTotal.compareTo(creditTotal) != 0) {
            throw new RuntimeException("Journal entry is not balanced. Debit " + debitTotal + " Credit " + creditTotal);
        }

        return journalEntryRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<JournalEntry> list(User seller, java.time.LocalDate from, java.time.LocalDate to) {
        validateSeller(seller);
        java.time.LocalDate end = to == null ? java.time.LocalDate.now() : to;
        java.time.LocalDate start = from == null ? end.withDayOfMonth(1) : from;
        return journalEntryRepository.findBySellerAndEntryDateBetweenOrderByEntryDateAscIdAsc(seller, start, end);
    }

    private BigDecimal amount(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) throw new RuntimeException("Journal source and event are required");
        return value.trim().toUpperCase();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validateSeller(User seller) {
        if (seller == null || seller.getId() == null || seller.getRole() != Role.SELLER || !seller.isEnabled()) {
            throw new RuntimeException("Only active sellers can post journal entries");
        }
    }
}
