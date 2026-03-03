package com.resumeai.candidate;

import java.util.List;

public record CompatibilityResponse(
        int matchScore,
        List<String> matchingSkills,
        List<String> missingCriticalSkills,
        List<String> partiallyMatching,
        int experienceGapYears,
        boolean educationMatch,
        String compatibilityTier,
        String detailedReasoning,
        java.util.UUID historyId // Optional, to return the saved history ID
) {
}
