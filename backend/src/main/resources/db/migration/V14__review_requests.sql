-- "Request human review" — the other half of explainability: a candidate who
-- disagrees with an AI score/match can flag it for a human to look at instead
-- of just accepting the number.
CREATE TABLE review_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requested_by_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subject_type VARCHAR(50) NOT NULL,
    subject_id UUID NOT NULL,
    reason TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_review_requests_subject ON review_requests(subject_type, subject_id);
