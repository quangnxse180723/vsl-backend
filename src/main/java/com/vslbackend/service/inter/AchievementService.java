package com.vslbackend.service.inter;

import com.vslbackend.dto.response.AchievementResponse;

import java.util.List;

public interface AchievementService {
    List<AchievementResponse> getAll(Long userId);
    void checkAndUnlock(Long userId);
}
