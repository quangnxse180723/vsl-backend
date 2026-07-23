package com.vslbackend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FollowStatusResponse {
    private Long targetUserId;
    private boolean followedByMe;
    private boolean followsMe;
    private boolean friend;
    private long followerCount;
    private long followingCount;
}
