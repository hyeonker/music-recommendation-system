package com.example.musicrecommendation.repository;

import com.example.musicrecommendation.domain.ChatReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 채팅 신고 리포지토리
 */
@Repository
public interface ChatReportRepository extends JpaRepository<ChatReport, Long> {
    
    /**
     * 특정 상태의 신고 목록 조회 (페이징)
     */
    Page<ChatReport> findByStatusOrderByCreatedAtDesc(ChatReport.ReportStatus status, Pageable pageable);
    
    /**
     * 모든 신고 목록 조회 (최신순, 페이징)
     */
    Page<ChatReport> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    /**
     * 신고자별 신고 목록 조회
     */
    List<ChatReport> findByReporterIdOrderByCreatedAtDesc(Long reporterId);
    
    /**
     * 피신고자별 신고 목록 조회
     */
    List<ChatReport> findByReportedUserIdOrderByCreatedAtDesc(Long reportedUserId);
    
    /**
     * 특정 채팅방의 신고 목록 조회
     */
    List<ChatReport> findByChatRoomIdOrderByCreatedAtDesc(String chatRoomId);
    
    /**
     * 중복 신고 방지를 위한 24시간 내 동일 신고 확인
     */
    @Query("SELECT COUNT(r) > 0 FROM ChatReport r WHERE r.reporterId = :reporterId " +
           "AND r.reportedUserId = :reportedUserId AND r.chatRoomId = :chatRoomId " +
           "AND r.createdAt >= :since")
    boolean existsRecentReport(@Param("reporterId") Long reporterId, 
                              @Param("reportedUserId") Long reportedUserId,
                              @Param("chatRoomId") String chatRoomId,
                              @Param("since") LocalDateTime since);
    
    /**
     * 관리자별 처리한 신고 개수 조회
     */
    @Query("SELECT COUNT(r) FROM ChatReport r WHERE r.adminUserId = :adminUserId " +
           "AND r.status IN ('RESOLVED', 'DISMISSED')")
    long countResolvedByAdmin(@Param("adminUserId") Long adminUserId);
    
    /**
     * 상태별 신고 개수 조회
     */
    long countByStatus(ChatReport.ReportStatus status);
    
    /**
     * 특정 사용자에 대한 신고 개수 조회 (피신고자)
     */
    @Query("SELECT COUNT(r) FROM ChatReport r WHERE r.reportedUserId = :userId " +
           "AND r.status IN ('PENDING', 'UNDER_REVIEW')")
    long countActiveReportsForUser(@Param("userId") Long userId);
    
    /**
     * 최근 신고 통계 (지난 7일)
     */
    @Query("SELECT COUNT(r) FROM ChatReport r WHERE r.createdAt >= :since")
    long countReportsInLastWeek(@Param("since") LocalDateTime since);
    
    /**
     * 신고 유형별 통계
     */
    @Query("SELECT r.reportType, COUNT(r) FROM ChatReport r " +
           "WHERE r.createdAt >= :since GROUP BY r.reportType")
    List<Object[]> countByReportTypeInLastWeek(@Param("since") LocalDateTime since);
}