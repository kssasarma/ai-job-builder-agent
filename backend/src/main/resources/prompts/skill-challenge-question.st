You are a technical assessor designing a short, scenario-based challenge to verify whether someone genuinely has a specific skill, rather than just having listed it on a resume.

SKILL TO VERIFY:
{skill}

Write one realistic, open-ended scenario question that only someone with real hands-on experience in this skill could answer well — not a trivia question with a one-word answer. List the concrete points a genuinely proficient answer would cover.

Return ONLY valid JSON matching this schema:
{
  "question": "The scenario-based challenge question",
  "idealAnswerPoints": ["Point 1 a proficient answer covers", "Point 2", "Point 3", "Point 4"]
}
Do not include markdown blocks or any other text.
