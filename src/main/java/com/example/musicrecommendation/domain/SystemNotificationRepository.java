package com.example.musicrecommendation.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface SystemNotificationRepository extends JpaRepository<SystemNotification, Long> {
    
    /**
     * 최근 생성된 알림 목록 조회 (최신순)
     */
    List<SystemNotification> findAllByOrderByCreatedAtDesc();
    
    /**
     * 특정 유형의 알림 목록 조회
     */
    List<SystemNotification> findByTypeOrderByCreatedAtDesc(SystemNotification.NotificationType type);
    
    /**
     * 특정 상태의 알림 목록 조회
     */
    List<SystemNotification> findByStatusOrderByCreatedAtDesc(SystemNotification.NotificationStatus status);
    
    /**
     * 발송 대기 중인 예약 알림 조회 (현재 시간 이전의 예약 시간)
     */
    @Query("SELECT sn FROM SystemNotification sn WHERE sn.status = 'SCHEDULED' AND sn.scheduledAt <= :currentTime")
    List<SystemNotification> findScheduledNotificationsToSend(@Param("currentTime") OffsetDateTime currentTime);
    
    /**
     * 특정 기간 내의 알림 개수 조회
     */
    @Query("SELECT COUNT(sn) FROM SystemNotification sn WHERE sn.createdAt >= :startTime AND sn.createdAt <= :endTime")
    long countByCreatedAtBetween(@Param("startTime") OffsetDateTime startTime, @Param("endTime") OffsetDateTime endTime);
    
    /**
     * 유형별 알림 개수 조회
     */
    long countByType(SystemNotification.NotificationType type);
    
    /**
     * 상태별 알림 개수 조회
     */
    long countByStatus(SystemNotification.NotificationStatus status);
    
    /**
     * 최근 30일간의 알림 목록 조회
     */
    @Query("SELECT sn FROM SystemNotification sn WHERE sn.createdAt >= :thirtyDaysAgo ORDER BY sn.createdAt DESC")
    List<SystemNotification> findRecentNotifications(@Param("thirtyDaysAgo") OffsetDateTime thirtyDaysAgo);
}