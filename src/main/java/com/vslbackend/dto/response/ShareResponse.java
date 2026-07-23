package com.vslbackend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShareResponse {
    private long shareCount;
    private String blogUrl;      // FE dùng để copy
    private String shareType;    // COPY_URL hoặc PROFILE
}
