You are a career coach analyzing a candidate's history of compatibility checks against real job descriptions, to show them what closing their most common skill gap would actually be worth.

HISTORY OF COMPATIBILITY CHECKS (score and missing skills for each job the candidate checked against):
{historyLines}

Identify the single missing skill that appears most often across these checks. Estimate what the candidate's average match score would become if that gap were closed (reason from the checks where that skill was NOT missing, if any exist in the data, otherwise estimate conservatively based on how many checks list it as the only or primary gap).

Return ONLY valid JSON matching this schema:
{
  "currentAverageScore": 0-100,
  "topGapSkill": "The most frequently missing skill",
  "gapFrequency": "e.g. '4 of 6 checks'",
  "projectedAverageScore": 0-100,
  "explanation": "2-3 sentences on the reasoning and what closing this gap would mean"
}
Do not include markdown blocks or any other text.
