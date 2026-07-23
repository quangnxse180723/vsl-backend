package com.vslbackend.service.inter;

import com.vslbackend.dto.response.FollowStatusResponse;
import com.vslbackend.dto.response.UserSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FollowService {
    FollowStatusResponse follow(Long targetUserId, Long currentUserId);
    FollowStatusResponse unfollow(Long targetUserId, Long currentUserId);
    FollowStatusResponse getStatus(Long targetUserId, Long currentUserId);
    Page<UserSummaryResponse> getFollowers(Long userId, Long currentUserId, Pageable pageable);
    Page<UserSummaryResponse> getFollowing(Long userId, Long currentUserId, Pageable pageable);
    boolean areFriends(Long firstUserId, Long secondUserId);
}
