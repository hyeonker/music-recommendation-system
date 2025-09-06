-- Create chat_reports table for managing user reports
CREATE TABLE chat_reports (
    id BIGSERIAL PRIMARY KEY,
    reporter_id BIGINT NOT NULL REFERENCES users(id),
    reported_user_id BIGINT NOT NULL REFERENCES users(id),
    chat_room_id VARCHAR(255) NOT NULL,
    report_type VARCHAR(50) NOT NULL,
    reason TEXT NOT NULL,
    additional_info TEXT,
    status VARCHAR(20) DEFAULT 'PENDING' NOT NULL,
    admin_response TEXT,
    admin_user_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    
    -- Check constraint to prevent self-reporting
    CONSTRAINT check_not_self_report CHECK (reporter_id != reported_user_id)
);

-- Create indexes for better query performance
CREATE INDEX idx_chat_reports_reporter ON chat_reports(reporter_id);
CREATE INDEX idx_chat_reports_reported_user ON chat_reports(reported_user_id);
CREATE INDEX idx_chat_reports_status ON chat_reports(status);
CREATE INDEX idx_chat_reports_created_at ON chat_reports(created_at);
CREATE INDEX idx_chat_reports_chat_room ON chat_reports(chat_room_id);

-- Add comments
COMMENT ON TABLE chat_reports IS 'User reports for chat messages and behavior';
COMMENT ON COLUMN chat_reports.report_type IS 'Type of report: INAPPROPRIATE_MESSAGE, HARASSMENT, SPAM, FAKE_PROFILE, OTHER';
COMMENT ON COLUMN chat_reports.status IS 'Report status: PENDING, UNDER_REVIEW, RESOLVED, DISMISSED';