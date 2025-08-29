-- 테스트용 리뷰 데이터 추가

-- 테스트용 MusicItem 생성 (다른 사용자의 리뷰용)
INSERT INTO music_items (external_id, item_type, name, artist_name, album_name, image_url, created_at, updated_at) 
VALUES ('test-track-001', 'TRACK', 'Shape of You', 'Ed Sheeran', '÷ (Divide)', 'https://i.scdn.co/image/ab67616d0000b273ba5db46f4b838ef6027e6f96', NOW(), NOW())
ON CONFLICT (external_id, item_type) DO NOTHING;

-- 테스트용 리뷰 생성 (사용자 ID 1이 작성, 사용자 ID 2가 도움이 됨을 누를 수 있음)
INSERT INTO music_reviews (user_id, music_item_id, rating, review_text, tags, helpful_count, created_at, updated_at, is_public)
SELECT 
    1 as user_id,
    mi.id as music_item_id,
    5 as rating,
    '정말 좋은 곡입니다! Ed Sheeran의 대표작 중 하나라고 생각해요. 멜로디가 중독적이고 가사도 로맨틱합니다.' as review_text,
    ARRAY['팝', '로맨틱', '중독성'] as tags,
    0 as helpful_count,
    NOW() as created_at,
    NOW() as updated_at,
    true as is_public
FROM music_items mi 
WHERE mi.external_id = 'test-track-001' AND mi.item_type = 'TRACK';