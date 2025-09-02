-- 운영자(USER ID 1)에게 누락된 배지들 추가

INSERT INTO user_badges (user_id, badge_type, badge_name, description, icon_url, earned_at, metadata)
VALUES 
-- 리뷰 관련 배지
(1, 'REVIEW_MASTER', '리뷰 마스터', '10개 이상의 리뷰를 작성한 숙련된 리뷰어입니다!', '📝', NOW() - INTERVAL '20 days', '{"reviewCount": 25}'),
(1, 'HELPFUL_REVIEWER', '도움이 되는 리뷰어', '다른 사용자들에게 도움이 되는 리뷰를 작성합니다!', '👍', NOW() - INTERVAL '18 days', '{"helpfulCount": 50}'),
(1, 'CRITIC', '음악 평론가', '전문가 수준의 음악 평론가입니다!', '🎭', NOW() - INTERVAL '15 days', '{"expertReviews": 75}'),

-- 음악 탐험 배지
(1, 'GENRE_EXPLORER', '장르 탐험가', '다양한 음악 장르를 탐험하는 모험가입니다!', '🗺️', NOW() - INTERVAL '12 days', '{"genresExplored": 15}'),
(1, 'EARLY_ADOPTER', '얼리 어답터', '새로운 음악을 빠르게 발견하는 선구자입니다!', '⚡', NOW() - INTERVAL '10 days', '{"earlyFinds": 30}'),
(1, 'MUSIC_DISCOVERER', '음악 발굴자', '숨겨진 음악 보석을 찾아내는 발굴 전문가입니다!', '💎', NOW() - INTERVAL '8 days', '{"hiddenGems": 20}'),

-- 소셜 활동 배지
(1, 'FRIEND_MAKER', '친구 메이커', '많은 음악 친구들을 만든 사교적인 멤버입니다!', '🤝', NOW() - INTERVAL '6 days', '{"friendsCount": 100}'),
(1, 'CHAT_MASTER', '대화의 달인', '활발한 대화로 커뮤니티를 이끄는 소통의 달인입니다!', '💬', NOW() - INTERVAL '4 days', '{"chatMessages": 500}'),

-- 특별 배지
(1, 'ANNIVERSARY', '기념일', '특별한 기념일을 함께한 소중한 멤버입니다!', '🎉', NOW() - INTERVAL '2 days', '{"anniversaryYear": "2024"}'),
(1, 'SPECIAL_EVENT', '특별 이벤트', '특별한 이벤트에 참여한 활동적인 멤버입니다!', '🌟', NOW() - INTERVAL '1 day', '{"eventName": "Launch Party"}')

-- 중복 방지 (이미 있는 배지는 무시)
ON CONFLICT (user_id, badge_type) DO NOTHING;