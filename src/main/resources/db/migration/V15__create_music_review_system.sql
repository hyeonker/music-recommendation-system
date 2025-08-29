-- 음악 리뷰 및 평점 시스템 스키마
-- V15: 소셜 기능 강화 - 리뷰 시스템

-- 음악/아티스트 정보 테이블 (확장 가능한 구조)
CREATE TABLE IF NOT EXISTS music_items (
    id BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(255) NOT NULL, -- Spotify ID 등 외부 서비스 ID
    item_type VARCHAR(50) NOT NULL CHECK (item_type IN ('TRACK', 'ALBUM', 'ARTIST', 'PLAYLIST')),
    name VARCHAR(500) NOT NULL,
    artist_name VARCHAR(500),
    album_name VARCHAR(500),
    genre VARCHAR(100),
    release_date DATE,
    duration_ms INTEGER,
    image_url TEXT,
    external_url TEXT,
    metadata JSONB, -- 추가 메타데이터 저장
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    UNIQUE(external_id, item_type)
);

-- 음악 리뷰 테이블
CREATE TABLE IF NOT EXISTS music_reviews (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    music_item_id BIGINT NOT NULL REFERENCES music_items(id) ON DELETE CASCADE,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    review_text TEXT,
    tags TEXT[], -- 리뷰 태그 (예: 'chill', 'energetic', 'emotional')
    is_spoiler BOOLEAN DEFAULT FALSE,
    is_public BOOLEAN DEFAULT TRUE,
    helpful_count INTEGER DEFAULT 0,
    report_count INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    -- 한 사용자당 같은 음악 아이템에 대해 하나의 리뷰만
    UNIQUE(user_id, music_item_id)
);

-- 리뷰 도움됨/신고 테이블
CREATE TABLE IF NOT EXISTS review_interactions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    review_id BIGINT NOT NULL REFERENCES music_reviews(id) ON DELETE CASCADE,
    interaction_type VARCHAR(20) NOT NULL CHECK (interaction_type IN ('HELPFUL', 'NOT_HELPFUL', 'REPORT')),
    reason VARCHAR(100), -- 신고 이유
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    -- 한 사용자당 같은 리뷰에 대해 하나의 상호작용만
    UNIQUE(user_id, review_id, interaction_type)
);

-- 음악 아이템 평점 통계 테이블 (성능 최적화용)
CREATE TABLE IF NOT EXISTS music_item_stats (
    id BIGSERIAL PRIMARY KEY,
    music_item_id BIGINT NOT NULL REFERENCES music_items(id) ON DELETE CASCADE UNIQUE,
    average_rating DECIMAL(3,2) DEFAULT 0.00,
    total_reviews INTEGER DEFAULT 0,
    rating_distribution JSONB DEFAULT '{"1":0,"2":0,"3":0,"4":0,"5":0}',
    last_updated TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 사용자 배지/업적 시스템
CREATE TABLE IF NOT EXISTS user_badges (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    badge_type VARCHAR(50) NOT NULL,
    badge_name VARCHAR(100) NOT NULL,
    description TEXT,
    icon_url VARCHAR(500),
    earned_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    metadata JSONB, -- 배지 관련 추가 정보
    
    UNIQUE(user_id, badge_type)
);

-- 인덱스 생성 (성능 최적화)
CREATE INDEX idx_music_items_external_id ON music_items(external_id);
CREATE INDEX idx_music_items_type_name ON music_items(item_type, name);
CREATE INDEX idx_music_items_genre ON music_items(genre) WHERE genre IS NOT NULL;

CREATE INDEX idx_reviews_user_id ON music_reviews(user_id);
CREATE INDEX idx_reviews_item_id ON music_reviews(music_item_id);
CREATE INDEX idx_reviews_rating ON music_reviews(rating);
CREATE INDEX idx_reviews_created_at ON music_reviews(created_at DESC);
CREATE INDEX idx_reviews_public ON music_reviews(is_public) WHERE is_public = TRUE;

CREATE INDEX idx_review_interactions_user_id ON review_interactions(user_id);
CREATE INDEX idx_review_interactions_review_id ON review_interactions(review_id);

CREATE INDEX idx_user_badges_user_id ON user_badges(user_id);
CREATE INDEX idx_user_badges_type ON user_badges(badge_type);

-- 트리거: 리뷰 통계 자동 업데이트
CREATE OR REPLACE FUNCTION update_music_item_stats()
RETURNS TRIGGER AS $$
BEGIN
    -- 통계 테이블 업데이트
    INSERT INTO music_item_stats (music_item_id, average_rating, total_reviews, rating_distribution, last_updated)
    SELECT 
        NEW.music_item_id,
        AVG(rating)::DECIMAL(3,2),
        COUNT(*),
        jsonb_build_object(
            '1', COUNT(*) FILTER (WHERE rating = 1),
            '2', COUNT(*) FILTER (WHERE rating = 2),
            '3', COUNT(*) FILTER (WHERE rating = 3),
            '4', COUNT(*) FILTER (WHERE rating = 4),
            '5', COUNT(*) FILTER (WHERE rating = 5)
        ),
        NOW()
    FROM music_reviews 
    WHERE music_item_id = NEW.music_item_id AND is_public = TRUE
    ON CONFLICT (music_item_id) 
    DO UPDATE SET
        average_rating = EXCLUDED.average_rating,
        total_reviews = EXCLUDED.total_reviews,
        rating_distribution = EXCLUDED.rating_distribution,
        last_updated = EXCLUDED.last_updated;
        
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 트리거 연결
DROP TRIGGER IF EXISTS trigger_update_stats ON music_reviews;
CREATE TRIGGER trigger_update_stats
    AFTER INSERT OR UPDATE OR DELETE ON music_reviews
    FOR EACH ROW
    EXECUTE FUNCTION update_music_item_stats();

-- 보안: RLS (Row Level Security) 설정
ALTER TABLE music_reviews ENABLE ROW LEVEL SECURITY;

-- 사용자는 자신의 리뷰만 수정/삭제 가능
CREATE POLICY review_owner_policy ON music_reviews
    FOR ALL TO PUBLIC
    USING (user_id = current_setting('app.current_user_id')::bigint)
    WITH CHECK (user_id = current_setting('app.current_user_id')::bigint);

-- 공개 리뷰는 모든 사용자가 읽기 가능
CREATE POLICY review_public_read_policy ON music_reviews
    FOR SELECT TO PUBLIC
    USING (is_public = TRUE);