-- Gap-aware mock interview sessions: questions are generated from a specific
-- AMBER-tier TailoringHistory's GapAnalysisResponse, and each answer is scored
-- against the skill it targets so practice stays focused on the real gap.
CREATE TABLE mock_interview_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tailoring_history_id UUID NOT NULL REFERENCES tailoring_history(id) ON DELETE CASCADE,
    candidate_id UUID NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE mock_interview_questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES mock_interview_sessions(id) ON DELETE CASCADE,
    target_skill VARCHAR(255) NOT NULL,
    question TEXT NOT NULL,
    ideal_talking_points JSONB,
    candidate_answer TEXT,
    feedback TEXT,
    score INTEGER,
    display_order INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mock_interview_questions_session ON mock_interview_questions(session_id);
CREATE INDEX idx_mock_interview_sessions_candidate ON mock_interview_sessions(candidate_id);
