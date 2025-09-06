package com.example.musicrecommendation.repository;

import com.example.musicrecommendation.domain.UserRecommendationActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 사용자 추천 활동 데이터 액세스 레포지토리
 */
@Repository
public interface UserRecommendationActivityRepository extends JpaRepository<UserRecommendationActivity, Long> {
    
    /**
     * 특정 사용자의 특정 날짜 활동 조회
     */
    Optional<UserRecommendationActivity> findByUserIdAndActivityDate(Long userId, LocalDate activityDate);
    
    /**
     * 오래된 활동 기록 정리 (성능 및 스토리지 최적화)
     * 기본적으로 30일 이전 데이터를 삭제
     */
    @Modifying
    @Query("DELETE FROM UserRecommendationActivity u WHERE u.activityDate < :cutoffDate")
    int deleteOldActivities(@Param("cutoffDate") LocalDate cutoffDate);
    
    /**
     * 특정 날짜 범위의 총 새로고침 횟수 조회 (통계용)
     */
    @Query("SELECT COALESCE(SUM(u.refreshCount), 0) FROM UserRecommendationActivity u " +
           "WHERE u.activityDate BETWEEN :startDate AND :endDate")
    Long getTotalRefreshCountBetween(@Param("startDate") LocalDate startDate, 
                                   @Param("endDate") LocalDate endDate);
    
    /**
     * 특정 사용자의 총 새로고침 횟수 조회 (통계용)
     */
    @Query("SELECT COALESCE(SUM(u.refreshCount), 0) FROM UserRecommendationActivity u WHERE u.userId = :userId")
    Long getTotalRefreshCountByUser(@Param("userId") Long userId);
    
    /**
     * 활성 사용자 수 조회 (특정 날짜)
     */
    @Query("SELECT COUNT(DISTINCT u.userId) FROM UserRecommendationActivity u WHERE u.activityDate = :date")
    Long getActiveUsersCount(@Param("date") LocalDate date);
}