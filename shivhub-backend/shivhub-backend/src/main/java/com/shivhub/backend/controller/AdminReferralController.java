package com.shivhub.backend.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shivhub.backend.dto.AdminReferralResponse;
import com.shivhub.backend.service.AdminReferralService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/referrals")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminReferralController {
    private final AdminReferralService referrals;

    @GetMapping
    public AdminReferralResponse list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return referrals.list(search, status, from, to, page, size);
    }

    @GetMapping(value = "/export.csv", produces = "text/csv")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        List<AdminReferralResponse.Row> rows = referrals.export(search, status, from, to);
        StringBuilder csv = new StringBuilder("Referral ID,Referrer,Referrer Email,Referrer Mobile,Referred Customer,Referred Email,Referral Code,Date,Status,Reward Coupon,Reward Used,Qualifying Order,Order Revenue\n");
        for (AdminReferralResponse.Row row : rows) {
            csv.append(csv(row.id())).append(',').append(csv(row.referrer().name())).append(',').append(csv(row.referrer().email())).append(',')
                    .append(csv(row.referrer().mobile())).append(',').append(csv(row.referredCustomer().name())).append(',').append(csv(row.referredCustomer().email())).append(',')
                    .append(csv(row.referralCode())).append(',').append(csv(row.referralDate())).append(',').append(csv(row.status())).append(',')
                    .append(csv(row.rewardCoupon().code())).append(',').append(row.rewardCoupon().used()).append(',').append(csv(row.qualifyingOrderNumber())).append(',')
                    .append(csv(row.qualifyingOrderRevenue())).append('\n');
        }
        byte[] data = csv.toString().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("shivhub-referrals.csv").build().toString())
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).body(data);
    }

    private String csv(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        return '"' + text.replace("\"", "\"\"") + '"';
    }
}
