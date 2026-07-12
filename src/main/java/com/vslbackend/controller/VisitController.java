package com.vslbackend.controller;

import com.vslbackend.security.CustomUserDetails;
import com.vslbackend.service.inter.VisitService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ghi nhan luot truy cap he thong (ca khach lan hoc vien).
 * Cong khai: frontend gui 1 beacon moi phien khi mo app.
 */
@RestController
@RequestMapping("/api/visits")
@RequiredArgsConstructor
public class VisitController {

    private final VisitService visitService;

    @PostMapping("/track")
    public ResponseEntity<Void> track(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestHeader(value = "X-Visit-Session", required = false) String sessionId,
            HttpServletRequest request) {

        Long userId = principal != null ? principal.getUser().getUserId() : null;
        // Ghi nen (async) roi tra 202 ngay -> khong lam cham page load.
        // sessionId cho phep upsert: khach dang nhap thi nang cap dung luot cua phien.
        visitService.recordVisit(userId, sessionId, resolveClientIp(request), request.getHeader("User-Agent"));
        return ResponseEntity.accepted().build();
    }

    /** Lay IP that khi dung sau proxy/load balancer (X-Forwarded-For). */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
