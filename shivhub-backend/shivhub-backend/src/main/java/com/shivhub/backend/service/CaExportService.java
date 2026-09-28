package com.shivhub.backend.service;

import java.math.BigDecimal;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.shivhub.backend.dto.SellerSalesReportRow;
import com.shivhub.backend.entity.CustomerReceivable;
import com.shivhub.backend.entity.CustomerReceivablePayment;
import com.shivhub.backend.entity.DistributorCreditNote;
import com.shivhub.backend.entity.Product;
import com.shivhub.backend.entity.Purchase;
import com.shivhub.backend.entity.PurchaseItem;
import com.shivhub.backend.entity.PurchaseItemSerial;
import com.shivhub.backend.entity.PurchasePayment;
import com.shivhub.backend.entity.SellerExpense;
import com.shivhub.backend.entity.StockMovement;
import com.shivhub.backend.entity.StockTransferAdjustment;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.repository.CustomerReceivablePaymentRepository;
import com.shivhub.backend.repository.CustomerReceivableRepository;
import com.shivhub.backend.repository.DistributorCreditNoteRepository;
import com.shivhub.backend.repository.ProductRepository;
import com.shivhub.backend.repository.PurchaseItemSerialRepository;
import com.shivhub.backend.repository.PurchasePaymentRepository;
import com.shivhub.backend.repository.PurchaseRepository;
import com.shivhub.backend.repository.SellerExpenseRepository;
import com.shivhub.backend.repository.StockMovementRepository;
import com.shivhub.backend.repository.StockTransferAdjustmentRepository;
import com.shivhub.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CaExportService {

    private final SellerSalesReportService salesReportService;
    private final UserRepository userRepository;
    private final PurchaseRepository purchaseRepository;
    private final ProductRepository productRepository;
    private final SellerExpenseRepository expenseRepository;
    private final CustomerReceivableRepository receivableRepository;
    private final CustomerReceivablePaymentRepository receivablePaymentRepository;
    private final PurchasePaymentRepository purchasePaymentRepository;
    private final DistributorCreditNoteRepository creditNoteRepository;
    private final StockMovementRepository stockMovementRepository;
    private final PurchaseItemSerialRepository serialRepository;
    private final StockTransferAdjustmentRepository stockTransferRepository;

    @Transactional(readOnly = true)
    public byte[] salesCsv(String sellerEmail, LocalDate startDate, LocalDate endDate) {
        List<SellerSalesReportRow> rows = salesReportService.getExportRows(sellerEmail, startDate, endDate, null, null, null, null);
        StringBuilder csv = newCsv("Source,Reference Number,Date,Customer,Payment Method,Product,Quantity,Taxable Amount,CGST,SGST,IGST,Total GST,Discount,Final Invoice Amount\n");
        for (SellerSalesReportRow row : rows) {
            csv.append(cell(row.source())).append(',').append(cell(row.referenceNumber())).append(',')
                    .append(cell(text(row.saleDate()))).append(',').append(cell(row.customerName())).append(',')
                    .append(cell(row.paymentMethod())).append(',').append(cell(row.productName())).append(',')
                    .append(row.quantity() == null ? "" : row.quantity()).append(',')
                    .append(amount(row.taxableAmount())).append(',').append(amount(row.cgst())).append(',')
                    .append(amount(row.sgst())).append(',').append(amount(row.igst())).append(',')
                    .append(amount(row.gst())).append(',').append(amount(row.discount())).append(',')
                    .append(amount(row.total())).append('\n');
        }
        return bytes(csv);
    }

    @Transactional(readOnly = true)
    public byte[] purchaseCsv(String sellerEmail, LocalDate startDate, LocalDate endDate) {
        User seller = seller(sellerEmail);
        Period period = period(startDate, endDate);
        StringBuilder csv = newCsv("Purchase ID,Invoice Number,Date,Distributor,IRN,Ack No,Ack Date,E-way Bill,Mode/Terms Of Payment,Delivery Note,Reference Number,Other References,Buyer Order Number,Dispatch Document,Delivery Note Date,Dispatched Through,Destination,Terms Of Delivery,Taxable Amount,CGST,SGST,IGST,Discount,Round Off,Grand Total,Status,Invoice File,QR Image URL\n");
        for (Purchase purchase : purchaseRepository.findBySellerOrderByPurchaseDateDesc(seller)) {
            if (!between(purchase.getPurchaseDate(), period)) continue;
            csv.append(purchase.getId()).append(',').append(cell(purchase.getInvoiceNumber())).append(',')
                    .append(cell(text(purchase.getPurchaseDate()))).append(',').append(cell(distributorName(purchase))).append(',')
                    .append(cell(purchase.getIrn())).append(',').append(cell(purchase.getAcknowledgementNumber())).append(',')
                    .append(cell(text(purchase.getAcknowledgementDate()))).append(',').append(cell(purchase.getEwayBillNumber())).append(',')
                    .append(cell(purchase.getModeTermsOfPayment())).append(',').append(cell(purchase.getDeliveryNote())).append(',')
                    .append(cell(purchase.getReferenceNumber())).append(',').append(cell(purchase.getOtherReferences())).append(',')
                    .append(cell(purchase.getBuyerOrderNumber())).append(',').append(cell(purchase.getDispatchDocumentNumber())).append(',')
                    .append(cell(text(purchase.getDeliveryNoteDate()))).append(',').append(cell(purchase.getDispatchedThrough())).append(',')
                    .append(cell(purchase.getDestination())).append(',').append(cell(purchase.getTermsOfDelivery())).append(',')
                    .append(amount(purchase.getSubtotal())).append(',').append(amount(purchase.getCgst())).append(',')
                    .append(amount(purchase.getSgst())).append(',').append(amount(purchase.getIgst())).append(',')
                    .append(amount(purchase.getDiscount())).append(',').append(amount(purchase.getRoundOff())).append(',')
                    .append(amount(purchase.getGrandTotal())).append(',')
                    .append(cell(purchase.getStatus() == null ? "" : purchase.getStatus().name())).append(',')
                    .append(cell(purchase.getInvoiceFileUrl())).append(',').append(cell(purchase.getEInvoiceQrImageUrl())).append('\n');
        }
        return bytes(csv);
    }

    @Transactional(readOnly = true)
    public byte[] stockCsv(String sellerEmail, LocalDate startDate, LocalDate endDate) {
        User seller = seller(sellerEmail);
        Period period = period(startDate, endDate);
        Map<Long, MovementTotals> movement = new LinkedHashMap<>();
        for (StockMovement row : stockMovementRepository.findBySellerOrderByCreatedAtDesc(seller)) {
            if (!between(row.getCreatedAt(), period) || row.getProduct() == null) continue;
            movement.computeIfAbsent(row.getProduct().getId(), id -> new MovementTotals()).add(row);
        }
        StringBuilder csv = newCsv("Product ID,Product,Current Stock,Reserved Stock,Saleable Stock,Opening Qty,Purchases,Sales,Transfers,Returns/Adjustments,Closing Qty,Estimated Closing Value\n");
        for (Product product : productRepository.findBySeller(seller)) {
            MovementTotals totals = movement.getOrDefault(product.getId(), new MovementTotals());
            int closing = product.getStock() == null ? 0 : product.getStock();
            int opening = closing - totals.netQuantity;
            int reserved = product.getReservedStock() == null ? 0 : product.getReservedStock();
            BigDecimal price = product.getFinalSellingPrice() == null ? BigDecimal.ZERO : product.getFinalSellingPrice();
            csv.append(product.getId()).append(',').append(cell(product.getName())).append(',').append(closing).append(',')
                    .append(reserved).append(',').append(product.getAvailableStock()).append(',').append(opening).append(',')
                    .append(totals.purchases).append(',').append(totals.sales).append(',').append(totals.transfers).append(',')
                    .append(totals.adjustments).append(',').append(closing).append(',')
                    .append(amount(price.multiply(BigDecimal.valueOf(closing)))).append('\n');
        }
        csv.append('\n').append("IMEI/Serial ID,Product,IMEI 1,IMEI 2,Serial Number,Status,Sold At,Offline Bill,Transfer ID\n");
        for (PurchaseItemSerial serial : serialRepository.findByPurchaseItemPurchaseSeller(seller)) {
            csv.append(serial.getId()).append(',').append(cell(serial.getPurchaseItem() == null ? "" : serial.getPurchaseItem().getProductName())).append(',')
                    .append(cell(serial.getImei1())).append(',').append(cell(serial.getImei2())).append(',')
                    .append(cell(serial.getSerialNumber())).append(',').append(cell(serial.getStatus())).append(',')
                    .append(cell(text(serial.getSoldAt()))).append(',')
                    .append(cell(serial.getSoldOfflineBillItem() == null || serial.getSoldOfflineBillItem().getOfflineBill() == null ? "" : serial.getSoldOfflineBillItem().getOfflineBill().getBillNumber())).append(',')
                    .append(serial.getTransferAdjustment() == null ? "" : serial.getTransferAdjustment().getId()).append('\n');
        }
        return bytes(csv);
    }

    @Transactional(readOnly = true)
    public byte[] expenseCsv(String sellerEmail, LocalDate startDate, LocalDate endDate) {
        User seller = seller(sellerEmail);
        Period period = period(startDate, endDate);
        StringBuilder csv = newCsv("Date,Category,Description,Payment Method,Amount,Notes\n");
        for (SellerExpense expense : expenseRepository.findBySellerAndExpenseDateBetweenOrderByExpenseDateDesc(seller, period.start(), period.end())) {
            csv.append(cell(text(expense.getExpenseDate()))).append(',').append(cell(expense.getCategory())).append(',')
                    .append(cell(expense.getDescription())).append(',').append(cell(expense.getPaymentMethod())).append(',')
                    .append(amount(expense.getAmount())).append(',').append(cell(expense.getNotes())).append('\n');
        }
        return bytes(csv);
    }

    @Transactional(readOnly = true)
    public byte[] receivablesCsv(String sellerEmail, LocalDate startDate, LocalDate endDate) {
        User seller = seller(sellerEmail);
        Period period = period(startDate, endDate);
        StringBuilder csv = newCsv("Customer,Mobile,Email,Invoice,Sale Date,Due Date,Credit Sale,Received,Closing Balance,Product Details,IMEI\n");
        for (CustomerReceivable receivable : receivableRepository.findBySellerOrderByDueDateAsc(seller)) {
            if (!between(receivable.getSaleDate(), period)) continue;
            BigDecimal received = receivablePaymentRepository.totalPaid(receivable);
            BigDecimal closing = amount(receivable.getSaleAmount()).subtract(amount(received)).max(BigDecimal.ZERO);
            csv.append(cell(receivable.getCustomerName())).append(',').append(cell(receivable.getCustomerMobile())).append(',')
                    .append(cell(receivable.getCustomerEmail())).append(',').append(cell(receivable.getInvoiceNumber())).append(',')
                    .append(cell(text(receivable.getSaleDate()))).append(',').append(cell(text(receivable.getDueDate()))).append(',')
                    .append(amount(receivable.getSaleAmount())).append(',').append(amount(received)).append(',')
                    .append(amount(closing)).append(',').append(cell(receivable.getProductDetails())).append(',')
                    .append(cell(receivable.getImei())).append('\n');
        }
        return bytes(csv);
    }

    @Transactional(readOnly = true)
    public byte[] payablesCsv(String sellerEmail, LocalDate startDate, LocalDate endDate) {
        User seller = seller(sellerEmail);
        Period period = period(startDate, endDate);
        StringBuilder csv = newCsv("Distributor,Purchase ID,Invoice,Purchase Date,Purchase Total,Payments,Credit Notes,Closing Payable\n");
        for (Purchase purchase : purchaseRepository.findBySellerOrderByPurchaseDateDesc(seller)) {
            if (!between(purchase.getPurchaseDate(), period)) continue;
            BigDecimal payments = purchasePaymentRepository.findByPurchaseOrderByPaymentDateDesc(purchase).stream()
                    .map(PurchasePayment::getAmount).map(this::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal credits = creditNoteRepository.totalForPurchase(purchase);
            BigDecimal closing = amount(purchase.getGrandTotal()).subtract(payments).subtract(amount(credits)).max(BigDecimal.ZERO);
            csv.append(cell(distributorName(purchase))).append(',').append(purchase.getId()).append(',')
                    .append(cell(purchase.getInvoiceNumber())).append(',').append(cell(text(purchase.getPurchaseDate()))).append(',')
                    .append(amount(purchase.getGrandTotal())).append(',').append(amount(payments)).append(',')
                    .append(amount(credits)).append(',').append(amount(closing)).append('\n');
        }
        return bytes(csv);
    }

    @Transactional(readOnly = true)
    public byte[] paymentsCsv(String sellerEmail, LocalDate startDate, LocalDate endDate) {
        User seller = seller(sellerEmail);
        Period period = period(startDate, endDate);
        StringBuilder csv = newCsv("Type,Date,Party,Method,Reference,Amount,Notes\n");
        for (CustomerReceivablePayment payment : receivablePaymentRepository.findBySeller(seller)) {
            if (!between(payment.getPaymentDate(), period)) continue;
            CustomerReceivable receivable = payment.getReceivable();
            csv.append("CUSTOMER_RECEIPT,").append(cell(text(payment.getPaymentDate()))).append(',')
                    .append(cell(receivable == null ? "" : receivable.getCustomerName())).append(',')
                    .append(cell(payment.getPaymentMethod() == null ? "" : payment.getPaymentMethod().name())).append(',')
                    .append(cell(payment.getTransactionReference())).append(',').append(amount(payment.getAmount())).append(',')
                    .append(cell(payment.getNotes())).append('\n');
        }
        for (PurchasePayment payment : purchasePaymentRepository.findBySeller(seller)) {
            if (!between(payment.getPaymentDate(), period)) continue;
            Purchase purchase = payment.getPurchase();
            csv.append("DISTRIBUTOR_PAYMENT,").append(cell(text(payment.getPaymentDate()))).append(',')
                    .append(cell(purchase == null ? "" : distributorName(purchase))).append(',')
                    .append(cell(payment.getPaymentMethod() == null ? "" : payment.getPaymentMethod().name())).append(',')
                    .append(cell(payment.getTransactionReference())).append(',').append(amount(payment.getAmount())).append(',')
                    .append(cell(payment.getNotes())).append('\n');
        }
        return bytes(csv);
    }

    @Transactional(readOnly = true)
    public byte[] creditDebitNoteCsv(String sellerEmail, LocalDate startDate, LocalDate endDate) {
        User seller = seller(sellerEmail);
        Period period = period(startDate, endDate);
        StringBuilder csv = newCsv("Type,Date,Number,Linked Invoice,Party,Amount,Reason,Attachment\n");
        for (DistributorCreditNote note : creditNoteRepository.findBySellerOrderByCreditNoteDateDesc(seller)) {
            if (!between(note.getCreditNoteDate(), period)) continue;
            csv.append("DISTRIBUTOR_CREDIT_NOTE,").append(cell(text(note.getCreditNoteDate()))).append(',')
                    .append(cell(note.getCreditNoteNumber())).append(',')
                    .append(cell(note.getPurchase() == null ? "" : note.getPurchase().getInvoiceNumber())).append(',')
                    .append(cell(note.getPurchase() == null ? "" : distributorName(note.getPurchase()))).append(',')
                    .append(amount(note.getAmount())).append(',').append(cell(note.getReason())).append(',')
                    .append(cell(note.getAttachmentUrl())).append('\n');
        }
        return bytes(csv);
    }

    @Transactional(readOnly = true)
    public byte[] stockTransferCsv(String sellerEmail, LocalDate startDate, LocalDate endDate) {
        User seller = seller(sellerEmail);
        Period period = period(startDate, endDate);
        StringBuilder csv = newCsv("Transfer ID,Status,Date,Product,IMEI/Serial,Receiving Shop,Contact,Distributor,Reference,Adjustment Amount,Credit Note,Adjustment Date\n");
        for (StockTransferAdjustment transfer : stockTransferRepository.findBySellerOrderByCreatedAtDesc(seller)) {
            if (!between(transfer.getCreatedAt(), period)) continue;
            csv.append(transfer.getId()).append(',').append(cell(transfer.getStatus())).append(',')
                    .append(cell(text(transfer.getCreatedAt()))).append(',')
                    .append(cell(transfer.getProduct() == null ? "" : transfer.getProduct().getName())).append(',')
                    .append(cell(transfer.getSerial() == null ? "" : serialDisplay(transfer.getSerial()))).append(',')
                    .append(cell(transfer.getReceivingShopName())).append(',').append(cell(transfer.getReceivingShopContact())).append(',')
                    .append(cell(transfer.getSellerDistributor() == null || transfer.getSellerDistributor().getDistributor() == null ? "" : transfer.getSellerDistributor().getDistributor().getBusinessName())).append(',')
                    .append(cell(transfer.getDistributorReference())).append(',').append(amount(transfer.getAdjustmentAmount())).append(',')
                    .append(cell(transfer.getCreditNoteNumber())).append(',').append(cell(text(transfer.getAdjustmentConfirmedAt()))).append('\n');
        }
        return bytes(csv);
    }

    @Transactional(readOnly = true)
    public byte[] gstSummaryCsv(String sellerEmail, LocalDate startDate, LocalDate endDate) {
        User seller = seller(sellerEmail);
        Period period = period(startDate, endDate);
        BigDecimal salesGst = BigDecimal.ZERO;
        BigDecimal salesCgst = BigDecimal.ZERO;
        BigDecimal salesSgst = BigDecimal.ZERO;
        BigDecimal salesIgst = BigDecimal.ZERO;
        BigDecimal salesTotal = BigDecimal.ZERO;
        for (SellerSalesReportRow row : salesReportService.getExportRows(sellerEmail, period.start(), period.end(), null, null, null, null)) {
            salesGst = salesGst.add(amount(row.gst()));
            salesCgst = salesCgst.add(amount(row.cgst()));
            salesSgst = salesSgst.add(amount(row.sgst()));
            salesIgst = salesIgst.add(amount(row.igst()));
            salesTotal = salesTotal.add(amount(row.total()));
        }
        BigDecimal purchaseTaxable = BigDecimal.ZERO, purchaseCgst = BigDecimal.ZERO, purchaseSgst = BigDecimal.ZERO, purchaseIgst = BigDecimal.ZERO, purchaseTotal = BigDecimal.ZERO;
        for (Purchase purchase : purchaseRepository.findBySellerOrderByPurchaseDateDesc(seller)) {
            if (!between(purchase.getPurchaseDate(), period)) continue;
            purchaseTaxable = purchaseTaxable.add(amount(purchase.getSubtotal()));
            purchaseCgst = purchaseCgst.add(amount(purchase.getCgst()));
            purchaseSgst = purchaseSgst.add(amount(purchase.getSgst()));
            purchaseIgst = purchaseIgst.add(amount(purchase.getIgst()));
            purchaseTotal = purchaseTotal.add(amount(purchase.getGrandTotal()));
        }
        StringBuilder csv = newCsv("Section,Taxable Amount,CGST,SGST,IGST,Total GST,Total Value\n");
        csv.append("Sales,").append(amount(salesTotal.subtract(salesGst).max(BigDecimal.ZERO))).append(',')
                .append(amount(salesCgst)).append(',').append(amount(salesSgst)).append(',').append(amount(salesIgst)).append(',')
                .append(amount(salesGst)).append(',').append(amount(salesTotal)).append('\n');
        csv.append("Purchases,").append(amount(purchaseTaxable)).append(',').append(amount(purchaseCgst)).append(',')
                .append(amount(purchaseSgst)).append(',').append(amount(purchaseIgst)).append(',')
                .append(amount(purchaseCgst.add(purchaseSgst).add(purchaseIgst))).append(',').append(amount(purchaseTotal)).append('\n');
        return bytes(csv);
    }

    @Transactional(readOnly = true)
    public byte[] profitLossCsv(String sellerEmail, LocalDate startDate, LocalDate endDate) {
        User seller = seller(sellerEmail);
        Period period = period(startDate, endDate);
        BigDecimal netSales = salesReportService.getExportRows(sellerEmail, period.start(), period.end(), null, null, null, null)
                .stream().map(SellerSalesReportRow::total).map(this::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal purchaseCostApprox = BigDecimal.ZERO;
        for (Purchase purchase : purchaseRepository.findBySellerOrderByPurchaseDateDesc(seller)) {
            if (!between(purchase.getPurchaseDate(), period)) continue;
            for (PurchaseItem item : purchase.getItems()) {
                purchaseCostApprox = purchaseCostApprox.add(amount(item.getUnitPrice()).multiply(BigDecimal.valueOf(item.getQuantity() == null ? 0 : item.getQuantity())));
            }
        }
        BigDecimal expenses = expenseRepository.findBySellerAndExpenseDateBetweenOrderByExpenseDateDesc(seller, period.start(), period.end())
                .stream().map(SellerExpense::getAmount).map(this::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        StringBuilder csv = newCsv("Particulars,Amount,Note\n");
        csv.append("Net Sales,").append(amount(netSales)).append(",Online plus POS sales\n");
        csv.append("Cost of Goods Sold Approximation,").append(amount(purchaseCostApprox)).append(",Uses purchase items in selected period until item-level sale cost posting is completed\n");
        csv.append("Operating Expenses,").append(amount(expenses)).append(",Seller expenses in selected period\n");
        csv.append("Profit / Loss,").append(amount(netSales.subtract(purchaseCostApprox).subtract(expenses))).append(",Final P&L should use ledger-posted COGS after automatic posting is fully wired\n");
        return bytes(csv);
    }

    @Transactional(readOnly = true)
    public byte[] excelWorkbook(String sellerEmail, LocalDate startDate, LocalDate endDate) {
        Map<String, byte[]> sheets = allCsvReports(sellerEmail, startDate, endDate);
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\"?>");
        xml.append("<?mso-application progid=\"Excel.Sheet\"?>");
        xml.append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\" ")
                .append("xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">");
        for (Map.Entry<String, byte[]> sheet : sheets.entrySet()) {
            xml.append("<Worksheet ss:Name=\"").append(xml(sheet.getKey())).append("\"><Table>");
            String csv = new String(sheet.getValue(), StandardCharsets.UTF_8);
            for (String line : csv.split("\\R")) {
                if (line.isBlank()) continue;
                xml.append("<Row>");
                for (String value : line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1)) {
                    xml.append("<Cell><Data ss:Type=\"String\">")
                            .append(xml(unquote(value)))
                            .append("</Data></Cell>");
                }
                xml.append("</Row>");
            }
            xml.append("</Table></Worksheet>");
        }
        xml.append("</Workbook>");
        return xml.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] pdfSummary(String sellerEmail, LocalDate startDate, LocalDate endDate) {
        Period period = period(startDate, endDate);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(new Paragraph("ShivHub CA Summary", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
            document.add(new Paragraph("Period: " + period.start() + " to " + period.end()));
            document.add(new Paragraph(" "));
            document.add(new Paragraph(new String(profitLossCsv(sellerEmail, startDate, endDate), StandardCharsets.UTF_8)));
            document.add(new Paragraph(" "));
            document.add(new Paragraph(new String(gstSummaryCsv(sellerEmail, startDate, endDate), StandardCharsets.UTF_8)));
            document.close();
            return out.toByteArray();
        } catch (Exception exception) {
            throw new RuntimeException("Could not create CA PDF summary", exception);
        }
    }

    @Transactional(readOnly = true)
    public byte[] documentsZip(String sellerEmail, LocalDate startDate, LocalDate endDate) {
        Period period = period(startDate, endDate);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(out)) {
                for (Map.Entry<String, byte[]> report : allCsvReports(sellerEmail, startDate, endDate).entrySet()) {
                    zip.putNextEntry(new ZipEntry("csv/" + report.getKey() + ".csv"));
                    zip.write(report.getValue());
                    zip.closeEntry();
                }

                zip.putNextEntry(new ZipEntry("summary/shivhub-ca-summary.pdf"));
                zip.write(pdfSummary(sellerEmail, startDate, endDate));
                zip.closeEntry();

                zip.putNextEntry(new ZipEntry("summary/shivhub-ca-workbook.xml"));
                zip.write(excelWorkbook(sellerEmail, startDate, endDate));
                zip.closeEntry();

                String manifest = "{\n"
                        + "  \"platform\": \"ShivHub\",\n"
                        + "  \"periodStart\": \"" + period.start() + "\",\n"
                        + "  \"periodEnd\": \"" + period.end() + "\",\n"
                        + "  \"generatedAt\": \"" + LocalDateTime.now() + "\",\n"
                        + "  \"folders\": [\"csv\", \"summary\"],\n"
                        + "  \"note\": \"CSV reports, XML workbook and PDF summary are generated from live ShivHub database records. Invoice/receipt binary attachments are included when durable document storage links are available.\"\n"
                        + "}\n";
                zip.putNextEntry(new ZipEntry("manifest.json"));
                zip.write(manifest.getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();

                zip.putNextEntry(new ZipEntry("README.txt"));
                zip.write(("ShivHub CA export bundle\n"
                        + "Period: " + period.start() + " to " + period.end() + "\n\n"
                        + "Included:\n"
                        + "- csv/: individual source reports\n"
                        + "- summary/shivhub-ca-summary.pdf: concise PDF summary\n"
                        + "- summary/shivhub-ca-workbook.xml: Excel-compatible workbook with separate sheets\n"
                        + "- manifest.json: machine-readable export manifest\n\n"
                        + "This bundle does not invent invoice files. Attachments are collected only when durable storage links exist.\n")
                        .getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            return out.toByteArray();
        } catch (Exception exception) {
            throw new RuntimeException("Could not create documents ZIP", exception);
        }
    }

    private Map<String, byte[]> allCsvReports(String sellerEmail, LocalDate startDate, LocalDate endDate) {
        Map<String, byte[]> reports = new LinkedHashMap<>();
        reports.put("Sales", salesCsv(sellerEmail, startDate, endDate));
        reports.put("Purchase", purchaseCsv(sellerEmail, startDate, endDate));
        reports.put("Stock", stockCsv(sellerEmail, startDate, endDate));
        reports.put("Expenses", expenseCsv(sellerEmail, startDate, endDate));
        reports.put("Receivables", receivablesCsv(sellerEmail, startDate, endDate));
        reports.put("Payables", payablesCsv(sellerEmail, startDate, endDate));
        reports.put("Payments", paymentsCsv(sellerEmail, startDate, endDate));
        reports.put("CreditDebitNotes", creditDebitNoteCsv(sellerEmail, startDate, endDate));
        reports.put("StockTransfers", stockTransferCsv(sellerEmail, startDate, endDate));
        reports.put("GstSummary", gstSummaryCsv(sellerEmail, startDate, endDate));
        reports.put("ProfitLoss", profitLossCsv(sellerEmail, startDate, endDate));
        return reports;
    }

    private User seller(String email) {
        User seller = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Seller not found"));
        if (!seller.isEnabled() || seller.getRole() == null || !"SELLER".equals(seller.getRole().name())) {
            throw new RuntimeException("Only active sellers can access CA reports");
        }
        return seller;
    }

    private Period period(LocalDate startDate, LocalDate endDate) {
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        LocalDate start = startDate == null ? end.withDayOfMonth(1) : startDate;
        if (end.isBefore(start)) throw new RuntimeException("End date cannot be before start date");
        return new Period(start, end, start.atStartOfDay(), end.plusDays(1).atStartOfDay());
    }

    private boolean between(LocalDate value, Period period) {
        return value != null && !value.isBefore(period.start()) && !value.isAfter(period.end());
    }

    private boolean between(LocalDateTime value, Period period) {
        return value != null && !value.isBefore(period.from()) && value.isBefore(period.until());
    }

    private String distributorName(Purchase purchase) {
        return purchase.getDistributor() != null && purchase.getDistributor().getDistributor() != null
                ? purchase.getDistributor().getDistributor().getBusinessName()
                : "Distributor";
    }

    private String serialDisplay(PurchaseItemSerial serial) {
        if (serial.getImei1() != null && !serial.getImei1().isBlank()) return serial.getImei1();
        if (serial.getSerialNumber() != null && !serial.getSerialNumber().isBlank()) return serial.getSerialNumber();
        if (serial.getImei2() != null && !serial.getImei2().isBlank()) return serial.getImei2();
        return String.valueOf(serial.getId());
    }

    private BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String text(LocalDate value) {
        return value == null ? "" : value.toString();
    }

    private String text(LocalDateTime value) {
        return value == null ? "" : value.toString();
    }

    private StringBuilder newCsv(String header) {
        return new StringBuilder(header);
    }

    private byte[] bytes(StringBuilder csv) {
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String cell(String value) {
        String text = value == null ? "" : value;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private String unquote(String value) {
        String text = value == null ? "" : value.trim();
        if (text.startsWith("\"") && text.endsWith("\"") && text.length() >= 2) {
            return text.substring(1, text.length() - 1).replace("\"\"", "\"");
        }
        return text;
    }

    private String xml(String value) {
        String text = value == null ? "" : value;
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private record Period(LocalDate start, LocalDate end, LocalDateTime from, LocalDateTime until) {}

    private static class MovementTotals {
        int netQuantity;
        int purchases;
        int sales;
        int transfers;
        int adjustments;

        void add(StockMovement movement) {
            int qty = movement.getQuantity() == null ? 0 : movement.getQuantity();
            netQuantity += qty;
            String type = movement.getMovementType() == null ? "" : movement.getMovementType();
            if (type.contains("PURCHASE")) purchases += Math.max(0, qty);
            else if (type.contains("SALE")) sales += Math.abs(Math.min(0, qty));
            else if (type.contains("TRANSFER")) transfers += Math.abs(Math.min(0, qty));
            else adjustments += qty;
        }
    }
}
