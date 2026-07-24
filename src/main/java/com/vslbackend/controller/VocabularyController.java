package com.vslbackend.controller;

import com.vslbackend.dto.response.ApiResponse;
import com.vslbackend.dto.response.VocabularyExistsResponse;
import com.vslbackend.dto.response.VocabularyResponse;
import com.vslbackend.dto.response.VocabularySynonymResponse;
import com.vslbackend.service.inter.VocabularyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/vocabularies")
@RequiredArgsConstructor
public class VocabularyController {

    private final VocabularyService vocabularyService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<VocabularyResponse>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.of("Danh sach tu vung", vocabularyService.getAll(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VocabularyResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.of("Chi tiet tu vung", vocabularyService.getById(id)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<VocabularyResponse>>> search(
            @RequestParam @NotBlank(message = "Keyword cannot be blank") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.of("Ket qua tim kiem", vocabularyService.search(keyword, page, size)));
    }

    /**
     * Kiem tra tu vung da ton tai chua (so khop chinh xac ca chuoi, khong phan biet hoa/thuong,
     * tren TOAN he thong). Dung cho form de xuat tu vung cua nguoi dung.
     */
    @GetMapping("/exists")
    public ResponseEntity<ApiResponse<VocabularyExistsResponse>> exists(
            @RequestParam @NotBlank(message = "Word cannot be blank") String word) {
        return ResponseEntity.ok(ApiResponse.of("Ket qua kiem tra", vocabularyService.checkWordExists(word)));
    }

    /**
     * Dung AI quet cac tu da co co nghia giong / dong nghia voi tu ung vien (goi y, khong chan submit).
     * FAIL-OPEN: khi AI tat / thieu key / loi -> tra ve aiChecked=false, synonyms rong.
     */
    @GetMapping("/ai-synonyms")
    public ResponseEntity<ApiResponse<VocabularySynonymResponse>> aiSynonyms(
            @RequestParam @NotBlank(message = "Word cannot be blank") String word) {
        return ResponseEntity.ok(ApiResponse.of("Ket qua quet AI", vocabularyService.findSynonyms(word)));
    }

    @GetMapping("/category/{id}")
    public ResponseEntity<ApiResponse<Page<VocabularyResponse>>> getByCategory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.of("Tu vung theo chu de", vocabularyService.getByCategory(id, page, size)));
    }
}