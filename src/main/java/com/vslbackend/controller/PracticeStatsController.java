package com.vslbackend.controller;

import com.vslbackend.dto.response.AchievementResponse;
import com.vslbackend.dto.response.ApiResponse;
import com.vslbackend.dto.response.PracticeStatsResponse;
import com.vslbackend.security.CustomUserDetails;
import com.vslbackend.service.inter.AchievementService;
import com.vslbackend.service.inter.PracticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/practice")
@RequiredArgsConstructor
public class PracticeStatsController {

    private final PracticeService practiceService;
    private final AchievementService achievementService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<PracticeStatsResponse>> getStats(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.of(
                "Lay thong ke thanh cong",
                practiceService.getStats(principal.getUser().getUserId())));
    }

    @GetMapping("/achievements")
    public ResponseEntity<ApiResponse<List<AchievementResponse>>> getAchievements(
            @AuthenticationPrincipal CustomUserDetails principal) {
        List<AchievementResponse> achievements = achievementService.getAll(principal.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.of("Lay thanh tuu thanh cong", achievements));
    }
}
