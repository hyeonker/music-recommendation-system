-- 기존 사용자들의 NULL인 created_at을 updated_at 값으로 설정
-- 이렇게 하면 프론트엔드에서 1970.1.1로 표시되지 않고 실제 가입일(또는 마지막 업데이트일)이 표시됨

UPDATE users 
SET created_at = updated_at 
WHERE created_at IS NULL;

-- 앞으로 created_at이 NULL이 되는 것을 방지하기 위해 NOT NULL 제약조건 추가
-- (이미 DEFAULT CURRENT_TIMESTAMP가 설정되어 있으므로 새로운 사용자는 문제없음)
ALTER TABLE users ALTER COLUMN created_at SET NOT NULL;