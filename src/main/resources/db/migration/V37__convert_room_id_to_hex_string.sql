-- V37: Convert chat_message.room_id from BIGINT to VARCHAR and transform existing data

-- 1. Add new column for hex string room_id
ALTER TABLE chat_message ADD COLUMN room_id_new VARCHAR(50);

-- 2. Convert existing numeric room_id values to hex strings (PostgreSQL format)
-- PostgreSQL doesn't have TO_HEX, so we use a different approach
UPDATE chat_message 
SET room_id_new = LPAD(room_id::TEXT, 16, '0')
WHERE room_id IS NOT NULL;

-- 3. Drop old column and rename new column
ALTER TABLE chat_message DROP COLUMN room_id;
ALTER TABLE chat_message RENAME COLUMN room_id_new TO room_id;

-- 4. Add NOT NULL constraint to the new room_id column
ALTER TABLE chat_message ALTER COLUMN room_id SET NOT NULL;

-- 5. Update any indexes that might exist on room_id
-- (Create index if it doesn't exist for better performance)
CREATE INDEX IF NOT EXISTS idx_chat_message_room_id ON chat_message(room_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_created_at ON chat_message(created_at);