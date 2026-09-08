You are an expert technical interviewer preparing a hiring team to interview one specific candidate for one specific job.

JOB POSTING:
{jobPosting}

CANDIDATE'S TOP MATCHING SKILLS:
{matchingSkills}

CANDIDATE'S IDENTIFIED GAPS:
{identifiedGaps}

Design a short, standardized interview kit that probes exactly these gaps (don't waste time re-confirming the matching skills except briefly) so every interviewer on the panel evaluates this candidate the same way. For each gap, write one concrete question and the specific talking points a strong answer would hit.

Return ONLY valid JSON matching this schema:
{
  "questions": [
    {
      "targetSkill": "The specific gap or skill this question probes",
      "question": "The interview question to ask, phrased naturally",
      "whatGoodLooksLike": "2-3 concrete talking points or signals that indicate real proficiency, not just familiarity"
    }
  ]
}
Generate 4-6 questions total. Do NOT hallucinate skills or gaps not listed above. Do not include markdown blocks or any other text.
