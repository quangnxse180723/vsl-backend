package com.vslbackend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShareResponse {
    private long shareCount;
    private String blogUrl;
    private String shareType;
    private Long recipientUserId;
    private String recipientName;
}
