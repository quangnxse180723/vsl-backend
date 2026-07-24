package com.vslbackend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserSummaryResponse {
    private Long userId;
    private String username;
    private String fullName;
    private String avatarUrl;
    private long followerCount;
    private long followingCount;
    private boolean followedByMe;
    private boolean followsMe;
    private boolean friend;
}
