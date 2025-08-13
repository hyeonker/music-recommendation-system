-- UserMatch 엔티티에 맞춰 누락된 컬럼들 추가
ALTER TABLE user_matches ADD COLUMN IF NOT EXISTS common_artists INTEGER DEFAULT 0;
ALTER TABLE user_matches ADD COLUMN IF NOT EXISTS common_genres VARCHAR(500);
ALTER TABLE user_matches ADD COLUMN IF NOT EXISTS match_type VARCHAR(20) DEFAULT 'MUSIC_TASTE';
ALTER TABLE user_matches ADD COLUMN IF NOT EXISTS last_interaction_at TIMESTAMP;