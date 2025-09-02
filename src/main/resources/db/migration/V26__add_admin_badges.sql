-- 운영자(USER ID 1)에게 모든 배지 부여 및 운영자 전용 배지 추가

-- 기존에 없는 배지들을 운영자에게 추가
INSERT INTO user_badges (user_id, badge_type, badge_name, description, icon_url, earned_at, metadata)
VALUES 
-- 소셜 나비 배지 추가 (기존에 다른 사용자만 가지고 있음)
(1, 'SOCIAL_BUTTERFLY', '소셜 나비', '활발한 소셜 활동을 보였습니다!', '🦋', NOW() - INTERVAL '1 day', '{"socialScore": 100}'),

-- 운영자 전용 배지들 추가
(1, 'ADMIN', '시스템 관리자', '시스템 운영자입니다!', '👑', NOW() - INTERVAL '30 days', '{"adminLevel": "SUPER"}'),
(1, 'FOUNDER', '창립자', '서비스의 창립자입니다!', '🏆', NOW() - INTERVAL '30 days', '{"foundingDate": "2024-01-01"}'),
(1, 'BETA_TESTER', '베타 테스터', '서비스 초기 베타 테스터입니다!', '🧪', NOW() - INTERVAL '25 days', '{"betaPhase": "alpha"}'),
(1, 'COMMUNITY_LEADER', '커뮤니티 리더', '커뮤니티를 이끌어가는 리더입니다!', '🌟', NOW() - INTERVAL '20 days', '{"leadershipScore": 95}'),
(1, 'CONTENT_CURATOR', '콘텐츠 큐레이터', '훌륭한 콘텐츠를 큐레이션합니다!', '🎨', NOW() - INTERVAL '15 days', '{"curationCount": 50}'),
(1, 'TRENDSETTER', '트렌드세터', '음악 트렌드를 선도합니다!', '🔥', NOW() - INTERVAL '10 days', '{"trendScore": 88}'),
(1, 'QUALITY_GUARDIAN', '품질 관리자', '서비스 품질을 관리합니다!', '🛡️', NOW() - INTERVAL '7 days', '{"qualityScore": 99}'),
(1, 'INNOVATION_PIONEER', '혁신 개척자', '새로운 기능을 개척합니다!', '🚀', NOW() - INTERVAL '5 days', '{"innovationCount": 15}'),
(1, 'MUSIC_SCHOLAR', '음악 학자', '음악에 대한 깊은 지식을 가지고 있습니다!', '🎓', NOW() - INTERVAL '3 days', '{"knowledgeScore": 92}'),
(1, 'PLATINUM_MEMBER', '플래티넘 멤버', '최고 등급 멤버입니다!', '💎', NOW() - INTERVAL '2 days', '{"membershipLevel": "PLATINUM"}')

-- 중복 방지 (이미 있는 배지는 무시)
ON CONFLICT (user_id, badge_type) DO NOTHING;