-- V7__Create_music_metadata_table.sql
-- 음악 메타데이터 테이블 생성
CREATE TABLE music_metadata (
                                id BIGSERIAL PRIMARY KEY,
                                song_id BIGINT UNIQUE,
                                spotify_id VARCHAR(255),

    -- 음악 특성 데이터
                                acousticness FLOAT,
                                danceability FLOAT,
                                energy FLOAT,
                                instrumentalness FLOAT,
                                liveness FLOAT,
                                loudness FLOAT,
                                speechiness FLOAT,
                                valence FLOAT,
                                tempo FLOAT,

    -- 음악 정보
                                key_signature INTEGER,
                                mode INTEGER,
                                time_signature INTEGER,
                                duration_ms INTEGER,

    -- 장르 및 메타데이터
                                genres VARCHAR(1000),
                                popularity INTEGER,
                                preview_url VARCHAR(500),
                                external_urls VARCHAR(500),

    -- 메타데이터
                                fetched_at TIMESTAMP,
                                updated_at TIMESTAMP,

                                FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE CASCADE
);

-- 인덱스 생성
CREATE INDEX idx_music_metadata_song_id ON music_metadata(song_id);
CREATE INDEX idx_music_metadata_spotify_id ON music_metadata(spotify_id);
CREATE INDEX idx_music_metadata_energy ON music_metadata(energy);
CREATE INDEX idx_music_metadata_danceability ON music_metadata(danceability);
CREATE INDEX idx_music_metadata_valence ON music_metadata(valence);
CREATE INDEX idx_music_metadata_popularity ON music_metadata(popularity);