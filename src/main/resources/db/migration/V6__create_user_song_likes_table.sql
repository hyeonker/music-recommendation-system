-- 사용자 음악 좋아요 테이블 생성
-- 사용자가 곡에 누른 좋아요 정보를 저장

CREATE TABLE user_song_likes (
                                 id          BIGSERIAL PRIMARY KEY,
                                 user_id     BIGINT NOT NULL,
                                 song_id     BIGINT NOT NULL,
                                 liked_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- 외래키 제약조건
                                 CONSTRAINT fk_user_song_likes_user_id
                                     FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                                 CONSTRAINT fk_user_song_likes_song_id
                                     FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE CASCADE
);

-- 성능 최적화를 위한 인덱스 생성
CREATE INDEX idx_user_song_likes_user_id ON user_song_likes(user_id);
CREATE INDEX idx_user_song_likes_song_id ON user_song_likes(song_id);
CREATE INDEX idx_user_song_likes_liked_at ON user_song_likes(liked_at);

-- 중복 좋아요 방지를 위한 유니크 제약조건
CREATE UNIQUE INDEX uk_user_song_likes_user_song ON user_song_likes(user_id, song_id);

-- 테이블 설명 주석
COMMENT ON TABLE user_song_likes IS '사용자 음악 좋아요 정보 테이블';
COMMENT ON COLUMN user_song_likes.user_id IS '좋아요를 누른 사용자 ID';
COMMENT ON COLUMN user_song_likes.song_id IS '좋아요를 받은 곡 ID';
COMMENT ON COLUMN user_song_likes.liked_at IS '좋아요를 누른 시간';