-- Consent-based AI data use, anonymized-first discovery, and proof-of-skill
-- micro-credentials all hang small flags/columns off the existing candidate profile
-- rather than new tables, since they're per-candidate settings/state.
ALTER TABLE candidate_profiles ADD COLUMN ai_consent BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE candidate_profiles ADD COLUMN anonymized_discovery BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE candidate_profiles ADD COLUMN verified_skills TEXT[];
