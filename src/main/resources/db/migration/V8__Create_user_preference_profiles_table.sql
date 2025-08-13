-- V8__Create_user_preference_profiles_table.sql
-- 사용자 취향 프로필 테이블 생성
CREATE TABLE user_preference_profiles (
                                          id BIGSERIAL PRIMARY KEY,
                                          user_id BIGINT UNIQUE,

    -- 평균 음악 특성
                                          avg_acousticness FLOAT,
                                          avg_danceability FLOAT,
                                          avg_energy FLOAT,
                                          avg_instrumentalness FLOAT,
                                          avg_liveness FLOAT,
                                          avg_speechiness FLOAT,
                                          avg_valence FLOAT,
                                          avg_tempo FLOAT,

    -- 선호 장르 및 패턴
                                          preferred_genres VARCHAR(1000),
                                          preferred_artists VARCHAR(1000),
                                          listening_diversity_score FLOAT,

    -- 활동 패턴
                                          total_likes INTEGER DEFAULT 0,
                                          avg_likes_per_day FLOAT DEFAULT 0.0,
                                          most_active_hour INTEGER,
                                          most_active_day INTEGER,

    -- 추천 가중치
                                          popularity_weight FLOAT DEFAULT 0.3,
                                          discovery_weight FLOAT DEFAULT 0.3,
                                          similarity_weight FLOAT DEFAULT 0.4,

    -- 메타데이터
                                          last_calculated_at TIMESTAMP,
                                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                                          FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 인덱스 생성
CREATE INDEX idx_user_preference_profiles_user_id ON user_preference_profiles(user_id);
CREATE INDEX idx_user_preference_profiles_energy ON user_preference_profiles(avg_energy);
CREATE INDEX idx_user_preference_profiles_danceability ON user_preference_profiles(avg_danceability);
CREATE INDEX idx_user_preference_profiles_valence ON user_preference_profiles(avg_valence);