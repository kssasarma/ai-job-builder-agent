-- Trust-weighted referrals: a candidate refers someone from their network into an
-- open role; the referrer's trust score (computed on read from past outcomes) is
-- how many of their past referrals were SELECTED.
CREATE TABLE referrals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    referrer_candidate_id UUID NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
    job_posting_id UUID NOT NULL REFERENCES job_postings(id) ON DELETE CASCADE,
    referred_name VARCHAR(255) NOT NULL,
    referred_email VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'REFERRED', -- REFERRED, CONTACTED, INTERVIEW, REJECTED, SELECTED
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_referrals_referrer ON referrals(referrer_candidate_id);
CREATE INDEX idx_referrals_job ON referrals(job_posting_id);
