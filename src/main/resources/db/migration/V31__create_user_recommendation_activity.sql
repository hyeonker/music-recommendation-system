-- 사용자 추천 활동 추적 테이블
CREATE TABLE user_recommendation_activity (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    activity_date DATE NOT NULL,
    refresh_count INT NOT NULL DEFAULT 0,
    last_refresh_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_recommendation_activity_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_date UNIQUE (user_id, activity_date)
);

-- 인덱스 생성 (성능 최적화)
CREATE INDEX idx_user_recommendation_activity_user_date ON user_recommendation_activity(user_id, activity_date);
CREATE INDEX idx_user_recommendation_activity_date ON user_recommendation_activity(activity_date);

-- 자동 업데이트 트리거 (updated_at 필드)
CREATE OR REPLACE FUNCTION update_recommendation_activity_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_recommendation_activity_update
    BEFORE UPDATE ON user_recommendation_activity
    FOR EACH ROW
    EXECUTE FUNCTION update_recommendation_activity_timestamp();

-- 초기 데이터 정리 작업을 위한 인덱스
CREATE INDEX idx_user_recommendation_activity_created_at ON user_recommendation_activity(created_at);