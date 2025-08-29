-- 도움이 됨 테스트용 곡 5개 추가

INSERT INTO music_items (external_id, item_type, name, artist_name, album_name, image_url, created_at, updated_at) VALUES
('test-song-001', 'TRACK', 'Bohemian Rhapsody', 'Queen', 'A Night at the Opera', 'https://i.scdn.co/image/ab67616d0000b273ce4f1737bc8a646c8c4bd25a', NOW(), NOW()),
('test-song-002', 'TRACK', 'Stairway to Heaven', 'Led Zeppelin', 'Led Zeppelin IV', 'https://i.scdn.co/image/ab67616d0000b273c8a11e48c91a982d086afc69', NOW(), NOW()),
('test-song-003', 'TRACK', 'Hotel California', 'Eagles', 'Hotel California', 'https://i.scdn.co/image/ab67616d0000b273a9b1a7e26eb39d74b5e0e4cb', NOW(), NOW()),
('test-song-004', 'TRACK', 'Imagine', 'John Lennon', 'Imagine', 'https://i.scdn.co/image/ab67616d0000b27354c5495d6a6a99cfffe1a595', NOW(), NOW()),
('test-song-005', 'TRACK', 'Smells Like Teen Spirit', 'Nirvana', 'Nevermind', 'https://i.scdn.co/image/ab67616d0000b27351b0418c19aa9d5e9fe79176', NOW(), NOW());

-- 각 테스트 곡에 대해 리뷰 1개씩 추가 (사용자 ID 1로 가정)
INSERT INTO music_reviews (user_id, music_item_id, rating, review_text, tags, is_spoiler, is_public, helpful_count, report_count, created_at, updated_at) VALUES
(1, (SELECT id FROM music_items WHERE external_id = 'test-song-001'), 5, 'Queen의 대표작! 6분 동안 다양한 장르가 혼합된 완벽한 곡입니다.', ARRAY['클래식록', '오페라록', '명곡'], false, true, 0, 0, NOW(), NOW()),
(1, (SELECT id FROM music_items WHERE external_id = 'test-song-002'), 5, 'Led Zeppelin의 최고작. 8분의 대서사시가 펼쳐집니다.', ARRAY['하드록', '블루스록', '레전드'], false, true, 0, 0, NOW(), NOW()),
(1, (SELECT id FROM music_items WHERE external_id = 'test-song-003'), 4, 'Eagles의 시그니처 송. 기타 솔로가 정말 인상적입니다.', ARRAY['컨트리록', '웨스트코스트', '기타솔로'], false, true, 0, 0, NOW(), NOW()),
(1, (SELECT id FROM music_items WHERE external_id = 'test-song-004'), 5, 'John Lennon의 평화 메시지가 담긴 명곡. 가사가 감동적입니다.', ARRAY['평화', '팝록', '메시지'], false, true, 0, 0, NOW(), NOW()),
(1, (SELECT id FROM music_items WHERE external_id = 'test-song-005'), 4, 'Nirvana의 그런지 록 대표작. 90년대를 상징하는 곡입니다.', ARRAY['그런지', '얼터너티브', '90년대'], false, true, 0, 0, NOW(), NOW());