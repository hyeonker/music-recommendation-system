-- 테스트용 리뷰/배지 데이터 추가
-- V17: 테스트 데이터

-- 테스트 음악 아이템 추가
INSERT INTO music_items (external_id, item_type, name, artist_name, album_name, genre, image_url, external_url, created_at, updated_at)
VALUES 
('test_track_1', 'TRACK', 'Dynamite', 'BTS', 'BE', 'K-Pop', 'https://i.scdn.co/image/ab67616d0000b273c9b7d506ec8cdc7b8b0a1e2c', 'https://open.spotify.com/track/test1', NOW(), NOW()),
('test_track_2', 'TRACK', 'Blinding Lights', 'The Weeknd', 'After Hours', 'Pop', 'https://i.scdn.co/image/ab67616d0000b2738863bc11d2aa12b54f5aeb36', 'https://open.spotify.com/track/test2', NOW(), NOW()),
('test_track_3', 'TRACK', 'Good 4 U', 'Olivia Rodrigo', 'Sour', 'Pop', 'https://i.scdn.co/image/ab67616d0000b273a91c10fe9472d9bd89802e5a', 'https://open.spotify.com/track/test3', NOW(), NOW()),
('test_album_1', 'ALBUM', 'Map of the Soul: 7', 'BTS', 'Map of the Soul: 7', 'K-Pop', 'https://i.scdn.co/image/ab67616d0000b273a7d901ba966a4a3faaf30b4e', 'https://open.spotify.com/album/test1', NOW(), NOW()),
('test_artist_1', 'ARTIST', 'IU', 'IU', NULL, 'K-Pop', 'https://i.scdn.co/image/ab6761610000e5eba5b2e1b2e90e56a54dd97d34', 'https://open.spotify.com/artist/test1', NOW(), NOW());

-- 테스트 리뷰 추가 (user_id 1로 가정)
INSERT INTO music_reviews (user_id, music_item_id, rating, review_text, tags, is_spoiler, is_public, helpful_count, created_at, updated_at)
VALUES 
(1, 1, 5, '정말 신나고 에너지 넘치는 곡이에요! BTS의 매력이 가득 담긴 명곡입니다.', ARRAY['신남', '에너지틱', 'K-Pop', '추천'], FALSE, TRUE, 12, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
(1, 2, 4, 'The Weeknd의 독특한 보컬과 신스팝 사운드가 완벽하게 조화를 이룬 곡. 밤에 듣기 좋아요.', ARRAY['신스팝', '밤음악', '분위기'], FALSE, TRUE, 8, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
(1, 3, 5, '올리비아 로드리고의 솔직하고 감정적인 가사가 인상깊어요. 10대들의 마음을 잘 표현한 곡.', ARRAY['감성', '10대', '솔직함'], FALSE, TRUE, 15, NOW() - INTERVAL '3 hours', NOW() - INTERVAL '3 hours'),
(1, 4, 4, 'BTS의 앨범 중에서도 특히 다양한 장르를 시도한 앨범. 각 트랙마다 개성이 뚜렷해요.', ARRAY['다양함', '완성도', 'K-Pop'], FALSE, TRUE, 6, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
(1, 5, 5, 'IU는 역시 아이유! 감성적인 목소리와 완벽한 가창력으로 항상 감동을 주는 아티스트입니다.', ARRAY['감성', '가창력', '완벽'], FALSE, TRUE, 20, NOW() - INTERVAL '1 week', NOW() - INTERVAL '1 week');

-- 추가 사용자 리뷰 (다른 사용자들)
INSERT INTO music_reviews (user_id, music_item_id, rating, review_text, tags, is_spoiler, is_public, helpful_count, created_at, updated_at)
VALUES 
(2, 1, 4, '처음엔 별로였는데 계속 듣다 보니 중독성이 있어요. BTS 특유의 밝은 에너지가 좋습니다.', ARRAY['중독성', '밝음', 'BTS'], FALSE, TRUE, 5, NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
(3, 2, 5, '이 곡을 들으면 80년대 신스팝이 생각나요. 모던한 느낌도 있고 정말 잘 만든 곡.', ARRAY['80년대', '신스팝', '모던'], FALSE, TRUE, 9, NOW() - INTERVAL '6 days', NOW() - INTERVAL '6 days'),
(2, 3, 3, '좋은 곡이긴 한데 조금 뻔한 느낌도 있어요. 그래도 감정 표현은 잘 된 것 같습니다.', ARRAY['감정표현', '평범'], FALSE, TRUE, 2, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days');

-- 테스트 사용자 배지 추가
INSERT INTO user_badges (user_id, badge_type, badge_name, description, icon_url, earned_at, metadata)
VALUES 
(1, 'FIRST_REVIEW', '첫 리뷰어', '첫 번째 리뷰를 작성했습니다!', '🎵', NOW() - INTERVAL '1 week', '{"reviewCount": 1}'),
(1, 'REVIEW_MASTER', '리뷰 마스터', '10개 이상의 리뷰를 작성했습니다!', '📝', NOW() - INTERVAL '3 days', '{"reviewCount": 10}'),
(1, 'HELPFUL_REVIEWER', '도움이 되는 리뷰어', '리뷰가 50번 이상 도움이 됨을 받았습니다!', '👍', NOW() - INTERVAL '1 day', '{"helpfulCount": 50}'),
(1, 'MUSIC_DISCOVERER', '음악 발굴러', '다양한 장르의 음악을 리뷰했습니다!', '🎶', NOW() - INTERVAL '2 hours', '{"genreCount": 5}');

-- 배지 통계 업데이트를 위한 더미 사용자 배지
INSERT INTO user_badges (user_id, badge_type, badge_name, description, earned_at)
VALUES 
(2, 'FIRST_REVIEW', '첫 리뷰어', '첫 번째 리뷰를 작성했습니다!', NOW() - INTERVAL '5 days'),
(3, 'FIRST_REVIEW', '첫 리뷰어', '첫 번째 리뷰를 작성했습니다!', NOW() - INTERVAL '3 days'),
(2, 'SOCIAL_BUTTERFLY', '소셜 나비', '활발한 소셜 활동을 보였습니다!', NOW() - INTERVAL '2 days');