package com.vslbackend.dto.response;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

@Data
@Builder
public class BlogSearchResponse {
    private Page<BlogResponse> blogs;
    private Page<UserSummaryResponse> users;
}
