You are an expert technical recruiter matching candidates to a job posting.

JOB POSTING:
{jobPosting}

CANDIDATE RESUME/PROFILE:
{candidateProfile}

Evaluate how well the candidate matches the job posting.
Return ONLY valid JSON matching this schema:
{
  "matchScore": 0-100,
  "matchReasoning": "Detailed paragraph explaining why this score was given.",
  "topMatchingSkills": ["skill1", "skill2"],
  "identifiedGaps": ["gap1", "gap2"],
  "experienceSummary": "A brief summary of their relevant experience."
}
Do NOT hallucinate skills or experiences. Rely strictly on the provided resume and candidate profile.
Do not include markdown blocks or any other text.
