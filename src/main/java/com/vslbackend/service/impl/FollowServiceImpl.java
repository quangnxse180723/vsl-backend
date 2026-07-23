package com.vslbackend.service.impl;

import com.vslbackend.dto.response.FollowStatusResponse;
import com.vslbackend.dto.response.UserSummaryResponse;
import com.vslbackend.entity.User;
import com.vslbackend.entity.UserFollow;
import com.vslbackend.exception.AppException;
import com.vslbackend.exception.ErrorCode;
import com.vslbackend.repository.UserFollowRepository;
import com.vslbackend.repository.UserRepository;
import com.vslbackend.service.inter.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FollowServiceImpl implements FollowService {

    private final UserRepository userRepository;
    private final UserFollowRepository userFollowRepository;

    @Override
    public FollowStatusResponse follow(Long targetUserId, Long currentUserId) {
        if (targetUserId.equals(currentUserId)) {
            throw new AppException(ErrorCode.CANNOT_FOLLOW_SELF);
        }

        User follower = getUserOrThrow(currentUserId);
        User following = getUserOrThrow(targetUserId);

        if (!userFollowRepository.existsByFollower_UserIdAndFollowing_UserId(currentUserId, targetUserId)) {
            userFollowRepository.save(UserFollow.builder()
                    .follower(follower)
                    .following(following)
                    .build());
        }

        return buildStatus(targetUserId, currentUserId);
    }

    @Override
    public FollowStatusResponse unfollow(Long targetUserId, Long currentUserId) {
        userFollowRepository.findByFollower_UserIdAndFollowing_UserId(currentUserId, targetUserId)
                .ifPresent(userFollowRepository::delete);
        return buildStatus(targetUserId, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public FollowStatusResponse getStatus(Long targetUserId, Long currentUserId) {
        getUserOrThrow(targetUserId);
        return buildStatus(targetUserId, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> getFollowers(Long userId, Long currentUserId, Pageable pageable) {
        getUserOrThrow(userId);
        return userFollowRepository.findFollowerUsers(userId, pageable)
                .map(user -> toSummary(user, currentUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> getFollowing(Long userId, Long currentUserId, Pageable pageable) {
        getUserOrThrow(userId);
        return userFollowRepository.findFollowingUsers(userId, pageable)
                .map(user -> toSummary(user, currentUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean areFriends(Long firstUserId, Long secondUserId) {
        if (firstUserId == null || secondUserId == null || firstUserId.equals(secondUserId)) {
            return false;
        }
        return userFollowRepository.existsByFollower_UserIdAndFollowing_UserId(firstUserId, secondUserId)
                && userFollowRepository.existsByFollower_UserIdAndFollowing_UserId(secondUserId, firstUserId);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private FollowStatusResponse buildStatus(Long targetUserId, Long currentUserId) {
        boolean followedByMe = currentUserId != null
                && userFollowRepository.existsByFollower_UserIdAndFollowing_UserId(currentUserId, targetUserId);
        boolean followsMe = currentUserId != null
                && userFollowRepository.existsByFollower_UserIdAndFollowing_UserId(targetUserId, currentUserId);

        return FollowStatusResponse.builder()
                .targetUserId(targetUserId)
                .followedByMe(followedByMe)
                .followsMe(followsMe)
                .friend(followedByMe && followsMe)
                .followerCount(userFollowRepository.countByFollowing_UserId(targetUserId))
                .followingCount(userFollowRepository.countByFollower_UserId(targetUserId))
                .build();
    }

    private UserSummaryResponse toSummary(User user, Long currentUserId) {
        boolean followedByMe = currentUserId != null
                && userFollowRepository.existsByFollower_UserIdAndFollowing_UserId(currentUserId, user.getUserId());
        boolean followsMe = currentUserId != null
                && userFollowRepository.existsByFollower_UserIdAndFollowing_UserId(user.getUserId(), currentUserId);

        return UserSummaryResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .followerCount(userFollowRepository.countByFollowing_UserId(user.getUserId()))
                .followingCount(userFollowRepository.countByFollower_UserId(user.getUserId()))
                .followedByMe(followedByMe)
                .followsMe(followsMe)
                .friend(followedByMe && followsMe)
                .build();
    }
}
