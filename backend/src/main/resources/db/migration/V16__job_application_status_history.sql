-- Backs the funnel/time-to-hire analytics dashboard: without this, JobApplication
-- only ever shows the *current* status, so there's no way to compute time-in-stage.
-- One row is appended every time a recruiter changes an application's status
-- (including the initial APPLIED row, written at application time).
CREATE TABLE job_application_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_application_id UUID NOT NULL REFERENCES job_applications(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_job_application_status_history_app ON job_application_status_history(job_application_id, changed_at);

-- Backfill a history row for every application that already exists, so the
-- analytics endpoint has at least one data point per application.
INSERT INTO job_application_status_history (job_application_id, status, changed_at)
SELECT id, status, applied_at FROM job_applications;
