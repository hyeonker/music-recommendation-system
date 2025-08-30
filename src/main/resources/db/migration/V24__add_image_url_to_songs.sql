-- Add image_url column to songs table
ALTER TABLE songs ADD COLUMN image_url VARCHAR(500);

-- Add index on image_url for performance
CREATE INDEX idx_songs_image_url ON songs(image_url);

-- Update existing songs with placeholder image URLs (optional)
-- UPDATE songs SET image_url = 'https://api.dicebear.com/7.x/initials/svg?seed=' || encode(title::bytea, 'base64') WHERE image_url IS NULL;