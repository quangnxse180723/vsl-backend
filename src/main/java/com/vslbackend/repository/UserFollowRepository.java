package com.vslbackend.repository;

import com.vslbackend.entity.User;
import com.vslbackend.entity.UserFollow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {
    boolean existsByFollower_UserIdAndFollowing_UserId(Long followerId, Long followingId);
    Optional<UserFollow> findByFollower_UserIdAndFollowing_UserId(Long followerId, Long followingId);
    long countByFollower_UserId(Long followerId);
    long countByFollowing_UserId(Long followingId);

    @Query("SELECT f.following FROM UserFollow f WHERE f.follower.userId = :userId ORDER BY f.createdAt DESC")
    Page<User> findFollowingUsers(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT f.follower FROM UserFollow f WHERE f.following.userId = :userId ORDER BY f.createdAt DESC")
    Page<User> findFollowerUsers(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT f.following.userId FROM UserFollow f WHERE f.follower.userId = :userId")
    List<Long> findFollowingIds(@Param("userId") Long userId);
}
