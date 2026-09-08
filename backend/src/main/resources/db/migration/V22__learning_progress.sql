-- Gap-to-learning bridge: closes the loop the gap analysis opens — "here's what's
-- missing" becomes "mark it learned" becomes "re-check compatibility and see the
-- score move."
CREATE TABLE learning_progress (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tailoring_history_id UUID NOT NULL REFERENCES tailoring_history(id) ON DELETE CASCADE,
    skill VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    UNIQUE(tailoring_history_id, skill)
);
