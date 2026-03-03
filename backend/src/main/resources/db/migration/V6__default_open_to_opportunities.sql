-- Default open_to_opportunities to true for new candidates
ALTER TABLE candidate_profiles ALTER COLUMN open_to_opportunities SET DEFAULT true;

-- Update existing records where open_to_opportunities = false and the candidate has at least one resume
UPDATE candidate_profiles
SET open_to_opportunities = true
WHERE open_to_opportunities = false
  AND id IN (SELECT candidate_id FROM resumes);
