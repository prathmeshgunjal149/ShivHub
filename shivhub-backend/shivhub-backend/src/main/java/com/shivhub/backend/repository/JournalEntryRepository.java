package com.shivhub.backend.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shivhub.backend.entity.JournalEntry;
import com.shivhub.backend.entity.User;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {

    boolean existsBySellerAndSourceTypeAndSourceIdAndEventType(
            User seller,
            String sourceType,
            Long sourceId,
            String eventType
    );

    Optional<JournalEntry> findBySellerAndSourceTypeAndSourceIdAndEventType(
            User seller,
            String sourceType,
            Long sourceId,
            String eventType
    );

    List<JournalEntry> findBySellerAndEntryDateBetweenOrderByEntryDateAscIdAsc(
            User seller,
            LocalDate from,
            LocalDate to
    );
}
