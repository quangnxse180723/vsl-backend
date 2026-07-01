package com.vslbackend.service.inter;

import com.vslbackend.dto.response.AttemptResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AttemptService {

    Page<AttemptResponse> getMyAttempts(Long userId, int page, int size);

    List<AttemptResponse> getRecentAttempts(Long userId, int limit);
}
