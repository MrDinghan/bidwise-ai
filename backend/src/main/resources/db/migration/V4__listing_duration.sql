-- P3: bounded auction duration. The seller picks a fixed duration (1/3/5 days) and
-- the server derives end_at on publish, instead of accepting an arbitrary end time.
-- The window must stay within Stripe's ~7-day pre-auth hold validity (see DESIGN §9).

ALTER TABLE listings ADD COLUMN duration VARCHAR(20);

-- Backfill any pre-P3 rows so the column can be made NOT NULL. THREE_DAYS is a
-- sensible default for the small amount of dev data created before this change.
UPDATE listings SET duration = 'THREE_DAYS' WHERE duration IS NULL;

ALTER TABLE listings ALTER COLUMN duration SET NOT NULL;
