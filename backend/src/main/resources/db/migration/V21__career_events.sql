-- Career record, not a resume: a living timeline that keeps compounding value
-- across job searches instead of resetting to a blank upload every time.
CREATE TABLE career_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id UUID NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
    type VARCHAR(30) NOT NULL, -- PROMOTION, SKILL_GAINED, JOB_COMPLETED, CERTIFICATION
    title VARCHAR(255) NOT NULL,
    description TEXT,
    event_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_career_events_candidate ON career_events(candidate_id, event_date DESC);
