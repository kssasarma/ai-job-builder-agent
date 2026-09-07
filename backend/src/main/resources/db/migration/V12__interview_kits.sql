-- Auto-generated, per-candidate interview questions targeting the exact gaps
-- CandidateMatch already identified, so every interviewer works from the same
-- standardized kit instead of ad hoc questions.
CREATE TABLE interview_kits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_match_id UUID UNIQUE NOT NULL REFERENCES candidate_matches(id) ON DELETE CASCADE,
    questions JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
