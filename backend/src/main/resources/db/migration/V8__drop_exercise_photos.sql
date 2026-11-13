-- Plan 9 step 01b: the built-in exercise photos were hotlinked from
-- github.com/yuhonas/free-exercise-db, whose photos have no stated
-- origin. Dropped before release (owner, 2026-11-13); the exercise page
-- shows its stand-in. Own photos come later.
UPDATE exercises
SET thumbnail_url = NULL
WHERE source = 'DEFAULT'
  AND thumbnail_url LIKE 'https://raw.githubusercontent.com/yuhonas/free-exercise-db/%';

-- Clients replace their library when this differs from theirs.
UPDATE reference_version SET exercise_library_version = 10, updated_at = 0 WHERE id = 1;
