package com.shivhub.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shivhub.backend.entity.CustomerReceivable;
import com.shivhub.backend.entity.Distributor;
import com.shivhub.backend.entity.Purchase;
import com.shivhub.backend.entity.SellerPaymentReminderLog;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.repository.CustomerReceivablePaymentRepository;
import com.shivhub.backend.repository.CustomerReceivableRepository;
import com.shivhub.backend.repository.DistributorCreditNoteRepository;
import com.shivhub.backend.repository.PurchasePaymentRepository;
import com.shivhub.backend.repository.PurchaseRepository;
import com.shivhub.backend.repository.SellerPaymentReminderLogRepository;
import com.shivhub.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/** Sends one private consolidated payment summary to each active seller at 08:00 IST. */
@Service
@RequiredArgsConstructor
public class PaymentReminderService {
    private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");
    private final CustomerReceivableRepository receivables;
    private final CustomerReceivablePaymentRepository receivablePayments;
    private final PurchaseRepository purchases;
    private final PurchasePaymentRepository purchasePayments;
    private final DistributorCreditNoteRepository creditNotes;
    private final SellerPaymentReminderLogRepository reminderLogs;
    private final UserRepository users;
    private final EmailService emailService;

    @Value("${shivhub.reminders.send-empty:false}")
    private boolean sendEmptySummary;

    @Value("${shivhub.reminders.dashboard-url:http://localhost:5173/seller/dashboard}")
    private String sellerDashboardUrl;

    @Scheduled(cron = "${shivhub.reminders.seller-payment-cron:0 0 8 * * *}", zone = "${shivhub.reminders.zone:Asia/Kolkata}")
    public void sendDailySellerPaymentReminders() {
        LocalDate today = LocalDate.now(INDIA);
        for (User seller : users.findByRoleAndEnabled(Role.SELLER, true)) {
            try { sendForSeller(seller, today, false); }
            catch (Exception ignored) { /* A problem for one seller must not halt all remaining reminders. */ }
        }
    }

    @Transactional
    public boolean sendForSeller(User seller, LocalDate today, boolean manualTrigger) {
        if (seller == null || seller.getId() == null || seller.getRole() != Role.SELLER || !seller.isEnabled()) {
            throw new IllegalArgumentException("An active seller account is required");
        }
        SellerPaymentReminderLog log = reminderLogs.findBySellerAndReminderDate(seller, today).orElse(null);
        if (log != null && "SENT".equals(log.getStatus()) && !manualTrigger) return false;

        try {
            List<DueRow> customerRows = customerDues(seller, today);
            List<DueRow> distributorRows = distributorDues(seller, today);
            if (customerRows.isEmpty() && distributorRows.isEmpty() && !sendEmptySummary) return false;
            if (seller.getEmail() == null || seller.getEmail().isBlank()) {
                throw new IllegalStateException("Seller email is not configured");
            }
            emailService.sendSellerDailyPaymentReminder(seller.getEmail(), shopName(seller), report(today, customerRows, distributorRows));
            saveLog(log, seller, today, "SENT", null);
            return true;
        } catch (Exception exception) {
            saveLog(log, seller, today, "FAILED", safeMessage(exception));
            return false;
        }
    }

    private List<DueRow> customerDues(User seller, LocalDate today) {
        List<DueRow> output = new ArrayList<>();
        for (CustomerReceivable row : receivables.findBySellerOrderByDueDateAsc(seller)) {
            BigDecimal paid = money(receivablePayments.totalPaid(row));
            BigDecimal remaining = money(row.getSaleAmount()).subtract(paid).max(BigDecimal.ZERO);
            if (remaining.signum() == 0 || row.getDueDate() == null || row.getDueDate().isAfter(today)) continue;
            output.add(new DueRow(row.getCustomerName(), row.getCustomerMobile(), row.getCustomerEmail(), row.getInvoiceNumber(), row.getDueDate(), money(row.getSaleAmount()), paid, remaining, daysLate(row.getDueDate(), today)));
        }
        return output;
    }

    private List<DueRow> distributorDues(User seller, LocalDate today) {
        List<DueRow> output = new ArrayList<>();
        for (Purchase purchase : purchases.findBySellerIdOrderByPurchaseDateDesc(seller.getId())) {
            if (purchase.getDistributor() == null || purchase.getDistributor().getCreditPeriodDays() == null || purchase.getPurchaseDate() == null) continue;
            LocalDate dueDate = purchase.getPurchaseDate().toLocalDate().plusDays(Math.max(0, purchase.getDistributor().getCreditPeriodDays()));
            if (dueDate.isAfter(today)) continue;
            BigDecimal paid = purchasePayments.findByPurchaseOrderByPaymentDateDesc(purchase).stream().map(payment -> money(payment.getAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal total = money(purchase.getGrandTotal()).subtract(money(creditNotes.totalForPurchase(purchase))).max(BigDecimal.ZERO);
            BigDecimal remaining = total.subtract(paid).max(BigDecimal.ZERO);
            if (remaining.signum() == 0) continue;
            Distributor distributor = purchase.getDistributor().getDistributor();
            output.add(new DueRow(distributor == null ? "Distributor" : distributor.getBusinessName(), distributor == null ? null : distributor.getMobile(), distributor == null ? null : distributor.getEmail(), purchase.getInvoiceNumber(), dueDate, total, paid, remaining, daysLate(dueDate, today)));
        }
        return output;
    }

    private String report(LocalDate date, List<DueRow> customers, List<DueRow> distributors) {
        StringBuilder body = new StringBuilder("DAILY PAYMENT SUMMARY — ").append(date).append(" (IST)\n");
        appendSection(body, "CUSTOMER PAYMENTS TO COLLECT", customers);
        appendSection(body, "DISTRIBUTOR PAYMENTS TO MAKE", distributors);
        body.append("\nCustomer outstanding total: ₹").append(total(customers)).append("\nDistributor outstanding total: ₹").append(total(distributors));
        body.append("\n\nOpen your seller dashboard: ").append(sellerDashboardUrl);
        return body.toString();
    }

    private void appendSection(StringBuilder body, String title, List<DueRow> rows) {
        body.append("\n\n").append(title).append("\n");
        if (rows.isEmpty()) { body.append("No due or overdue payments today.\n"); return; }
        for (DueRow row : rows) body.append("• ").append(text(row.name())).append(" | ").append(text(row.reference())).append(" | Due ").append(row.dueDate())
                .append(" | Remaining ₹").append(row.remaining()).append(row.overdueDays() > 0 ? " | " + row.overdueDays() + " day(s) overdue" : " | Due today")
                .append(" | ").append(text(row.mobile())).append(" | ").append(text(row.email())).append('\n');
    }

    private void saveLog(SellerPaymentReminderLog current, User seller, LocalDate date, String status, String failure) {
        SellerPaymentReminderLog log = current == null ? new SellerPaymentReminderLog() : current;
        log.setSeller(seller); log.setReminderDate(date); log.setStatus(status); log.setSentAt("SENT".equals(status) ? java.time.LocalDateTime.now(INDIA) : null); log.setFailureReason(failure);
        reminderLogs.save(log);
    }
    private BigDecimal money(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private long daysLate(LocalDate dueDate, LocalDate today) { return Math.max(0, ChronoUnit.DAYS.between(dueDate, today)); }
    private BigDecimal total(List<DueRow> rows) { return rows.stream().map(DueRow::remaining).reduce(BigDecimal.ZERO, BigDecimal::add); }
    private String shopName(User seller) { return seller.getBusinessName() == null || seller.getBusinessName().isBlank() ? seller.getName() : seller.getBusinessName(); }
    private String text(String value) { return value == null || value.isBlank() ? "—" : value; }
    private String safeMessage(Exception exception) { String message = exception.getMessage() == null ? "Email delivery failed" : exception.getMessage(); return message.substring(0, Math.min(480, message.length())); }
    private record DueRow(String name, String mobile, String email, String reference, LocalDate dueDate, BigDecimal total, BigDecimal paid, BigDecimal remaining, long overdueDays) { }
}
