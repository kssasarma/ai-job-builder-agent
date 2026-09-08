-- Collaborative hiring rooms: threaded, timestamped feedback on one CandidateMatch
-- so interviewers leave structured notes in one place instead of an email chain.
CREATE TABLE match_comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_match_id UUID NOT NULL REFERENCES candidate_matches(id) ON DELETE CASCADE,
    author_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    body TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_match_comments_match ON match_comments(candidate_match_id, created_at);
