package com.vslbackend.repository;

import com.vslbackend.entity.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {

    @Query("SELECT ua.achievement.key FROM UserAchievement ua WHERE ua.user.userId = :userId")
    Set<String> findUnlockedKeysByUserId(@Param("userId") Long userId);

    boolean existsByUser_UserIdAndAchievement_Key(Long userId, String achievementKey);
}
