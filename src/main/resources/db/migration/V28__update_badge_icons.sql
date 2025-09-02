-- 기존 배지들의 아이콘을 DiceBear API를 사용한 새로운 아이콘으로 업데이트

UPDATE user_badges SET icon_url = 'https://api.dicebear.com/7.x/bottts/svg?seed=first_review&backgroundColor=c084fc&size=96&radius=20'
WHERE badge_type = 'FIRST_REVIEW';

UPDATE user_badges SET icon_url = 'https://api.dicebear.com/7.x/adventurer/svg?seed=review_master&backgroundColor=3b82f6&size=96&radius=20'
WHERE badge_type = 'REVIEW_MASTER';

UPDATE user_badges SET icon_url = 'https://api.dicebear.com/7.x/fun-emoji/svg?seed=helpful_reviewer&backgroundColor=10b981&size=96&radius=20'
WHERE badge_type = 'HELPFUL_REVIEWER';

UPDATE user_badges SET icon_url = 'https://api.dicebear.com/7.x/lorelei/svg?seed=critic&backgroundColor=f59e0b&size=96&radius=20'
WHERE badge_type = 'CRITIC';

UPDATE user_badges SET icon_url = 'https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=genre_explorer&backgroundColor=8b5cf6&size=96&radius=20'
WHERE badge_type = 'GENRE_EXPLORER';

UPDATE user_badges SET icon_url = 'https://api.dicebear.com/7.x/shapes/svg?seed=early_adopter&backgroundColor=06b6d4&size=96&radius=20'
WHERE badge_type = 'EARLY_ADOPTER';

UPDATE user_badges SET icon_url = 'https://api.dicebear.com/7.x/identicon/svg?seed=music_discoverer&backgroundColor=d946ef&size=96&radius=20'
WHERE badge_type = 'MUSIC_DISCOVERER';

UPDATE user_badges SET icon_url = 'https://api.dicebear.com/7.x/avataaars/svg?seed=social_butterfly&backgroundColor=f97316&size=96&radius=20'
WHERE badge_type = 'SOCIAL_BUTTERFLY';

UPDATE user_badges SET icon_url = 'https://api.dicebear.com/7.x/personas/svg?seed=friend_maker&backgroundColor=84cc16&size=96&radius=20'
WHERE badge_type = 'FRIEND_MAKER';

UPDATE user_badges SET icon_url = 'https://api.dicebear.com/7.x/notionists/svg?seed=chat_master&backgroundColor=6366f1&size=96&radius=20'
WHERE badge_type = 'CHAT_MASTER';

UPDATE user_badges SET icon_url = 'https://api.dicebear.com/7.x/bottts-neutral/svg?seed=beta_tester&backgroundColor=64748b&size=96&radius=20'
WHERE badge_type = 'BETA_TESTER';

UPDATE user_badges SET icon_url = 'https://api.dicebear.com/7.x/thumbs/svg?seed=anniversary&backgroundColor=dc2626&size=96&radius=20'
WHERE badge_type = 'ANNIVERSARY';

UPDATE user_badges SET icon_url = 'https://api.dicebear.com/7.x/fun-emoji/svg?seed=special_event&backgroundColor=7c3aed&size=96&radius=20'
WHERE badge_type = 'SPECIAL_EVENT';

UPDATE user_badges SET icon_url = 'https://api.dicebear.com/7.x/lorelei/svg?seed=admin&backgroundColor=eab308&size=96&radius=20'
WHERE badge_type = 'ADMIN';

UPDATE user_badges SET icon_url = 'https://api.dicebear.com/7.x/personas/svg?seed=founder&backgroundColor=dc2626&size=96&radius=20'
WHERE badge_type = 'FOUNDER';

UPDATE user_badges SET icon_url = 'https://api.dicebear.com/7.x/adventurer/svg?seed=community_leader&backgroundColor=059669&size=96&radius=20'
WHERE badge_type = 'COMMUNITY_LEADER';

UPDATE user_badges SET icon_url = 'https://api.dicebear.com/7.x/shapes/svg?seed=content_curator&backgroundColor=7c2d12&size=96&radius=20'
WHERE badge_type = 'CONTENT_CURATOR';

UPDATE user_badges SET icon_url = 'https://api.dicebear.com/7.x/avataaars/svg?seed=trendsetter&backgroundColor=be123c&size=96&radius=20'
WHERE badge_type = 'TRENDSETTER';

UPDATE user_badges SET icon_url = 'https://api.dicebear.com/7.x/bottts/svg?seed=quality_guardian&backgroundColor=1e40af&size=96&radius=20'
WHERE badge_type = 'QUALITY_GUARDIAN';

UPDATE user_badges SET icon_url = 'https://api.dicebear.com/7.x/identicon/svg?seed=innovation_pioneer&backgroundColor=9333ea&size=96&radius=20'
WHERE badge_type = 'INNOVATION_PIONEER';

UPDATE user_badges SET icon_url = 'https://api.dicebear.com/7.x/lorelei/svg?seed=music_scholar&backgroundColor=166534&size=96&radius=20'
WHERE badge_type = 'MUSIC_SCHOLAR';

UPDATE user_badges SET icon_url = 'https://api.dicebear.com/7.x/fun-emoji/svg?seed=platinum_member&backgroundColor=4338ca&size=96&radius=20'
WHERE badge_type = 'PLATINUM_MEMBER';