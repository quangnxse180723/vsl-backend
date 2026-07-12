package com.vslbackend.dto.response;

import lombok.Builder;
import lombok.Data;

/** Mot diem tren bieu do luot truy cap theo thoi gian. */
@Data
@Builder
public class VisitTimePoint {
    private String label;  // vd "2026-07-10" (ngay), "2026-07" (thang), "2026" (nam)
    private long count;
}
