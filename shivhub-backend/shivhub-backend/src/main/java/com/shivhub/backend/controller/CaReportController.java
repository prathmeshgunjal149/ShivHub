package com.shivhub.backend.controller;

import java.time.LocalDate;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shivhub.backend.service.CaExportService;

@RestController
@RequestMapping("/api/ca-reports")
public class CaReportController {

    private final CaExportService caExportService;

    public CaReportController(CaExportService caExportService) {
        this.caExportService = caExportService;
    }

    @GetMapping(value = "/sales.csv", produces = "text/csv")
    public ResponseEntity<byte[]> salesCsv(
            Authentication authentication,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        return csv(
                "shivhub-sales-report.csv",
                caExportService.salesCsv(email(authentication), startDate, endDate)
        );
    }

    @GetMapping(value = "/purchase.csv", produces = "text/csv")
    public ResponseEntity<byte[]> purchaseCsv(
            Authentication authentication,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        return csv("shivhub-purchase-report.csv", caExportService.purchaseCsv(email(authentication), startDate, endDate));
    }

    @GetMapping(value = "/stock.csv", produces = "text/csv")
    public ResponseEntity<byte[]> stockCsv(
            Authentication authentication,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        return csv("shivhub-stock-report.csv", caExportService.stockCsv(email(authentication), startDate, endDate));
    }

    @GetMapping(value = "/expenses.csv", produces = "text/csv")
    public ResponseEntity<byte[]> expensesCsv(
            Authentication authentication,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        return csv("shivhub-expense-report.csv", caExportService.expenseCsv(email(authentication), startDate, endDate));
    }

    @GetMapping(value = "/receivables.csv", produces = "text/csv")
    public ResponseEntity<byte[]> receivablesCsv(
            Authentication authentication,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        return csv("shivhub-receivables-report.csv", caExportService.receivablesCsv(email(authentication), startDate, endDate));
    }

    @GetMapping(value = "/payables.csv", produces = "text/csv")
    public ResponseEntity<byte[]> payablesCsv(
            Authentication authentication,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        return csv("shivhub-payables-report.csv", caExportService.payablesCsv(email(authentication), startDate, endDate));
   }

    @GetMapping(value = "/payments.csv", produces = "text/csv")
    public ResponseEntity<byte[]> paymentsCsv(
            Authentication authentication,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        return csv("shivhub-payment-collection-report.csv", caExportService.paymentsCsv(email(authentication), startDate, endDate));
    }

    @GetMapping(value = "/credit-debit-notes.csv", produces = "text/csv")
    public ResponseEntity<byte[]> creditDebitNotesCsv(
            Authentication authentication,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        return csv("shivhub-credit-debit-note-report.csv", caExportService.creditDebitNoteCsv(email(authentication), startDate, endDate));
    }

    @GetMapping(value = "/stock-transfers.csv", produces = "text/csv")
    public ResponseEntity<byte[]> stockTransfersCsv(
            Authentication authentication,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        return csv("shivhub-stock-transfer-report.csv", caExportService.stockTransferCsv(email(authentication), startDate, endDate));
    }

    @GetMapping(value = "/gst-summary.csv", produces = "text/csv")
    public ResponseEntity<byte[]> gstSummaryCsv(
            Authentication authentication,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        return csv("shivhub-gst-summary.csv", caExportService.gstSummaryCsv(email(authentication), startDate, endDate));
    }

    @GetMapping(value = "/profit-loss.csv", produces = "text/csv")
    public ResponseEntity<byte[]> profitLossCsv(
            Authentication authentication,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        return csv("shivhub-profit-loss-summary.csv", caExportService.profitLossCsv(email(authentication), startDate, endDate));
    }

    @GetMapping(value = "/workbook.xls")
    public ResponseEntity<byte[]> excelWorkbook(
            Authentication authentication,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        return file(
                "shivhub-ca-workbook.xls",
                MediaType.parseMediaType("application/vnd.ms-excel"),
                caExportService.excelWorkbook(email(authentication), startDate, endDate)
        );
    }

    @GetMapping(value = "/summary.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdfSummary(
            Authentication authentication,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        return file(
                "shivhub-ca-summary.pdf",
                MediaType.APPLICATION_PDF,
                caExportService.pdfSummary(email(authentication), startDate, endDate)
        );
    }

    @GetMapping(value = "/documents.zip")
    public ResponseEntity<byte[]> documentsZip(
            Authentication authentication,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        return file(
                "shivhub-ca-documents.zip",
                MediaType.parseMediaType("application/zip"),
                caExportService.documentsZip(email(authentication), startDate, endDate)
        );
    }

    private ResponseEntity<byte[]> csv(String filename, byte[] body) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .body(body);
    }

    private ResponseEntity<byte[]> file(String filename, MediaType mediaType, byte[] body) {
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .body(body);
    }

    private String email(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Authentication required");
        }
        return authentication.getName();
    }
}
