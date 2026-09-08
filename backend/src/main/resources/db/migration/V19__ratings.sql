-- Two-sided reputation: candidates rate recruiters/employers and vice versa, the
-- same way most platforms only let one side rate the other.
CREATE TABLE ratings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rater_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    ratee_type VARCHAR(20) NOT NULL, -- CANDIDATE or RECRUITER
    ratee_id UUID NOT NULL, -- candidate_profiles.id or recruiter_profiles.id depending on ratee_type
    job_application_id UUID REFERENCES job_applications(id) ON DELETE SET NULL,
    score INTEGER NOT NULL CHECK (score BETWEEN 1 AND 5),
    comment TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(rater_user_id, ratee_type, ratee_id, job_application_id)
);

CREATE INDEX idx_ratings_ratee ON ratings(ratee_type, ratee_id);
