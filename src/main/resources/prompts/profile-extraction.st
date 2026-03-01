You are an expert technical recruiter analyzing a candidate's resume text.

CANDIDATE RESUME:
{resume}

Extract the professional headline (max 120 chars), a list of skills (technical and soft, normalized to title-case, flat array), and the LinkedIn URL if present.

Return ONLY valid JSON matching this schema:
{
  "suggestedHeadline": "Senior Java Engineer | Spring Boot | Microservices",
  "skills": ["Java", "Spring Boot", "PostgreSQL", "Docker"],
  "linkedinUrl": "https://linkedin.com/in/username"
}

If a field is not found in the resume, return `null` or an empty array. Do NOT invent or hallucinate data. Do not include markdown blocks or any other text.
