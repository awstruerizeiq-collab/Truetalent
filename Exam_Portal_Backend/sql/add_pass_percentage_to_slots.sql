ALTER TABLE slots
ADD COLUMN IF NOT EXISTS pass_percentage INT NOT NULL DEFAULT 80;

UPDATE slots
SET pass_percentage = 80
WHERE pass_percentage IS NULL;
