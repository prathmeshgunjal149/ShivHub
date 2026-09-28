package com.shivhub.backend.service;

import com.shivhub.backend.entity.*;
import com.shivhub.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.stream.Collectors;

/** Email delivery is committed independently so an SMTP failure never reverses a completed sale. */
@Service
@RequiredArgsConstructor
public class FinanceNotificationService {
    private final FinanceSaleRepository sales;
    private final EmiInstallmentRepository installments;
    private final FinanceService finance;
    private final EmailService email;
    private final WhatsAppNotificationService whatsapp;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void summary(Long id) {
        FinanceSale sale = sales.lock(id).orElseThrow();
        if (!sale.isActive() || sale.isSummarySent()) return;
        var bill = sale.getBill();
        StringBuilder text = new StringBuilder(greeting(bill)).append("Thank you for your purchase.\nInvoice: ").append(bill.getBillNumber())
                .append("\nPurchase date: ").append(bill.getCreatedAt().toLocalDate()).append("\nProducts: ").append(products(bill))
                .append("\nFull purchase amount: Rs. ").append(sale.getFullSaleAmount()).append("\nFinance provider: ").append(sale.getCompanyName())
                .append("\nDownpayment: Rs. ").append(sale.getDownpayment()).append("\nScheme: ").append(sale.getSchemeName())
                .append("; installments: ").append(sale.getTenureMonths()).append("\nLoan reference: ").append(sale.getLoanNumber() == null ? "NA" : sale.getLoanNumber()).append("\n\nEMI schedule:\n");
        for (var row : installments.findBySaleIdOrderByInstallmentNumber(id)) text.append(row.getMonthLabel()).append(" - Rs. ").append(row.getAmount()).append(" - Due: ").append(row.getDueDate()).append('\n');
        text.append("\nPlease maintain sufficient balance in your linked bank account before each EMI due date to help avoid bounce or late charges.\n\nRegards,\n").append(shop(bill));
        try { email.sendFinanceNotification(bill.getCustomerEmail(), "Purchase EMI schedule - " + bill.getBillNumber(), text.toString());
            try { whatsapp.sendCustomerEvent(bill.getCustomerEmail(), WhatsAppNotificationEvent.EMI_SCHEDULE, bill.getCustomerName(), bill.getBillNumber(), java.util.List.of(bill.getCustomerName() == null ? "Customer" : bill.getCustomerName(), bill.getBillNumber(), String.valueOf(sale.getFullSaleAmount()), String.valueOf(sale.getTenureMonths()))); } catch (Exception ignored) { }
            sale.setSummarySent(true); sale.setSummarySentAt(LocalDateTime.now()); finance.audit(id, null, "SUMMARY_SENT", "EMAIL accepted by mail transport"); }
        catch (Exception ex) { finance.audit(id, null, "SUMMARY_FAILED", "Email delivery failed; retry pending"); }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reminder(Long id, LocalDate today, boolean retry) {
        var initial = installments.findById(id).orElseThrow();
        var sale = sales.lock(initial.getSale().getId()).orElseThrow();
        var row = installments.lock(id).orElseThrow();
        boolean initialWindow = row.getDueDate().equals(today.plusDays(2));
        boolean retryWindow = retry && row.getFailureMessage() != null && !row.getDueDate().isBefore(today);
        if (!sale.isActive() || !"ACTIVE".equals(row.getStatus()) || row.isReminderSent() || (!initialWindow && !retryWindow)) return;
        var bill = sale.getBill();
        String text = greeting(bill) + "Your EMI for " + products(bill) + " under " + sale.getCompanyName() + " finance is due soon.\n\nAmount: Rs. " + row.getAmount() + "\nDue date: " + row.getDueDate() + "\nInvoice: " + bill.getBillNumber() + "\n\nPlease maintain sufficient balance in your linked bank account before the due date to help avoid bounce or late-payment charges.\n\nRegards,\n" + shop(bill);
        try { email.sendFinanceNotification(bill.getCustomerEmail(), "Upcoming EMI Reminder - Rs. " + row.getAmount() + " due on " + row.getDueDate(), text);
            try { whatsapp.sendCustomerEvent(bill.getCustomerEmail(), WhatsAppNotificationEvent.EMI_REMINDER, bill.getCustomerName(), bill.getBillNumber(), java.util.List.of(bill.getCustomerName() == null ? "Customer" : bill.getCustomerName(), bill.getBillNumber(), String.valueOf(row.getAmount()), String.valueOf(row.getDueDate()), String.valueOf(row.getInstallmentNumber()))); } catch (Exception ignored) { }
            row.setReminderSent(true); row.setReminderSentAt(LocalDateTime.now()); row.setFailureMessage(null); finance.audit(sale.getId(), null, "REMINDER_SENT", "Installment " + row.getInstallmentNumber() + "; EMAIL"); }
        catch (Exception ex) { row.setFailureMessage("Email delivery failed; eligible for controlled retry"); finance.audit(sale.getId(), null, "REMINDER_FAILED", "Installment " + row.getInstallmentNumber()); }
    }
    private String products(OfflineBill bill) { return bill.getItems().stream().map(OfflineBillItem::getProductName).collect(Collectors.joining(", ")); }
    private String shop(OfflineBill bill) { return bill.getSellerBusinessName() == null ? bill.getSellerName() : bill.getSellerBusinessName(); }
    private String greeting(OfflineBill bill) { return "Dear " + bill.getCustomerName() + ",\n\n"; }
}
