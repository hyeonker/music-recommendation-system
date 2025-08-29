-- V18: 리뷰 도움이 됨 추적 테이블 생성
CREATE TABLE review_helpful (
    id BIGSERIAL PRIMARY KEY,
    review_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    CONSTRAINT fk_review_helpful_review FOREIGN KEY (review_id) REFERENCES music_reviews(id) ON DELETE CASCADE,
    CONSTRAINT uk_review_helpful_user UNIQUE (review_id, user_id)
);

-- 인덱스 생성
CREATE INDEX idx_review_helpful_review_id ON review_helpful(review_id);
CREATE INDEX idx_review_helpful_user_id ON review_helpful(user_id);

-- 기존 리뷰들의 helpfulCount 초기화 (중복 방지를 위해)
UPDATE music_reviews SET helpful_count = 0;