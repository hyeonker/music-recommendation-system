package com.example.musicrecommendation.repository;

import com.example.musicrecommendation.domain.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {
    
    List<UserBadge> findByUserIdOrderByEarnedAtDesc(Long userId);
    
    Optional<UserBadge> findByUserIdAndBadgeType(Long userId, UserBadge.BadgeType badgeType);
    
    boolean existsByUserIdAndBadgeType(Long userId, UserBadge.BadgeType badgeType);
    
    @Query("SELECT COUNT(b) FROM UserBadge b WHERE b.userId = :userId")
    long countByUserId(@Param("userId") Long userId);
    
    @Query("SELECT b.badgeType, COUNT(b) FROM UserBadge b GROUP BY b.badgeType ORDER BY COUNT(b) DESC")
    List<Object[]> getBadgeTypeStatistics();
    
    @Query("SELECT b FROM UserBadge b WHERE b.badgeType IN :badgeTypes ORDER BY b.earnedAt DESC")
    List<UserBadge> findByBadgeTypes(@Param("badgeTypes") List<UserBadge.BadgeType> badgeTypes);
    
    @Query("SELECT DISTINCT b.userId FROM UserBadge b WHERE b.badgeType = :badgeType")
    List<Long> findUserIdsByBadgeType(@Param("badgeType") UserBadge.BadgeType badgeType);
}