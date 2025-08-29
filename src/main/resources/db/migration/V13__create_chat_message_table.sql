-- 채팅 메시지 보관 테이블 (암호문 저장)
CREATE TABLE IF NOT EXISTS chat_message (
                                            id               BIGSERIAL PRIMARY KEY,
                                            room_id          BIGINT      NOT NULL,
                                            sender_id        BIGINT      NOT NULL,
                                            content_cipher   TEXT        NOT NULL,    -- AES-GCM 암호문(Base64)
                                            iv_base64        TEXT        NOT NULL,    -- AES-GCM IV(Base64, 12바이트)
                                            created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    hold_for_dispute BOOLEAN     NOT NULL DEFAULT FALSE -- 신고/분쟁 한시보존 플래그
    );

-- 조회/페이징 성능용 인덱스
CREATE INDEX IF NOT EXISTS idx_chat_msg_room_created   ON chat_message (room_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_chat_msg_sender_created ON chat_message (sender_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_chat_msg_created        ON chat_message (created_at);
CREATE INDEX IF NOT EXISTS idx_chat_msg_hold           ON chat_message (hold_for_dispute);
