package com.resumeai.recruiter;

import java.util.List;

public record CandidateMatchResultDto(
        int matchScore,
        String matchReasoning,
        List<String> topMatchingSkills,
        List<String> identifiedGaps,
        String experienceSummary
) {
}
