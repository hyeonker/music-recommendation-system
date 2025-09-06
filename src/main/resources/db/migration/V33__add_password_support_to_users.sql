-- V33: 아이디/비밀번호 로그인을 위한 사용자 테이블 확장
-- 작성일: 2025-09-06
-- 목적: LOCAL 인증 제공자와 비밀번호 해시 컬럼 추가

-- 1. password_hash 컬럼 추가 (아이디/비밀번호 로그인용)
ALTER TABLE users 
ADD COLUMN password_hash VARCHAR(255);

-- 2. provider_id 컬럼을 NOT NULL에서 NULL 허용으로 변경 (아이디/비밀번호 로그인에서는 null)
ALTER TABLE users 
ALTER COLUMN provider_id DROP NOT NULL;

-- 3. LOCAL 인증 제공자 추가를 위한 provider 컬럼 제약 조건 업데이트
-- (enum 타입이라면 추후 애플리케이션 재시작 시 자동으로 LOCAL이 추가됨)

-- 4. 인덱스 추가: 로컬 로그인 성능 향상
CREATE INDEX IF NOT EXISTS idx_users_email_provider ON users(email, provider);
CREATE INDEX IF NOT EXISTS idx_users_provider ON users(provider);

-- 5. 주석 추가
COMMENT ON COLUMN users.password_hash IS '아이디/비밀번호 로그인용 BCrypt 해시된 비밀번호';
COMMENT ON COLUMN users.provider_id IS 'OAuth2 제공자 ID (LOCAL 로그인 시 NULL)';