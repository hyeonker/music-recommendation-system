-- 모든 사용자의 새로고침 제한 횟수 초기화 스크립트
-- 실행: psql -h localhost -U music -d music -f reset_limits.sql

BEGIN;

-- 기존 새로고침 카운트 초기화
UPDATE user_recommendation_activity 
SET 
    refresh_count = 0,
    hourly_count = 0,
    last_refresh_at = NULL,
    last_hourly_reset = NULL,
    updated_at = CURRENT_TIMESTAMP
WHERE refresh_count > 0 OR hourly_count > 0;

-- 영향받은 행 수 확인
SELECT 
    COUNT(*) as total_users,
    SUM(CASE WHEN refresh_count = 0 THEN 1 ELSE 0 END) as reset_users
FROM user_recommendation_activity;

-- 각 사용자별 현재 상태 확인
SELECT 
    user_id,
    activity_date,
    refresh_count,
    hourly_count,
    last_refresh_at,
    last_hourly_reset
FROM user_recommendation_activity 
ORDER BY user_id, activity_date DESC;

COMMIT;

ECHO "모든 사용자의 새로고침 제한이 초기화되었습니다.";