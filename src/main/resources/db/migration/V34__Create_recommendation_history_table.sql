-- 추천 히스토리 테이블 생성
CREATE TABLE recommendation_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    track_id VARCHAR(100) NOT NULL,
    track_title VARCHAR(500) NOT NULL,
    track_artist VARCHAR(500) NOT NULL,
    track_genre VARCHAR(100),
    recommendation_type VARCHAR(100),
    recommendation_score INTEGER,
    spotify_id VARCHAR(100),
    session_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스 생성 (성능 최적화)
CREATE INDEX idx_recommendation_history_user_id ON recommendation_history(user_id);
CREATE INDEX idx_recommendation_history_user_created ON recommendation_history(user_id, created_at);
CREATE INDEX idx_recommendation_history_track_id ON recommendation_history(track_id);
CREATE INDEX idx_recommendation_history_session_id ON recommendation_history(session_id);
CREATE INDEX idx_recommendation_history_user_track_created ON recommendation_history(user_id, track_id, created_at);

-- 중복 추천 방지를 위한 복합 인덱스
CREATE INDEX idx_recommendation_history_duplicate_check ON recommendation_history(user_id, track_title, track_artist, created_at);

-- 통계 조회를 위한 인덱스
CREATE INDEX idx_recommendation_history_type_stats ON recommendation_history(user_id, recommendation_type);

COMMENT ON TABLE recommendation_history IS '사용자별 음악 추천 히스토리';
COMMENT ON COLUMN recommendation_history.user_id IS '사용자 ID';
COMMENT ON COLUMN recommendation_history.track_id IS 'Spotify 트랙 ID 또는 고유 트랙 ID';
COMMENT ON COLUMN recommendation_history.track_title IS '트랙 제목';
COMMENT ON COLUMN recommendation_history.track_artist IS '아티스트명';
COMMENT ON COLUMN recommendation_history.track_genre IS '장르';
COMMENT ON COLUMN recommendation_history.recommendation_type IS '추천 타입 (리뷰 기반, 선호 아티스트, 선호 장르, 트렌딩 등)';
COMMENT ON COLUMN recommendation_history.recommendation_score IS '추천 점수 (70-100)';
COMMENT ON COLUMN recommendation_history.spotify_id IS 'Spotify 트랙 ID';
COMMENT ON COLUMN recommendation_history.session_id IS '추천 세션 ID (같은 시점에 생성된 추천들 그룹화)';
COMMENT ON COLUMN recommendation_history.created_at IS '추천 생성 시간';