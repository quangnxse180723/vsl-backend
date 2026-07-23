package com.vslbackend.controller;

import com.vslbackend.dto.response.ApiResponse;
import com.vslbackend.service.inter.PublicStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicStatsController {

    private final PublicStatsService publicStatsService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLandingStats() {
        return ResponseEntity.ok(ApiResponse.of("Success", publicStatsService.getLandingStats()));
    }
}
