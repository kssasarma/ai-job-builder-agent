package com.resumeai.candidate;

import java.util.List;

public record ATSScoreResponse(
        int overallScore,
        List<CategoryScore> categories,
        List<Improvement> improvements,
        List<String> detectedSkills,
        String experienceSummary,
        String educationSummary
) {
    public record CategoryScore(String name, int score, String feedback) {}
    public record Improvement(String text, String priority) {}
}
