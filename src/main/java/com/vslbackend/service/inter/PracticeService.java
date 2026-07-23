package com.vslbackend.service.inter;

import com.vslbackend.dto.response.PracticeStatsResponse;
import com.vslbackend.dto.response.UserProgressResponse;

import java.util.List;

public interface PracticeService {
    List<UserProgressResponse> getMyProgress(Long userId);
    PracticeStatsResponse getStats(Long userId);
}
