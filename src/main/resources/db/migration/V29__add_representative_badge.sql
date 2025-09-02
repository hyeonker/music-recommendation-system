-- 사용자 테이블에 대표 배지 필드 추가
ALTER TABLE users ADD COLUMN representative_badge_id BIGINT;

-- 외래키 제약조건 추가 (선택사항 - 배지가 삭제되면 NULL로 설정)
ALTER TABLE users ADD CONSTRAINT fk_users_representative_badge 
    FOREIGN KEY (representative_badge_id) REFERENCES user_badges(id) 
    ON DELETE SET NULL;