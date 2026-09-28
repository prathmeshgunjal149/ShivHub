package com.shivhub.backend.service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.shivhub.backend.dto.SellerGstReportResponse;
import com.shivhub.backend.dto.SellerSalesReportRow;
import com.shivhub.backend.entity.AfterSalesCreditNote;
import com.shivhub.backend.entity.CustomerReceivable;
import com.shivhub.backend.entity.OfflineBill;
import com.shivhub.backend.entity.OfflineBillItem;
import com.shivhub.backend.entity.Purchase;
import com.shivhub.backend.entity.PurchaseItem;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.PurchaseStatus;
import com.shivhub.backend.repository.AfterSalesCreditNoteRepository;
import com.shivhub.backend.repository.CustomerReceivablePaymentRepository;
import com.shivhub.backend.repository.CustomerReceivableRepository;
import com.shivhub.backend.repository.OfflineBillRepository;
import com.shivhub.backend.repository.PurchaseRepository;
import com.shivhub.backend.repository.UserRepository;

/**
 * Read-only tax and party reporting.  It deliberately consumes existing
 * invoice/purchase snapshots; no GST is recalculated or posted from this page.
 */
@Service
public class SellerGstReportService {
    private final UserRepository users;
    private final OfflineBillRepository bills;
    private final PurchaseRepository purchases;
    private final SellerSalesReportService sales;
    private final CustomerReceivableRepository receivables;
    private final CustomerReceivablePaymentRepository receivablePayments;
    private final AfterSalesCreditNoteRepository creditNotes;

    public SellerGstReportService(UserRepository users, OfflineBillRepository bills, PurchaseRepository purchases,
            SellerSalesReportService sales, CustomerReceivableRepository receivables,
            CustomerReceivablePaymentRepository receivablePayments, AfterSalesCreditNoteRepository creditNotes) {
        this.users = users; this.bills = bills; this.purchases = purchases; this.sales = sales;
        this.receivables = receivables; this.receivablePayments = receivablePayments; this.creditNotes = creditNotes;
    }

    @Transactional(readOnly = true)
    public SellerGstReportResponse report(String email, String type, LocalDate fromInput, LocalDate toInput,
            String query, String party, String gstin, int page, int size) {
        User seller = seller(email);
        LocalDate from = fromInput == null ? LocalDate.now().withDayOfMonth(1) : fromInput;
        LocalDate to = toInput == null ? LocalDate.now() : toInput;
        if (to.isBefore(from)) throw new IllegalArgumentException("End date cannot be before start date");
        int safePage = Math.max(page, 0), safeSize = Math.min(Math.max(size, 1), 100000);
        String report = normalizeType(type);
        List<Row> rows = rows(seller, report, from, to);
        String needle = normalize(query), partyNeedle = normalize(party), gstNeedle = normalize(gstin);
        rows = rows.stream().filter(r -> matches(r, needle, partyNeedle, gstNeedle)).sorted(Comparator.comparing(Row::date, Comparator.nullsLast(Comparator.reverseOrder()))).toList();
        Map<String, BigDecimal> totals = totals(rows);
        int start = Math.min(safePage * safeSize, rows.size()), end = Math.min(start + safeSize, rows.size());
        return new SellerGstReportResponse(report, from, to, totals, rows.subList(start, end).stream().map(Row::map).toList(), rows.size(), safePage, safeSize, (int) Math.ceil((double) rows.size() / safeSize));
    }

    @Transactional(readOnly = true)
    public byte[] download(String email, String type, LocalDate from, LocalDate to, String query, String party, String gstin, String format) {
        SellerGstReportResponse report = report(email, type, from, to, query, party, gstin, 0, 100000);
        String csv = csv(report);
        if ("PDF".equalsIgnoreCase(format)) return pdf(report, csv);
        // SpreadsheetML opens directly in Excel and avoids a second spreadsheet dependency.
        if ("XLSX".equalsIgnoreCase(format)) return excelXml(report, csv).getBytes(StandardCharsets.UTF_8);
        return csv.getBytes(StandardCharsets.UTF_8);
    }

    private List<Row> rows(User seller, String type, LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay(), end = to.plusDays(1).atStartOfDay();
        if (type.equals("GSTR2") || type.equals("PURCHASE_HSN")) return purchaseRows(seller, start, end, type.equals("PURCHASE_HSN"));
        if (type.equals("OUTSTANDING") || type.equals("LEDGER")) return receivableRows(seller, from, to, type);
        if (type.equals("ITEM_PARTIES")) return itemPartyRows(seller, start, end);
        if (type.startsWith("TDS") || type.startsWith("TCS")) return List.of();
        List<Row> result = salesRows(seller, from, to);
        if (type.equals("SALES_HSN") || type.equals("HSN_SALES")) return hsnRows(result);
        if (type.equals("GSTR3B")) return summaryRows(result, purchaseRows(seller, start, end, false));
        return result;
    }

    private List<Row> salesRows(User seller, LocalDate from, LocalDate to) {
        List<Row> rows = new ArrayList<>();
        // Reuse the existing sales report only for online lines (it excludes cancelled orders).
        // POS bills are read directly so B2B GSTIN/place-of-supply snapshots survive into GSTR-1.
        for (SellerSalesReportRow r : sales.getExportRows(seller.getEmail(), from, to, null, null, null, null)) {
            if (!"ONLINE".equals(r.source())) continue;
            rows.add(new Row(r.saleDate() == null ? null : r.saleDate().toLocalDate(), r.source(), r.referenceNumber(), r.customerName(), null, null,
                    r.productName(), null, r.taxableAmount(), r.discount(), r.cgst(), r.sgst(), r.igst(), r.total(), BigDecimal.ZERO, BigDecimal.ZERO, "COMPLETED"));
        }
        for (OfflineBill bill : bills.findBySellerIdAndCreatedAtBetweenOrderByCreatedAtDesc(seller.getId(), from.atStartOfDay(), to.plusDays(1).atStartOfDay())) {
            String product = bill.getItems() == null ? "POS sale" : bill.getItems().stream().map(OfflineBillItem::getProductName).filter(name -> name != null && !name.isBlank()).reduce((left, right) -> left + ", " + right).orElse("POS sale");
            rows.add(new Row(bill.getCreatedAt() == null ? null : bill.getCreatedAt().toLocalDate(), "POS", bill.getBillNumber(), bill.getCustomerName(), bill.getCustomerGstin(), bill.getPlaceOfSupply(), product, null,
                    zero(bill.getTaxableAmount()), zero(bill.getDiscount()), zero(bill.getCgst()), zero(bill.getSgst()), zero(bill.getIgst()), zero(bill.getGrandTotal()), zero(bill.getPaymentAmount()), BigDecimal.ZERO, bill.getPaymentStatus() == null ? "COMPLETED" : bill.getPaymentStatus().name()));
        }
        BigDecimal taxable = BigDecimal.ZERO, cgst = BigDecimal.ZERO, sgst = BigDecimal.ZERO, igst = BigDecimal.ZERO, total = BigDecimal.ZERO;
        for (AfterSalesCreditNote note : creditNotes.findSellerNotesBetween(seller.getId(), from.atStartOfDay(), to.plusDays(1).atStartOfDay())) {
            taxable = taxable.add(zero(note.getTaxableAmount())); cgst = cgst.add(zero(note.getCgst())); sgst = sgst.add(zero(note.getSgst())); igst = igst.add(zero(note.getIgst())); total = total.add(zero(note.getGrandTotal()));
        }
        if (total.signum() > 0) rows.add(new Row(to, "CREDIT_NOTE", "Approved return/credit notes", "Returns / credit notes", null, null, "Approved returns", null,
                taxable.negate(), BigDecimal.ZERO, cgst.negate(), sgst.negate(), igst.negate(), total.negate(), BigDecimal.ZERO, BigDecimal.ZERO, "APPROVED"));
        return rows;
    }

    private List<Row> purchaseRows(User seller, LocalDateTime from, LocalDateTime to, boolean hsn) {
        List<Row> rows = new ArrayList<>();
        for (Purchase p : purchases.findBySellerOrderByPurchaseDateDesc(seller)) {
            if (p.getStatus() == PurchaseStatus.CANCELLED || p.getPurchaseDate() == null || p.getPurchaseDate().isBefore(from) || !p.getPurchaseDate().isBefore(to)) continue;
            String supplier = p.getDistributor() == null || p.getDistributor().getDistributor() == null ? "Supplier" : p.getDistributor().getDistributor().getBusinessName();
            String supplierGstin = p.getDistributor() == null || p.getDistributor().getDistributor() == null ? null : p.getDistributor().getDistributor().getGstin();
            if (hsn && p.getItems() != null) for (PurchaseItem item : p.getItems()) rows.add(new Row(p.getPurchaseDate().toLocalDate(), "PURCHASE", p.getInvoiceNumber(), supplier, supplierGstin, null, item.getProductName(), item.getHsnCode(), zero(item.getTaxableAmount()), zero(item.getDiscount()), zero(item.getCgst()), zero(item.getSgst()), zero(item.getIgst()), zero(item.getTotalPrice()), BigDecimal.ZERO, BigDecimal.ZERO, p.getStatus().name()));
            else rows.add(new Row(p.getPurchaseDate().toLocalDate(), "PURCHASE", p.getInvoiceNumber(), supplier, supplierGstin, null, "Purchase invoice", null, zero(p.getSubtotal()), zero(p.getDiscount()), zero(p.getCgst()), zero(p.getSgst()), zero(p.getIgst()), zero(p.getGrandTotal()), BigDecimal.ZERO, BigDecimal.ZERO, p.getStatus().name()));
        }
        return rows;
    }

    private List<Row> hsnRows(List<Row> salesRows) {
        Map<String, Row> group = new LinkedHashMap<>();
        for (Row row : salesRows) {
            String hsn = row.hsn == null || row.hsn.isBlank() ? "UNSPECIFIED" : row.hsn;
            Row existing = group.get(hsn);
            group.put(hsn, existing == null ? row.withHsn(hsn) : existing.plus(row));
        }
        return new ArrayList<>(group.values());
    }

    private List<Row> summaryRows(List<Row> saleRows, List<Row> purchaseRows) {
        Map<String, BigDecimal> salesTotals = totals(saleRows), purchaseTotals = totals(purchaseRows);
        return List.of(new Row(null, "GSTR-3B", "Output tax", "Sales", null, null, "Output GST", null, salesTotals.get("taxable"), BigDecimal.ZERO, salesTotals.get("cgst"), salesTotals.get("sgst"), salesTotals.get("igst"), salesTotals.get("total"), BigDecimal.ZERO, BigDecimal.ZERO, "SUMMARY"),
                new Row(null, "GSTR-3B", "Input tax credit", "Purchases", null, null, "Eligible purchase ITC", null, purchaseTotals.get("taxable"), BigDecimal.ZERO, purchaseTotals.get("cgst").negate(), purchaseTotals.get("sgst").negate(), purchaseTotals.get("igst").negate(), purchaseTotals.get("total").negate(), BigDecimal.ZERO, BigDecimal.ZERO, "SUMMARY"));
    }

    private List<Row> receivableRows(User seller, LocalDate from, LocalDate to, String type) {
        List<Row> rows = new ArrayList<>();
        for (CustomerReceivable r : receivables.findBySellerOrderByDueDateAsc(seller)) {
            if (r.getSaleDate() == null || r.getSaleDate().isBefore(from) || r.getSaleDate().isAfter(to)) continue;
            BigDecimal paid = zero(receivablePayments.totalPaid(r)); BigDecimal due = zero(r.getSaleAmount()).subtract(paid).max(BigDecimal.ZERO);
            String status = due.signum() == 0 ? "PAID" : paid.signum() == 0 ? "UNPAID" : "PARTIAL";
            rows.add(new Row(r.getSaleDate(), type, r.getInvoiceNumber(), r.getCustomerName(), null, null, r.getProductDetails(), null, zero(r.getSaleAmount()), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, zero(r.getSaleAmount()), paid, due, status));
        }
        return rows;
    }

    private List<Row> itemPartyRows(User seller, LocalDateTime from, LocalDateTime to) {
        List<Row> rows = new ArrayList<>();
        for (OfflineBill bill : bills.findBySellerIdAndCreatedAtBetweenOrderByCreatedAtDesc(seller.getId(), from, to)) {
            for (OfflineBillItem item : bill.getItems()) rows.add(new Row(bill.getCreatedAt().toLocalDate(), "SALE", bill.getBillNumber(), bill.getCustomerName(), bill.getCustomerGstin(), bill.getPlaceOfSupply(), item.getProductName(), null, zero(item.getTaxableAmount()), zero(item.getDiscount()), zero(item.getCgst()), zero(item.getSgst()), zero(item.getIgst()), zero(item.getTotalPrice()), zero(bill.getPaymentAmount()), BigDecimal.ZERO, bill.getPaymentStatus() == null ? "" : bill.getPaymentStatus().name()));
        }
        return rows;
    }

    private Map<String, BigDecimal> totals(List<Row> rows) {
        BigDecimal taxable = BigDecimal.ZERO, discount = BigDecimal.ZERO, cgst = BigDecimal.ZERO, sgst = BigDecimal.ZERO, igst = BigDecimal.ZERO, total = BigDecimal.ZERO, paid = BigDecimal.ZERO, due = BigDecimal.ZERO;
        for (Row row : rows) { taxable = taxable.add(row.taxable); discount = discount.add(row.discount); cgst = cgst.add(row.cgst); sgst = sgst.add(row.sgst); igst = igst.add(row.igst); total = total.add(row.total); paid = paid.add(row.paid); due = due.add(row.due); }
        Map<String, BigDecimal> out = new LinkedHashMap<>(); out.put("taxable", taxable); out.put("discount", discount); out.put("cgst", cgst); out.put("sgst", sgst); out.put("igst", igst); out.put("tax", cgst.add(sgst).add(igst)); out.put("total", total); out.put("paid", paid); out.put("due", due); out.put("netGstPayable", cgst.add(sgst).add(igst)); return out;
    }

    private boolean matches(Row row, String query, String party, String gstin) {
        boolean textMatches = query.isEmpty() || contains(row.reference, query) || contains(row.party, query)
                || contains(row.product, query) || contains(row.hsn, query);
        return textMatches && (party.isEmpty() || contains(row.party, party)) && (gstin.isEmpty() || contains(row.gstin, gstin));
    }
    private boolean contains(String value, String query) { return value != null && value.toLowerCase(Locale.ROOT).contains(query); }
    private String normalize(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }
    private String normalizeType(String value) { String type = value == null ? "GSTR1" : value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_'); return switch(type) { case "GSTR1", "GSTR2", "GSTR3B", "SALES_HSN", "PURCHASE_HSN", "HSN_SALES", "ITEM_PARTIES", "OUTSTANDING", "LEDGER", "TDS_PAYABLE", "TDS_RECEIVABLE", "TCS_PAYABLE", "TCS_RECEIVABLE" -> type; default -> throw new IllegalArgumentException("Unknown report type");}; }
    private User seller(String email) { User seller = users.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("Seller not found")); if (seller.getRole() == null || !"SELLER".equals(seller.getRole().name())) throw new IllegalArgumentException("Only sellers can access GST reports"); return seller; }
    private BigDecimal zero(BigDecimal amount) { return amount == null ? BigDecimal.ZERO : amount; }

    private String csv(SellerGstReportResponse report) { StringBuilder out = new StringBuilder("Date,Source,Reference,Party,GSTIN,Place of Supply,Product,HSN,Taxable,Discount,CGST,SGST,IGST,Total,Paid,Due,Status\n"); for (Map<String,Object> row: report.rows()) out.append(cell(row.get("date"))).append(',').append(cell(row.get("source"))).append(',').append(cell(row.get("reference"))).append(',').append(cell(row.get("party"))).append(',').append(cell(row.get("gstin"))).append(',').append(cell(row.get("placeOfSupply"))).append(',').append(cell(row.get("product"))).append(',').append(cell(row.get("hsn"))).append(',').append(row.get("taxable")).append(',').append(row.get("discount")).append(',').append(row.get("cgst")).append(',').append(row.get("sgst")).append(',').append(row.get("igst")).append(',').append(row.get("total")).append(',').append(row.get("paid")).append(',').append(row.get("due")).append(',').append(cell(row.get("status"))).append('\n'); return out.toString(); }
    private String cell(Object value) { String text = value == null ? "" : String.valueOf(value); return '"' + text.replace("\"", "\"\"") + '"'; }
    private String excelXml(SellerGstReportResponse r, String csv) {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\"?><Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\"><Worksheet ss:Name=\"").append(xml(r.report())).append("\"><Table>");
        for (String line : csv.split("\\R")) { xml.append("<Row>"); for (String value : line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1)) xml.append("<Cell><Data ss:Type=\"String\">").append(xml(value.replaceAll("^\"|\"$", "").replace("\"\"", "\""))).append("</Data></Cell>"); xml.append("</Row>"); }
        return xml.append("</Table></Worksheet></Workbook>").toString();
    }
    private String xml(String value) { return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
    private byte[] pdf(SellerGstReportResponse report, String csv) { try { ByteArrayOutputStream out = new ByteArrayOutputStream(); Document d = new Document(); PdfWriter.getInstance(d,out); d.open(); d.add(new Paragraph("ShivHub " + report.report() + " Report")); d.add(new Paragraph("Period: " + report.from() + " to " + report.to())); d.add(new Paragraph("\n" + csv)); d.close(); return out.toByteArray(); } catch(Exception e) { throw new IllegalStateException("Could not create GST PDF", e); } }

    private record Row(LocalDate date, String source, String reference, String party, String gstin, String placeOfSupply, String product, String hsn, BigDecimal taxable, BigDecimal discount, BigDecimal cgst, BigDecimal sgst, BigDecimal igst, BigDecimal total, BigDecimal paid, BigDecimal due, String status) {
        Row withHsn(String value) { return new Row(date,source,reference,party,gstin,placeOfSupply,product,value,taxable,discount,cgst,sgst,igst,total,paid,due,status); }
        Row plus(Row r) { return new Row(null,source,"HSN summary",party,gstin,placeOfSupply,"HSN summary",hsn,taxable.add(r.taxable),discount.add(r.discount),cgst.add(r.cgst),sgst.add(r.sgst),igst.add(r.igst),total.add(r.total),paid.add(r.paid),due.add(r.due),"SUMMARY"); }
        Map<String,Object> map() { Map<String,Object> m = new LinkedHashMap<>(); m.put("date",date);m.put("source",source);m.put("reference",reference);m.put("party",party);m.put("gstin",gstin);m.put("placeOfSupply",placeOfSupply);m.put("product",product);m.put("hsn",hsn);m.put("taxable",taxable);m.put("discount",discount);m.put("cgst",cgst);m.put("sgst",sgst);m.put("igst",igst);m.put("total",total);m.put("paid",paid);m.put("due",due);m.put("status",status);return m; }
    }
}
