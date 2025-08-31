-- 시스템 알림 테이블 생성
CREATE TABLE system_notifications (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(50) NOT NULL CHECK (type IN ('SYSTEM_ANNOUNCEMENT', 'ADMIN_MESSAGE', 'UPDATE_NOTIFICATION', 'MAINTENANCE_NOTICE', 'EVENT_NOTIFICATION')),
    title VARCHAR(100) NOT NULL,
    message VARCHAR(500) NOT NULL,
    target_type VARCHAR(20) NOT NULL CHECK (target_type IN ('ALL', 'SPECIFIC')),
    target_user_ids TEXT, -- JSON 배열 형태의 사용자 ID 목록
    priority VARCHAR(10) NOT NULL CHECK (priority IN ('LOW', 'NORMAL', 'HIGH')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'SENDING', 'SENT', 'FAILED', 'SCHEDULED')),
    created_at TIMESTAMPTZ NOT NULL,
    scheduled_at TIMESTAMPTZ, -- 예약 발송 시간 (NULL이면 즉시 발송)
    sent_at TIMESTAMPTZ -- 실제 발송 시간
);

-- 인덱스 생성
CREATE INDEX idx_system_notifications_type ON system_notifications(type);
CREATE INDEX idx_system_notifications_status ON system_notifications(status);
CREATE INDEX idx_system_notifications_created_at ON system_notifications(created_at DESC);
CREATE INDEX idx_system_notifications_scheduled_at ON system_notifications(scheduled_at) WHERE scheduled_at IS NOT NULL;

-- 테이블 코멘트
COMMENT ON TABLE system_notifications IS '시스템 알림 테이블';
COMMENT ON COLUMN system_notifications.type IS '알림 유형 (SYSTEM_ANNOUNCEMENT, ADMIN_MESSAGE, UPDATE_NOTIFICATION, MAINTENANCE_NOTICE, EVENT_NOTIFICATION)';
COMMENT ON COLUMN system_notifications.title IS '알림 제목 (최대 100자)';
COMMENT ON COLUMN system_notifications.message IS '알림 내용 (최대 500자)';
COMMENT ON COLUMN system_notifications.target_type IS '발송 대상 유형 (ALL: 전체, SPECIFIC: 특정 사용자)';
COMMENT ON COLUMN system_notifications.target_user_ids IS '특정 사용자 대상일 경우의 사용자 ID 목록 (JSON 배열)';
COMMENT ON COLUMN system_notifications.priority IS '우선순위 (LOW, NORMAL, HIGH)';
COMMENT ON COLUMN system_notifications.status IS '알림 상태 (PENDING, SENDING, SENT, FAILED, SCHEDULED)';
COMMENT ON COLUMN system_notifications.created_at IS '알림 생성 시간 (KST)';
COMMENT ON COLUMN system_notifications.scheduled_at IS '예약 발송 시간 (KST, NULL이면 즉시 발송)';
COMMENT ON COLUMN system_notifications.sent_at IS '실제 발송 완료 시간 (KST)';