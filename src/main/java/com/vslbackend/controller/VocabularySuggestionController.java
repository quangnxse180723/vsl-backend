package com.vslbackend.controller;

import com.vslbackend.dto.request.user.CreateVocabularySuggestionRequest;
import com.vslbackend.dto.response.ApiResponse;
import com.vslbackend.dto.response.VocabularySuggestionResponse;
import com.vslbackend.security.CustomUserDetails;
import com.vslbackend.service.inter.VocabularySuggestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Nguoi dung gui de xuat tu vung moi va xem lai cac de xuat cua chinh minh.
 * Yeu cau dang nhap (khong nam trong PUBLIC_ENDPOINTS).
 */
@RestController
@RequestMapping("/api/vocabulary-suggestions")
@RequiredArgsConstructor
public class VocabularySuggestionController {

    private final VocabularySuggestionService suggestionService;

    @PostMapping
    public ResponseEntity<ApiResponse<VocabularySuggestionResponse>> submit(
            @Valid @RequestBody CreateVocabularySuggestionRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        VocabularySuggestionResponse created =
                suggestionService.submit(principal.getUser().getUserId(), request);
        return ResponseEntity.ok(ApiResponse.of("Da gui de xuat tu vung, cam on ban!", created));
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<VocabularySuggestionResponse>>> mine(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.of("De xuat cua toi",
                suggestionService.getMine(principal.getUser().getUserId())));
    }
}
