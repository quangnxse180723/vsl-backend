package com.vslbackend.controller;

import com.vslbackend.dto.response.ApiResponse;
import com.vslbackend.dto.response.AttemptResponse;
import com.vslbackend.security.CustomUserDetails;
import com.vslbackend.service.inter.AttemptService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping({"/api/attempts", "/attempts"})
@RequiredArgsConstructor
public class AttemptController {

    private final AttemptService attemptService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AttemptResponse>>> getMyAttempts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails principal) {

        Long userId = principal.getUser().getUserId();
        Page<AttemptResponse> attempts = attemptService.getMyAttempts(userId, page, size);
        return ResponseEntity.ok(ApiResponse.of("Lay lich su luyen tap thanh cong", attempts));
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<AttemptResponse>>> getRecentAttempts(
            @RequestParam(defaultValue = "5") int limit,
            @AuthenticationPrincipal CustomUserDetails principal) {

        Long userId = principal.getUser().getUserId();
        List<AttemptResponse> attempts = attemptService.getRecentAttempts(userId, limit);
        return ResponseEntity.ok(ApiResponse.of("Lay cac lan hoc gan day thanh cong", attempts));
    }
}
