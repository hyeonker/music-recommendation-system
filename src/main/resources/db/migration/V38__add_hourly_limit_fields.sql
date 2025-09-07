-- V38: Add hourly refresh limit tracking fields
-- Add fields to track hourly refresh count and reset time

ALTER TABLE user_recommendation_activity
ADD COLUMN hourly_count INTEGER NOT NULL DEFAULT 0,
ADD COLUMN last_hourly_reset TIMESTAMP NULL;

-- Add comment to document the enhancement
COMMENT ON COLUMN user_recommendation_activity.hourly_count IS 'Number of refreshes within current hour';
COMMENT ON COLUMN user_recommendation_activity.last_hourly_reset IS 'Timestamp when hourly count was last reset';

-- Update existing records to have default values
UPDATE user_recommendation_activity 
SET hourly_count = 0, 
    last_hourly_reset = NULL 
WHERE hourly_count IS NULL;