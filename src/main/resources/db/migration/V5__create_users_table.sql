-- 사용자 테이블 생성
-- OAuth2 소셜 로그인을 통해 가입한 사용자들의 정보를 저장

CREATE TABLE users (
                       id                  BIGSERIAL PRIMARY KEY,
                       email               VARCHAR(255) NOT NULL UNIQUE,
                       name                VARCHAR(100) NOT NULL,
                       profile_image_url   VARCHAR(500),
                       provider            VARCHAR(20) NOT NULL,  -- GOOGLE, GITHUB, KAKAO, NAVER 등
                       provider_id         VARCHAR(100) NOT NULL, -- 각 소셜 로그인 제공업체에서의 사용자 고유 ID
                       created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 성능 최적화를 위한 인덱스 생성
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_provider_provider_id ON users(provider, provider_id);

-- 동일한 소셜 계정으로 중복 가입 방지를 위한 유니크 제약조건
CREATE UNIQUE INDEX uk_users_provider_provider_id ON users(provider, provider_id);

-- 테이블 설명 주석
COMMENT ON TABLE users IS 'OAuth2 소셜 로그인 사용자 정보 테이블';
COMMENT ON COLUMN users.email IS '사용자 이메일 (고유)';
COMMENT ON COLUMN users.name IS '사용자 실명';
COMMENT ON COLUMN users.profile_image_url IS '프로필 이미지 URL (소셜 로그인에서 가져옴)';
COMMENT ON COLUMN users.provider IS 'OAuth2 제공자 (GOOGLE, GITHUB, KAKAO, NAVER)';
COMMENT ON COLUMN users.provider_id IS '소셜 로그인 제공업체에서의 사용자 고유 ID';
COMMENT ON COLUMN users.created_at IS '계정 생성일시';
COMMENT ON COLUMN users.updated_at IS '계정 정보 마지막 수정일시';