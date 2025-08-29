-- 사용자가 생성한 모든 리뷰 데이터 삭제

-- 모든 관련 트리거와 함수 완전 제거
DROP TRIGGER IF EXISTS update_music_item_stats_trigger ON music_reviews;
DROP TRIGGER IF EXISTS trigger_update_stats ON music_reviews;
DROP FUNCTION IF EXISTS update_music_item_stats() CASCADE;

-- 테이블 데이터 삭제
TRUNCATE TABLE music_item_stats CASCADE;
TRUNCATE TABLE review_helpful CASCADE;
TRUNCATE TABLE music_reviews CASCADE;
TRUNCATE TABLE user_badges CASCADE;

-- 테스트용 음악 아이템만 남기고 모든 사용자 생성 음악 아이템 삭제
DELETE FROM music_items WHERE external_id NOT LIKE 'test-%';

-- 함수와 트리거 재생성 (수정된 버전)
CREATE OR REPLACE FUNCTION update_music_item_stats()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        -- 삭제된 리뷰의 music_item_id 처리
        IF OLD.music_item_id IS NOT NULL THEN
            INSERT INTO music_item_stats (music_item_id, average_rating, total_reviews, rating_distribution, last_updated)
            SELECT 
                OLD.music_item_id,
                COALESCE(AVG(rating), 0)::DECIMAL(3,2),
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
            WHERE music_item_id = OLD.music_item_id AND is_public = TRUE
            ON CONFLICT (music_item_id) 
            DO UPDATE SET
                average_rating = EXCLUDED.average_rating,
                total_reviews = EXCLUDED.total_reviews,
                rating_distribution = EXCLUDED.rating_distribution,
                last_updated = EXCLUDED.last_updated;
        END IF;
        RETURN OLD;
    ELSE
        -- INSERT 또는 UPDATE의 경우
        IF NEW.music_item_id IS NOT NULL THEN
            INSERT INTO music_item_stats (music_item_id, average_rating, total_reviews, rating_distribution, last_updated)
            SELECT 
                NEW.music_item_id,
                COALESCE(AVG(rating), 0)::DECIMAL(3,2),
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
        END IF;
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- 트리거 재생성
CREATE TRIGGER update_music_item_stats_trigger
    AFTER INSERT OR UPDATE OR DELETE ON music_reviews
    FOR EACH ROW EXECUTE FUNCTION update_music_item_stats();