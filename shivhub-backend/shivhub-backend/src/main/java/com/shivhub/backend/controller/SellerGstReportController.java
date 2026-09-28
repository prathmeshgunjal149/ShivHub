package com.shivhub.backend.controller;

import java.time.LocalDate;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shivhub.backend.dto.SellerGstReportResponse;
import com.shivhub.backend.service.SellerGstReportService;

@RestController
@RequestMapping("/api/seller/gst-reports")
@PreAuthorize("hasRole('SELLER')")
public class SellerGstReportController {
    private final SellerGstReportService service;
    public SellerGstReportController(SellerGstReportService service) { this.service = service; }

    @GetMapping
    public SellerGstReportResponse report(Authentication auth, @RequestParam(defaultValue="GSTR1") String type,
            @RequestParam(required=false) LocalDate from, @RequestParam(required=false) LocalDate to,
            @RequestParam(required=false) String q, @RequestParam(required=false) String party,
            @RequestParam(required=false) String gstin, @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="25") int size) { return service.report(auth.getName(), type, from, to, q, party, gstin, page, size); }

    @GetMapping("/download")
    public ResponseEntity<byte[]> download(Authentication auth, @RequestParam(defaultValue="GSTR1") String type,
            @RequestParam(required=false) LocalDate from, @RequestParam(required=false) LocalDate to,
            @RequestParam(required=false) String q, @RequestParam(required=false) String party,
            @RequestParam(required=false) String gstin, @RequestParam(defaultValue="CSV") String format) {
        String safe = format.toUpperCase(); byte[] body = service.download(auth.getName(),type,from,to,q,party,gstin,safe);
        MediaType media = "PDF".equals(safe) ? MediaType.APPLICATION_PDF : "XLSX".equals(safe) ? MediaType.parseMediaType("application/vnd.ms-excel") : MediaType.parseMediaType("text/csv");
        String extension = "PDF".equals(safe)?"pdf":"XLSX".equals(safe)?"xls":"csv";
        return ResponseEntity.ok().contentType(media).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=shivhub-"+type.toLowerCase()+"."+extension).body(body);
    }
}
