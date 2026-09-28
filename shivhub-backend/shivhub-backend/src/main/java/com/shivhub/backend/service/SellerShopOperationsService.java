package com.shivhub.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shivhub.backend.dto.InstantMobileBillCancelRequest;
import com.shivhub.backend.dto.InstantMobileBillRequest;
import com.shivhub.backend.dto.InstantMobileBillResponse;
import com.shivhub.backend.dto.SellerCustomerLookupResponse;
import com.shivhub.backend.dto.SellerCustomerUpsertRequest;
import com.shivhub.backend.dto.ShopRegisterAuditResponse;
import com.shivhub.backend.dto.ShopRegisterEntryRequest;
import com.shivhub.backend.dto.ShopRegisterEntryResponse;
import com.shivhub.backend.dto.ShopRegisterHistoryResponse;
import com.shivhub.backend.dto.ShopRegisterOpeningCashRequest;
import com.shivhub.backend.dto.ShopRegisterSummaryResponse;
import com.shivhub.backend.entity.InstantMobileBill;
import com.shivhub.backend.entity.ShopRegisterAudit;
import com.shivhub.backend.entity.ShopRegisterEntry;
import com.shivhub.backend.entity.ShopRegisterOpeningCash;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.InstantMobileBillStatus;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.enums.ShopPaymentMode;
import com.shivhub.backend.enums.ShopRegisterEntryType;
import com.shivhub.backend.repository.InstantMobileBillRepository;
import com.shivhub.backend.repository.ShopRegisterAuditRepository;
import com.shivhub.backend.repository.ShopRegisterEntryRepository;
import com.shivhub.backend.repository.ShopRegisterOpeningCashRepository;
import com.shivhub.backend.repository.UserRepository;

/**
 * Independent, non-GST shop operations. This service never calls Product,
 * Inventory, Purchase, Order, GST or the standard OfflineBilling service.
 */
@Service
@Transactional
public class SellerShopOperationsService {
    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;

    private final UserRepository users;
    private final ShopRegisterEntryRepository entries;
    private final ShopRegisterOpeningCashRepository openingCash;
    private final ShopRegisterAuditRepository audits;
    private final InstantMobileBillRepository instantBills;
    private final SellerCustomerService customers;

    public SellerShopOperationsService(
            UserRepository users,
            ShopRegisterEntryRepository entries,
            ShopRegisterOpeningCashRepository openingCash,
            ShopRegisterAuditRepository audits,
            InstantMobileBillRepository instantBills,
            SellerCustomerService customers) {
        this.users = users;
        this.entries = entries;
        this.openingCash = openingCash;
        this.audits = audits;
        this.instantBills = instantBills;
        this.customers = customers;
    }

    public ShopRegisterEntryResponse addEntry(String sellerEmail, ShopRegisterEntryRequest request) {
        User seller = seller(sellerEmail);
        validateEntry(request);
        ShopRegisterEntry entry = new ShopRegisterEntry();
        entry.setSeller(seller);
        apply(entry, request);
        entry.setCreatedByName(actorName(seller));
        ShopRegisterEntry saved = entries.save(entry);
        audit(seller, saved.getId(), "CREATE", actorName(seller), null, snapshot(saved), request.getReason());
        return entryResponse(saved);
    }

    public ShopRegisterEntryResponse updateEntry(String sellerEmail, Long entryId, ShopRegisterEntryRequest request) {
        User seller = seller(sellerEmail);
        validateEntry(request);
        ShopRegisterEntry entry = ownedEntry(seller, entryId);
        String before = snapshot(entry);
        apply(entry, request);
        ShopRegisterEntry saved = entries.save(entry);
        audit(seller, saved.getId(), "UPDATE", actorName(seller), before, snapshot(saved), requireReason(request.getReason(), "Enter a reason for this edit"));
        return entryResponse(saved);
    }

    public void deleteEntry(String sellerEmail, Long entryId, String reason) {
        User seller = seller(sellerEmail);
        ShopRegisterEntry entry = ownedEntry(seller, entryId);
        String before = snapshot(entry);
        entry.setActive(false);
        entries.save(entry);
        audit(seller, entry.getId(), "DELETE", actorName(seller), before, null, requireReason(reason, "Enter a reason for deleting this entry"));
    }

    @Transactional(readOnly = true)
    public List<ShopRegisterEntryResponse> listEntries(String sellerEmail, LocalDate from, LocalDate to, String query) {
        User seller = seller(sellerEmail);
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(30) : from;
        if (start.isAfter(end)) throw new IllegalArgumentException("From date cannot be after to date");
        String search = clean(query).toLowerCase(Locale.ROOT);
        return entries.findBySellerAndActiveTrueAndEntryAtBetweenOrderByEntryAtDesc(seller, start.atStartOfDay(), end.atTime(LocalTime.MAX))
                .stream().filter(entry -> search.isBlank() || searchable(entry).contains(search)).map(this::entryResponse).toList();
    }

    public ShopRegisterSummaryResponse saveOpeningCash(String sellerEmail, ShopRegisterOpeningCashRequest request) {
        User seller = seller(sellerEmail);
        if (request.openingCash().signum() < 0) throw new IllegalArgumentException("Opening cash cannot be negative");
        ShopRegisterOpeningCash row = openingCash.findBySellerAndBusinessDate(seller, request.businessDate()).orElseGet(ShopRegisterOpeningCash::new);
        String old = row.getId() == null ? null : "openingCash=" + money(row.getOpeningCash());
        row.setSeller(seller);
        row.setBusinessDate(request.businessDate());
        row.setOpeningCash(scale(request.openingCash()));
        row.setEnteredByName(actorName(seller));
        ShopRegisterOpeningCash saved = openingCash.save(row);
        audit(seller, null, old == null ? "OPENING_CASH_CREATE" : "OPENING_CASH_UPDATE", actorName(seller), old,
                "openingCash=" + money(saved.getOpeningCash()) + ";date=" + saved.getBusinessDate(), request.reason());
        return summary(seller, request.businessDate());
    }

    @Transactional(readOnly = true)
    public ShopRegisterSummaryResponse summary(String sellerEmail, LocalDate date) { return summary(seller(sellerEmail), date == null ? LocalDate.now() : date); }

    @Transactional(readOnly = true)
    public List<ShopRegisterHistoryResponse> history(String sellerEmail, LocalDate from, LocalDate to) {
        User seller = seller(sellerEmail);
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(30) : from;
        if (start.isAfter(end)) throw new IllegalArgumentException("From date cannot be after to date");
        List<ShopRegisterHistoryResponse> result = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            ShopRegisterSummaryResponse row = summary(seller, day);
            if (row.entryCount() > 0 || row.openingCash().signum() != 0) result.add(new ShopRegisterHistoryResponse(day, row.openingCash(), row.expectedClosingCash(), row.cashSales(), row.onlineCollection(), row.totalExpense(), row.entryCount()));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<ShopRegisterAuditResponse> audits(String sellerEmail, Long entryId) {
        User seller = seller(sellerEmail);
        return audits.findBySellerAndEntryIdOrderByCreatedAtDesc(seller, entryId).stream()
                .map(row -> new ShopRegisterAuditResponse(row.getId(), row.getEntryId(), row.getAction(), row.getActionByName(), row.getPreviousSnapshot(), row.getNextSnapshot(), row.getReason(), row.getCreatedAt())).toList();
    }

    @Transactional(readOnly = true)
    public byte[] csv(String sellerEmail, LocalDate from, LocalDate to, String query) {
        StringBuilder out = new StringBuilder("Date,Time,Type,Category,Item,Payment mode,Amount,Customer,Mobile,Created by,Notes\n");
        for (ShopRegisterEntryResponse row : listEntries(sellerEmail, from, to, query)) {
            out.append(csv(row.entryAt().toLocalDate())).append(',').append(csv(row.entryAt().toLocalTime().withNano(0))).append(',')
                    .append(csv(row.entryType())).append(',').append(csv(row.category())).append(',').append(csv(row.itemName())).append(',')
                    .append(csv(row.paymentMode())).append(',').append(csv(row.amount())).append(',').append(csv(row.customerName())).append(',')
                    .append(csv(row.customerMobile())).append(',').append(csv(row.createdByName())).append(',').append(csv(row.notes())).append('\n');
        }
        return out.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public InstantMobileBillResponse createInstantBill(String sellerEmail, InstantMobileBillRequest request) {
        User seller = seller(sellerEmail);
        BigDecimal gross = scale(request.getUnitPrice()).multiply(BigDecimal.valueOf(request.getQuantity()));
        BigDecimal discount = scale(request.getDiscount());
        if (discount.compareTo(gross) > 0) throw new IllegalArgumentException("Discount cannot exceed the bill amount");

        SellerCustomerUpsertRequest customer = new SellerCustomerUpsertRequest();
        customer.setName(clean(request.getCustomerName()));
        customer.setMobile(clean(request.getCustomerMobile()));
        customer.setEmail(blank(request.getCustomerEmail()));
        customer.setAddress(blank(request.getCustomerAddress()));
        customer.setWhatsappConsent(request.isWhatsappConsent());
        SellerCustomerLookupResponse profile = customers.registerOrUpdate(seller.getEmail(), customer);

        InstantMobileBill bill = new InstantMobileBill();
        bill.setSeller(seller);
        bill.setBillNumber("NGM-" + DAY.format(LocalDate.now()) + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT));
        bill.setCreatedByName(actorName(seller));
        bill.setCustomerName(clean(request.getCustomerName()));
        bill.setCustomerMobile(clean(request.getCustomerMobile()));
        bill.setCustomerEmail(blank(request.getCustomerEmail()));
        bill.setCustomerAddress(blank(request.getCustomerAddress()));
        bill.setCustomerId(profile.customerId());
        bill.setCustomerProfileId(profile.customerProfileId());
        bill.setWhatsappConsent(request.isWhatsappConsent());
        bill.setBrand(blank(request.getBrand())); bill.setModel(clean(request.getModel())); bill.setColor(blank(request.getColor()));
        bill.setRam(blank(request.getRam())); bill.setStorage(blank(request.getStorage())); bill.setImeiOrSerial(blank(request.getImeiOrSerial()));
        bill.setQuantity(request.getQuantity()); bill.setUnitPrice(scale(request.getUnitPrice())); bill.setDiscount(discount); bill.setTotalAmount(gross.subtract(discount));
        bill.setPaymentMode(request.getPaymentMode()); bill.setPaymentReference(blank(request.getPaymentReference())); bill.setNotes(blank(request.getNotes()));
        return instantResponse(instantBills.save(bill));
    }

    @Transactional(readOnly = true)
    public List<InstantMobileBillResponse> instantBills(String sellerEmail, LocalDate from, LocalDate to, String query) {
        User seller = seller(sellerEmail);
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(90) : from;
        String search = clean(query).toLowerCase(Locale.ROOT);
        return instantBills.findBySellerAndCreatedAtBetweenOrderByCreatedAtDesc(seller, start.atStartOfDay(), end.atTime(LocalTime.MAX)).stream()
                .filter(row -> search.isBlank() || (row.getBillNumber() + " " + row.getCustomerName() + " " + row.getCustomerMobile() + " " + row.getBrand() + " " + row.getModel() + " " + row.getCreatedByName()).toLowerCase(Locale.ROOT).contains(search))
                .map(this::instantResponse).toList();
    }

    public InstantMobileBillResponse cancelInstantBill(String sellerEmail, Long billId, InstantMobileBillCancelRequest request) {
        User seller = seller(sellerEmail);
        InstantMobileBill bill = instantBills.findBySellerAndId(seller, billId).orElseThrow(() -> new IllegalArgumentException("Instant bill not found"));
        if (bill.getStatus() == InstantMobileBillStatus.CANCELLED) throw new IllegalArgumentException("This cash memo is already cancelled");
        bill.setStatus(InstantMobileBillStatus.CANCELLED); bill.setCancellationReason(clean(request.reason())); bill.setCancelledByName(actorName(seller)); bill.setCancelledAt(LocalDateTime.now());
        return instantResponse(instantBills.save(bill));
    }

    /** Admin-safe read entry point. The admin UI can use this without mixing it into tax reports. */
    @Transactional(readOnly = true)
    public ShopRegisterSummaryResponse adminSummary(Long sellerId, LocalDate day) { return summary(sellerById(sellerId), day == null ? LocalDate.now() : day); }

    private ShopRegisterSummaryResponse summary(User seller, LocalDate day) {
        List<ShopRegisterEntry> rows = entries.findBySellerAndActiveTrueAndEntryAtBetweenOrderByEntryAtDesc(seller, day.atStartOfDay(), day.atTime(LocalTime.MAX));
        BigDecimal cashSales = BigDecimal.ZERO, cashDeposits = BigDecimal.ZERO, cashExpenses = BigDecimal.ZERO, cashWithdrawals = BigDecimal.ZERO, adjustments = BigDecimal.ZERO;
        BigDecimal upi = BigDecimal.ZERO, card = BigDecimal.ZERO, bank = BigDecimal.ZERO, other = BigDecimal.ZERO, totalExpense = BigDecimal.ZERO;
        for (ShopRegisterEntry row : rows) {
            BigDecimal amount = money(row.getAmount());
            boolean sale = row.getEntryType() == ShopRegisterEntryType.CASH_SALE || row.getEntryType() == ShopRegisterEntryType.ONLINE_SALE;
            if (sale) {
                if (row.getPaymentMode() == ShopPaymentMode.CASH) cashSales = cashSales.add(amount);
                else if (row.getPaymentMode() == ShopPaymentMode.UPI) upi = upi.add(amount);
                else if (row.getPaymentMode() == ShopPaymentMode.CARD) card = card.add(amount);
                else if (row.getPaymentMode() == ShopPaymentMode.BANK_TRANSFER) bank = bank.add(amount);
                else other = other.add(amount);
            }
            if (row.getEntryType() == ShopRegisterEntryType.CASH_DEPOSIT) cashDeposits = cashDeposits.add(amount);
            if (row.getEntryType() == ShopRegisterEntryType.CASH_WITHDRAWAL) cashWithdrawals = cashWithdrawals.add(amount);
            if (row.getEntryType() == ShopRegisterEntryType.EXPENSE) { totalExpense = totalExpense.add(amount); if (row.getPaymentMode() == ShopPaymentMode.CASH) cashExpenses = cashExpenses.add(amount); }
            if (row.getEntryType() == ShopRegisterEntryType.ADJUSTMENT) adjustments = adjustments.add(amount);
        }
        BigDecimal opening = openingCash.findBySellerAndBusinessDate(seller, day).map(ShopRegisterOpeningCash::getOpeningCash).map(this::money).orElse(BigDecimal.ZERO);
        BigDecimal online = upi.add(card).add(bank).add(other);
        BigDecimal closing = opening.add(cashSales).add(cashDeposits).subtract(cashExpenses).subtract(cashWithdrawals).add(adjustments);
        return new ShopRegisterSummaryResponse(day, scale(opening), scale(cashSales), scale(cashDeposits), scale(cashExpenses), scale(cashWithdrawals), scale(adjustments), scale(closing), scale(upi), scale(card), scale(bank), scale(other), scale(online), scale(totalExpense), scale(cashSales.add(online).subtract(totalExpense)), rows.size());
    }

    private void validateEntry(ShopRegisterEntryRequest request) {
        if (request.getAmount() == null || request.getAmount().signum() == 0) throw new IllegalArgumentException("Amount cannot be zero");
        if (request.getEntryType() != ShopRegisterEntryType.ADJUSTMENT && request.getAmount().signum() < 0) throw new IllegalArgumentException("Only an adjustment can be negative");
        if (request.getEntryAt() != null && request.getEntryAt().isAfter(LocalDateTime.now().plusMinutes(5))) throw new IllegalArgumentException("A shop entry cannot be dated in the future");
    }
    private void apply(ShopRegisterEntry entry, ShopRegisterEntryRequest request) {
        entry.setEntryType(request.getEntryType()); entry.setPaymentMode(request.getPaymentMode()); entry.setCategory(clean(request.getCategory())); entry.setItemName(blank(request.getItemName()));
        entry.setAmount(scale(request.getAmount())); entry.setEntryAt(request.getEntryAt() == null ? LocalDateTime.now() : request.getEntryAt());
        entry.setCustomerName(blank(request.getCustomerName())); entry.setCustomerMobile(blank(request.getCustomerMobile())); entry.setWhatsappConsent(request.isWhatsappConsent()); entry.setNotes(blank(request.getNotes()));
    }
    private ShopRegisterEntry ownedEntry(User seller, Long id) { return entries.findById(id).filter(row -> row.isActive() && row.getSeller().getId().equals(seller.getId())).orElseThrow(() -> new IllegalArgumentException("Shop entry not found")); }
    private void audit(User seller, Long entryId, String action, String actionBy, String previous, String next, String reason) { ShopRegisterAudit row = new ShopRegisterAudit(); row.setSeller(seller); row.setEntryId(entryId); row.setAction(action); row.setActionByName(actionBy); row.setPreviousSnapshot(previous); row.setNextSnapshot(next); row.setReason(blank(reason)); audits.save(row); }
    private String snapshot(ShopRegisterEntry row) { return "type=" + row.getEntryType() + ";payment=" + row.getPaymentMode() + ";category=" + clean(row.getCategory()) + ";item=" + clean(row.getItemName()) + ";amount=" + money(row.getAmount()) + ";at=" + row.getEntryAt() + ";customer=" + clean(row.getCustomerName()) + ";mobile=" + clean(row.getCustomerMobile()); }
    private ShopRegisterEntryResponse entryResponse(ShopRegisterEntry row) { return new ShopRegisterEntryResponse(row.getId(), row.getEntryType(), row.getPaymentMode(), row.getCategory(), row.getItemName(), row.getAmount(), row.getEntryAt(), row.getCustomerName(), row.getCustomerMobile(), row.isWhatsappConsent(), row.getNotes(), row.getCreatedByName(), row.getCreatedAt(), row.getUpdatedAt()); }
    private InstantMobileBillResponse instantResponse(InstantMobileBill row) { return new InstantMobileBillResponse(row.getId(), row.getBillNumber(), row.getStatus(), row.getCreatedByName(), row.getCustomerName(), row.getCustomerMobile(), row.getCustomerEmail(), row.getCustomerAddress(), row.isWhatsappConsent(), row.getBrand(), row.getModel(), row.getColor(), row.getRam(), row.getStorage(), row.getImeiOrSerial(), row.getQuantity(), row.getUnitPrice(), row.getDiscount(), row.getTotalAmount(), row.getPaymentMode(), row.getPaymentReference(), row.getNotes(), row.getCancellationReason(), row.getCancelledByName(), row.getCancelledAt(), row.getCreatedAt()); }
    private User seller(String email) { User user = users.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("Seller not found")); if (user.getRole() != Role.SELLER || !user.isEnabled()) throw new SecurityException("Only an active seller can access shop operations"); return user; }
    private User sellerById(Long id) { return users.findById(id).filter(user -> user.getRole() == Role.SELLER).orElseThrow(() -> new IllegalArgumentException("Seller not found")); }
    private String actorName(User seller) { return clean(seller.getName()).isBlank() ? seller.getEmail() : clean(seller.getName()); }
    private BigDecimal money(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private BigDecimal scale(BigDecimal value) { return money(value).setScale(2, RoundingMode.HALF_UP); }
    private String clean(Object value) { return value == null ? "" : String.valueOf(value).trim(); }
    private String blank(String value) { String cleaned = clean(value); return cleaned.isBlank() ? null : cleaned; }
    private String requireReason(String value, String message) { String cleaned = clean(value); if (cleaned.isBlank()) throw new IllegalArgumentException(message); return cleaned; }
    private String searchable(ShopRegisterEntry entry) { return (entry.getEntryType() + " " + entry.getPaymentMode() + " " + entry.getCategory() + " " + clean(entry.getItemName()) + " " + clean(entry.getCustomerName()) + " " + clean(entry.getCustomerMobile()) + " " + entry.getAmount() + " " + entry.getCreatedByName()).toLowerCase(Locale.ROOT); }
    private String csv(Object value) { String text = value == null ? "" : String.valueOf(value); return "\"" + text.replace("\"", "\"\"") + "\""; }
}
