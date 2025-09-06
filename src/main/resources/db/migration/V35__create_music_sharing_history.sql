-- 음악 공유 히스토리 테이블 생성
CREATE TABLE music_sharing_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    shared_by_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    shared_by_name VARCHAR(100) NOT NULL,
    track_name VARCHAR(500) NOT NULL,
    artist_name VARCHAR(300) NOT NULL,
    spotify_url TEXT,
    album_name VARCHAR(500),
    album_image_url TEXT,
    preview_url TEXT,
    duration_ms INTEGER,
    popularity INTEGER,
    genres TEXT[], -- PostgreSQL array for storing genres
    shared_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    chat_room_id VARCHAR(255), -- 채팅방 ID (참조용)
    matching_session_id VARCHAR(255), -- 매칭 세션 ID (참조용)
    is_liked BOOLEAN DEFAULT FALSE, -- 사용자가 좋아요 표시했는지
    liked_at TIMESTAMP WITH TIME ZONE,
    notes TEXT, -- 사용자 메모
    
    -- 인덱스
    CONSTRAINT music_sharing_history_user_id_idx 
        CHECK (user_id IS NOT NULL AND shared_by_user_id IS NOT NULL),
    
    UNIQUE(user_id, shared_by_user_id, track_name, artist_name, shared_at)
);

-- 인덱스 생성
CREATE INDEX idx_music_sharing_history_user_id ON music_sharing_history(user_id);
CREATE INDEX idx_music_sharing_history_shared_by ON music_sharing_history(shared_by_user_id);
CREATE INDEX idx_music_sharing_history_shared_at ON music_sharing_history(shared_at DESC);
CREATE INDEX idx_music_sharing_history_is_liked ON music_sharing_history(user_id, is_liked) WHERE is_liked = true;
CREATE INDEX idx_music_sharing_history_track_search ON music_sharing_history(track_name, artist_name);

-- 테이블 코멘트
COMMENT ON TABLE music_sharing_history IS '사용자들 간의 음악 공유 히스토리를 저장하는 테이블';
COMMENT ON COLUMN music_sharing_history.user_id IS '음악을 받은 사용자 ID';
COMMENT ON COLUMN music_sharing_history.shared_by_user_id IS '음악을 공유한 사용자 ID';
COMMENT ON COLUMN music_sharing_history.shared_by_name IS '음악을 공유한 사용자 이름 (당시 이름 보존)';
COMMENT ON COLUMN music_sharing_history.track_name IS '곡 제목';
COMMENT ON COLUMN music_sharing_history.artist_name IS '아티스트 이름';
COMMENT ON COLUMN music_sharing_history.spotify_url IS 'Spotify 곡 URL';
COMMENT ON COLUMN music_sharing_history.is_liked IS '사용자가 이 공유 음악을 좋아요 표시했는지';
COMMENT ON COLUMN music_sharing_history.notes IS '사용자가 추가한 메모';