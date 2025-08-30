-- 리뷰 신고 테이블 생성
CREATE TABLE review_reports (
    id BIGSERIAL PRIMARY KEY,
    review_id BIGINT NOT NULL,
    reporter_user_id BIGINT NOT NULL,
    reason VARCHAR(50) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMPTZ,
    resolved_by_user_id BIGINT,
    
    CONSTRAINT fk_review_reports_review_id 
        FOREIGN KEY (review_id) REFERENCES music_reviews(id) ON DELETE CASCADE,
    CONSTRAINT fk_review_reports_reporter_user_id 
        FOREIGN KEY (reporter_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_review_reports_resolved_by_user_id 
        FOREIGN KEY (resolved_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    
    -- 같은 사용자가 같은 리뷰를 중복 신고할 수 없도록 제약
    CONSTRAINT uk_review_reports_review_reporter 
        UNIQUE (review_id, reporter_user_id)
);

-- 인덱스 추가
CREATE INDEX idx_review_reports_review_id ON review_reports(review_id);
CREATE INDEX idx_review_reports_reporter_user_id ON review_reports(reporter_user_id);
CREATE INDEX idx_review_reports_status ON review_reports(status);
CREATE INDEX idx_review_reports_created_at ON review_reports(created_at);

-- 리뷰 신고 사유 제약 조건
ALTER TABLE review_reports 
ADD CONSTRAINT chk_review_reports_reason 
CHECK (reason IN ('SPAM', 'INAPPROPRIATE', 'HATE_SPEECH', 'FAKE_REVIEW', 'COPYRIGHT', 'OTHER'));

-- 리뷰 신고 상태 제약 조건
ALTER TABLE review_reports 
ADD CONSTRAINT chk_review_reports_status 
CHECK (status IN ('PENDING', 'REVIEWING', 'RESOLVED', 'DISMISSED'));