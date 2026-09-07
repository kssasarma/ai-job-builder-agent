-- Audit trail for every AI-influenced decision (score, match, generated content),
-- so a candidate or recruiter can ask "why" and it's answerable, not just logged
-- to stdout. Backs the AI Decision Ledger / explainability panel.
CREATE TABLE ai_decision_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    decision_type VARCHAR(50) NOT NULL,
    reference_id UUID NOT NULL,
    model VARCHAR(100) NOT NULL,
    prompt_version VARCHAR(50) NOT NULL,
    summary TEXT NOT NULL,
    subject_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_decision_logs_reference ON ai_decision_logs(reference_id, decision_type);
CREATE INDEX idx_ai_decision_logs_subject_user ON ai_decision_logs(subject_user_id);
