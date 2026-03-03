-- Profile Suggestions Table
CREATE TABLE profile_suggestions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resume_id UUID UNIQUE NOT NULL REFERENCES resumes(id) ON DELETE CASCADE,
    candidate_id UUID NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
    suggested_headline VARCHAR(255),
    suggested_skills JSONB,
    suggested_linkedin_url VARCHAR(255),
    status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
