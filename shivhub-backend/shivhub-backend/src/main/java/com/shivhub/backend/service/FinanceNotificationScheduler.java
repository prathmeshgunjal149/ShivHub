package com.shivhub.backend.service;

import com.shivhub.backend.repository.EmiInstallmentRepository;
import com.shivhub.backend.repository.FinanceSaleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;
import java.time.*;

@Component
@RequiredArgsConstructor
public class FinanceNotificationScheduler {
    private static final Logger LOG = LoggerFactory.getLogger(FinanceNotificationScheduler.class);
    private final FinanceNotificationService notifications;
    private final EmiInstallmentRepository installments;
    private final FinanceSaleRepository sales;
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void committed(FinanceService.Committed event) { try { notifications.summary(event.saleId()); } catch (Exception ex) { LOG.warn("Finance summary pending retry for record {}", event.saleId()); } }
    @Scheduled(cron = "${shivhub.finance.reminder-cron:0 0 8 * * *}", zone = "Asia/Kolkata")
    public void daily() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata")); long after = 0;
        while (true) { var ids = installments.failedBeforeDue(today, after, PageRequest.of(0, 100)); if (ids.isEmpty()) break; for (Long id : ids) { try { notifications.reminder(id, today, true); } catch (Exception ex) { LOG.warn("EMI notification pending retry for installment {}", id); } after = id; } }
        after = 0;
        while (true) { var ids = installments.due(today.plusDays(2), after, PageRequest.of(0, 100)); if (ids.isEmpty()) break; for (Long id : ids) { try { notifications.reminder(id, today, false); } catch (Exception ex) { LOG.warn("EMI notification pending retry for installment {}", id); } after = id; } }
        for (Long id : sales.summaries(PageRequest.of(0, 100))) { try { notifications.summary(id); } catch (Exception ex) { LOG.warn("Finance summary pending retry for record {}", id); } }
    }
}
