-- Proof-of-skill micro-credentials: an AI-graded challenge per skill. Passing
-- (score >= 70) appends the skill to candidate_profiles.verified_skills, turning a
-- self-reported skill into scored evidence.
CREATE TABLE skill_challenges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id UUID NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
    skill VARCHAR(255) NOT NULL,
    question TEXT NOT NULL,
    ideal_answer_points JSONB,
    answer TEXT,
    score INTEGER,
    passed BOOLEAN,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_skill_challenges_candidate ON skill_challenges(candidate_id);
