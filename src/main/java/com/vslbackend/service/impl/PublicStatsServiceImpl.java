package com.vslbackend.service.impl;

import com.vslbackend.repository.UserRepository;
import com.vslbackend.repository.VocabularyRepository;
import com.vslbackend.service.inter.PublicStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicStatsServiceImpl implements PublicStatsService {

    private final UserRepository userRepository;
    private final VocabularyRepository vocabularyRepository;

    @Override
    public Map<String, Object> getLandingStats() {
        return Map.of(
                "totalUsers", userRepository.count(),
                "totalVocabs", vocabularyRepository.count(),
                "satisfactionRate", 98
        );
    }
}
