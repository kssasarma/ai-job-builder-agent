package com.resumeai.candidate;

import java.util.List;

public record CompatibilityAnalysisDto(
        int matchScore,
        List<String> matchingSkills,
        List<String> missingCriticalSkills,
        List<String> partiallyMatching,
        int experienceGapYears,
        boolean educationMatch,
        String detailedReasoning
) {
}
