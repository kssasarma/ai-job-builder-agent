-- Anonymized-first discovery: when a candidate opts into candidate_profiles.
-- anonymized_discovery, a recruiter sees a skills-only card and must request the
-- full profile; nothing unlocks until the candidate approves it here.
CREATE TABLE profile_reveal_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id UUID NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
    recruiter_id UUID NOT NULL REFERENCES recruiter_profiles(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP WITH TIME ZONE,
    UNIQUE(candidate_id, recruiter_id)
);
