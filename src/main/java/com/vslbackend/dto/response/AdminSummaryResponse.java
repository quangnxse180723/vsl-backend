package com.vslbackend.dto.response;

import lombok.Builder;
import lombok.Data;

/** 4 chi so tong quan cho dashboard admin. */
@Data
@Builder
public class AdminSummaryResponse {
    private long totalStudents;      // tong hoc vien (role USER)
    private long totalVisits;        // tong luot truy cap (khach + hoc vien)
    private long totalVocabularies;  // tong tu vung
    private long totalBlogs;         // tong bai blog
}
