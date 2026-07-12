package com.vslbackend.service.inter;

import com.vslbackend.dto.response.AdminSummaryResponse;
import com.vslbackend.dto.response.VisitLogResponse;
import com.vslbackend.dto.response.VisitTimePoint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface VisitService {

    /**
     * Ghi 1 luot truy cap (chay nen). userId null = khach.
     * sessionId (neu co): upsert theo phien - da co dong cua phien nay thi gan user
     * vao dong do thay vi tao dong moi (tranh dem trung khi khach dang nhap).
     */
    void recordVisit(Long userId, String sessionId, String ipAddress, String userAgent);

    /** 4 chi so tong quan. */
    AdminSummaryResponse getSummary();

    /** Chuoi thoi gian luot truy cap theo do chi tiet (DAY/MONTH/YEAR), da lap day khoang trong. */
    List<VisitTimePoint> getVisitTimeSeries(LocalDate from, LocalDate to, String granularity);

    /** Bang log truy cap chi tiet, phan trang. */
    Page<VisitLogResponse> getVisitLogs(Pageable pageable);
}
