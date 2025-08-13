-- 사용자 매칭 테이블 생성 (샘플 데이터 제거)
CREATE TABLE user_matches (
                              id BIGSERIAL PRIMARY KEY,
                              user1_id BIGINT NOT NULL,
                              user2_id BIGINT NOT NULL,
                              similarity_score FLOAT,
                              match_status VARCHAR(20) DEFAULT 'PENDING',
                              match_reason VARCHAR(500),
                              common_liked_songs INTEGER DEFAULT 0,
                              created_at TIMESTAMP DEFAULT NOW(),
                              updated_at TIMESTAMP DEFAULT NOW(),

    -- 외래키 제약조건
                              CONSTRAINT fk_user_matches_user1 FOREIGN KEY (user1_id) REFERENCES users(id),
                              CONSTRAINT fk_user_matches_user2 FOREIGN KEY (user2_id) REFERENCES users(id),

    -- 중복 방지 (같은 사용자 쌍은 한번만)
                              CONSTRAINT unique_user_pair UNIQUE (user1_id, user2_id),

    -- 자기 자신과 매칭 방지
                              CONSTRAINT check_different_users CHECK (user1_id != user2_id)
    );

-- 인덱스 생성 (성능 최적화)
CREATE INDEX idx_user_matches_user1 ON user_matches(user1_id);
CREATE INDEX idx_user_matches_user2 ON user_matches(user2_id);
CREATE INDEX idx_user_matches_status ON user_matches(match_status);
CREATE INDEX idx_user_matches_similarity ON user_matches(similarity_score DESC);