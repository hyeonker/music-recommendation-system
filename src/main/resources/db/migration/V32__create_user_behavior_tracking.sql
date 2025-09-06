-- 사용자 행동 추적 테이블
CREATE TABLE user_behavior_events (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL, -- RECOMMENDATION_CLICK, LIKE, DISLIKE, SEARCH, PLAYLIST_ADD, REVIEW_WRITE, PROFILE_UPDATE, SESSION_START, SESSION_END, REFRESH_RECOMMENDATIONS
    item_type VARCHAR(20) NOT NULL,  -- SONG, ALBUM, ARTIST, PLAYLIST, GENRE
    item_id VARCHAR(255) NOT NULL,   -- Spotify ID 또는 내부 ID
    item_metadata JSONB,             -- 곡명, 아티스트, 장르 등 메타데이터
    duration_seconds INTEGER,        -- 세션 지속 시간 등
    session_id VARCHAR(100),         -- 세션 추적용
    device_type VARCHAR(50),         -- WEB, MOBILE, DESKTOP, TABLET
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_user_behavior_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 성능 최적화 인덱스
CREATE INDEX idx_user_behavior_user_event ON user_behavior_events(user_id, event_type);
CREATE INDEX idx_user_behavior_created_at ON user_behavior_events(created_at);
CREATE INDEX idx_user_behavior_item ON user_behavior_events(item_type, item_id);
CREATE INDEX idx_user_behavior_session ON user_behavior_events(session_id);

-- 사용자별 음악 취향 프로필 (집계 테이블)
CREATE TABLE user_music_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    
    -- 장르 선호도 (JSON: {"rock": 0.8, "pop": 0.6, ...})
    genre_preferences JSONB DEFAULT '{}',
    
    -- 아티스트 선호도 (JSON: {"artist_id": score, ...})
    artist_preferences JSONB DEFAULT '{}',
    
    -- 음악적 특성 선호도 (템포, 에너지, 댄스성 등)
    audio_features JSONB DEFAULT '{}',
    
    -- 활동 패턴 (시간대별, 요일별 등)
    listening_patterns JSONB DEFAULT '{}',
    
    -- 최근 업데이트 시간
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_user_preferences_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 아이템 간 유사도 매트릭스 (협업 필터링용)
CREATE TABLE item_similarity_matrix (
    id BIGSERIAL PRIMARY KEY,
    item1_id VARCHAR(255) NOT NULL,
    item2_id VARCHAR(255) NOT NULL,
    similarity_score DECIMAL(5,4) NOT NULL, -- 0.0000 ~ 1.0000
    similarity_type VARCHAR(50) NOT NULL,   -- COSINE, PEARSON, JACCARD
    computed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uq_item_similarity UNIQUE (item1_id, item2_id, similarity_type)
);

CREATE INDEX idx_item_similarity_item1 ON item_similarity_matrix(item1_id, similarity_score DESC);
CREATE INDEX idx_item_similarity_item2 ON item_similarity_matrix(item2_id, similarity_score DESC);

-- 사용자 간 유사도 매트릭스
CREATE TABLE user_similarity_matrix (
    id BIGSERIAL PRIMARY KEY,
    user1_id BIGINT NOT NULL,
    user2_id BIGINT NOT NULL,
    similarity_score DECIMAL(5,4) NOT NULL,
    similarity_type VARCHAR(50) NOT NULL,   -- COSINE, PEARSON, JACCARD
    computed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uq_user_similarity UNIQUE (user1_id, user2_id, similarity_type),
    CONSTRAINT fk_user_similarity_user1 FOREIGN KEY (user1_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_similarity_user2 FOREIGN KEY (user2_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_similarity_user1 ON user_similarity_matrix(user1_id, similarity_score DESC);
CREATE INDEX idx_user_similarity_user2 ON user_similarity_matrix(user2_id, similarity_score DESC);