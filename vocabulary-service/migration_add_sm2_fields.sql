-- Migration: Add SM-2 Algorithm fields to user_vocabularies table (DBeaver Compatible)
-- Date: 2025-01-XX
-- Description: This migration adds columns and default values for SM-2 spaced repetition fields
-- 
-- Usage in DBeaver:
--   1. Connect to your database
--   2. Select the vocabulary_db database
--   3. Open this SQL script
--   4. Execute the script (F5 or Ctrl+Enter)
--   5. If columns already exist, you'll get an error - that's OK, just continue

-- Step 1: Add columns if they don't exist
-- Note: If columns already exist, you'll get an error - that's fine, just continue
-- The columns should be created automatically by Hibernate/JPA (ddl-auto: update)

-- Add easiness_factor column (if it doesn't exist)
ALTER TABLE user_vocabularies 
ADD COLUMN IF NOT EXISTS easiness_factor DOUBLE DEFAULT 2.5;

-- Add interval_days column (if it doesn't exist)
ALTER TABLE user_vocabularies 
ADD COLUMN IF NOT EXISTS interval_days INT DEFAULT 0;

-- Add repetitions column (if it doesn't exist)
ALTER TABLE user_vocabularies 
ADD COLUMN IF NOT EXISTS repetitions INT DEFAULT 0;

-- Note: If your MySQL version doesn't support "IF NOT EXISTS" in ALTER TABLE,
-- you can manually check if columns exist first, or just run the UPDATE statements below
-- The UPDATE statements will work even if columns already exist

-- Step 2: Update existing records with NULL values to have default values
-- This ensures all existing records have proper SM-2 initialization

-- Update easiness_factor: Set to 2.5 (default) for NULL values
UPDATE user_vocabularies
SET easiness_factor = 2.5
WHERE easiness_factor IS NULL;

-- Update interval_days: Set to 0 (default) for NULL values
UPDATE user_vocabularies
SET interval_days = 0
WHERE interval_days IS NULL;

-- Update repetitions: Set to 0 (default) for NULL values
UPDATE user_vocabularies
SET repetitions = 0
WHERE repetitions IS NULL;

-- Step 3: Verify the migration
-- Count records with NULL values (should be 0 after migration)
SELECT 
    'Migration Verification' as info,
    COUNT(*) as total_records,
    SUM(CASE WHEN easiness_factor IS NULL THEN 1 ELSE 0 END) as null_easiness_factor,
    SUM(CASE WHEN interval_days IS NULL THEN 1 ELSE 0 END) as null_interval_days,
    SUM(CASE WHEN repetitions IS NULL THEN 1 ELSE 0 END) as null_repetitions
FROM user_vocabularies;

-- Step 4: Display sample records to verify
SELECT 
    id,
    user_id,
    word_id,
    easiness_factor,
    interval_days,
    repetitions,
    status,
    review_count,
    correct_count,
    incorrect_count
FROM user_vocabularies
ORDER BY id
LIMIT 10;

-- Migration completed successfully!
-- All existing records now have default SM-2 values:
--   - easiness_factor: 2.5 (default for new items)
--   - interval_days: 0 (not yet reviewed)
--   - repetitions: 0 (no successful consecutive reviews)

