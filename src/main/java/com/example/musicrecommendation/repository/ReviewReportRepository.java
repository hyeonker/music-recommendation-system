package com.example.musicrecommendation.repository;

import com.example.musicrecommendation.domain.ReviewReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {
    
    /**
     * 특정 사용자가 특정 리뷰를 신고했는지 확인
     */
    boolean existsByReviewIdAndReporterUserId(Long reviewId, Long reporterUserId);
    
    /**
     * 특정 리뷰의 신고 건수 조회
     */
    long countByReviewId(Long reviewId);
    
    /**
     * 특정 리뷰의 모든 신고 조회
     */
    List<ReviewReport> findByReviewIdOrderByCreatedAtDesc(Long reviewId);
    
    /**
     * 처리되지 않은 신고들 조회 (관리자용)
     */
    @Query("SELECT r FROM ReviewReport r WHERE r.status = 'PENDING' ORDER BY r.createdAt DESC")
    Page<ReviewReport> findPendingReports(Pageable pageable);
    
    /**
     * 특정 사용자가 신고한 내역 조회
     */
    Page<ReviewReport> findByReporterUserIdOrderByCreatedAtDesc(Long reporterUserId, Pageable pageable);
    
    /**
     * 특정 상태의 신고들 조회
     */
    Page<ReviewReport> findByStatusOrderByCreatedAtDesc(ReviewReport.ReportStatus status, Pageable pageable);
}