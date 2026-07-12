package com.vslbackend.controller;

import com.vslbackend.dto.response.AdminSummaryResponse;
import com.vslbackend.dto.response.VisitLogResponse;
import com.vslbackend.dto.response.VisitTimePoint;
import com.vslbackend.service.inter.VisitService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * So lieu tong quan + phan tich luot truy cap cho dashboard admin.
 */
@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyticsController {

    private final VisitService visitService;

    /** 4 chi so: tong hoc vien, tong luot truy cap, tong tu vung, tong blog. */
    @GetMapping("/summary")
    public ResponseEntity<AdminSummaryResponse> getSummary() {
        return ResponseEntity.ok(visitService.getSummary());
    }

    /** Bieu do luot truy cap theo thoi gian, co bo loc khoang & do chi tiet. */
    @GetMapping("/visits/timeseries")
    public ResponseEntity<List<VisitTimePoint>> getTimeSeries(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAY") String granularity) {
        return ResponseEntity.ok(visitService.getVisitTimeSeries(from, to, granularity));
    }

    /** Bang log truy cap chi tiet, phan trang (mac dinh 10/trang). */
    @GetMapping("/visits")
    public ResponseEntity<Page<VisitLogResponse>> getVisitLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(visitService.getVisitLogs(PageRequest.of(page, size)));
    }
}
