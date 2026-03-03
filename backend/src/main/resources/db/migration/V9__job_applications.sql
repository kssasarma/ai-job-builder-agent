CREATE TABLE job_applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id UUID NOT NULL REFERENCES candidate_profiles(id),
    job_posting_id UUID NOT NULL REFERENCES job_postings(id),
    status VARCHAR(20) NOT NULL DEFAULT 'APPLIED',
    applied_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(candidate_id, job_posting_id)
);
CREATE INDEX ON job_applications(job_posting_id);
CREATE INDEX ON job_applications(candidate_id);
