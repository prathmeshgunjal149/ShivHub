package com.shivhub.backend.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shivhub.backend.entity.JournalLine;
import com.shivhub.backend.entity.User;

public interface JournalLineRepository extends JpaRepository<JournalLine, Long> {

    @Query("""
            select line from JournalLine line
            join fetch line.journalEntry entry
            join fetch line.ledgerAccount account
            where entry.seller = :seller
              and entry.entryDate between :from and :to
            order by entry.entryDate asc, entry.id asc, line.id asc
            """)
    List<JournalLine> findLedgerLines(
            @Param("seller") User seller,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );
}
