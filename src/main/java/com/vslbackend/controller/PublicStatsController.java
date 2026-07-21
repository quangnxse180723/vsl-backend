package com.vslbackend.controller;

import com.vslbackend.dto.response.ApiResponse;
import com.vslbackend.repository.UserRepository;
import com.vslbackend.repository.VocabularyRepository;
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
    
    private final UserRepository userRepository;
    private final VocabularyRepository vocabularyRepository;
    
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLandingStats() {
        // Lay thong so that tu database
        long totalUsers = userRepository.count();
        long totalVocabs = vocabularyRepository.count();
        
        // Ty le hai long hien tai chua co tinh nang danh gia nen de mac dinh 98%
        return ResponseEntity.ok(ApiResponse.of("Success", Map.of(
            "totalUsers", totalUsers,
            "totalVocabs", totalVocabs,
            "satisfactionRate", 98
        )));
    }
}
