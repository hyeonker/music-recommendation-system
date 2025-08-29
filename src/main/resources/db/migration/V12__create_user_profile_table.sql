-- V12__create_user_profile_table.sql

-- 1) 사용자 프로필(선호 아티스트/장르/페스티벌/취향)을 JSONB로 보관
CREATE TABLE IF NOT EXISTS user_profile (
                                            id                  BIGSERIAL PRIMARY KEY,
                                            user_id             BIGINT NOT NULL UNIQUE
                                            REFERENCES users(id) ON DELETE CASCADE,

    favorite_artists    JSONB NOT NULL DEFAULT '[]',  -- [{id,name,image,followers,...}]
    favorite_genres     JSONB NOT NULL DEFAULT '[]',  -- [{id,name}, ...]
    attended_festivals  JSONB NOT NULL DEFAULT '[]',  -- ["SUMMER SONIC 2024", ...]
    music_preferences   JSONB NOT NULL DEFAULT '{}',  -- {preferredEra, moodPreferences, ...}

    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

-- 2) 조회 패턴에 대비해 GIN 인덱스(선택, 나중에 느리면 추가해도 됨)
CREATE INDEX IF NOT EXISTS idx_user_profile_artists_gin
    ON user_profile USING GIN (favorite_artists);

CREATE INDEX IF NOT EXISTS idx_user_profile_genres_gin
    ON user_profile USING GIN (favorite_genres);
