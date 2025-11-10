-- Migration: Add SM-2 Algorithm fields to user_vocabularies table
-- Date: 2025-01-XX
-- Description: This migration updates existing records with default values for SM-2 fields
-- 
-- IMPORTANT: Run this migration after the application has created the columns via ddl-auto: update
-- If columns don't exist yet, they will be created automatically by Hibernate/JPA
--
-- Usage in DBeaver:
--   1. Connect to your database
--   2. Select the vocabulary_db database
--   3. Open this SQL script
--   4. Execute the script (F5 or Ctrl+Enter)

-- Step 1: Update existing records with NULL values to have default SM-2 values
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

-- Step 2: Verify the migration
-- Count records with NULL values (should be 0 after migration)
SELECT 
    'Migration Verification' as info,
    COUNT(*) as total_records,
    SUM(CASE WHEN easiness_factor IS NULL THEN 1 ELSE 0 END) as null_easiness_factor,
    SUM(CASE WHEN interval_days IS NULL THEN 1 ELSE 0 END) as null_interval_days,
    SUM(CASE WHEN repetitions IS NULL THEN 1 ELSE 0 END) as null_repetitions
FROM user_vocabularies;

-- Step 3: Display sample records to verify migration
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

-- Migration completed!
-- All existing records now have default SM-2 values:
--   - easiness_factor: 2.5 (default for new items in SM-2 algorithm)
--   - interval_days: 0 (not yet reviewed, will be set on first review)
--   - repetitions: 0 (no successful consecutive reviews yet)

