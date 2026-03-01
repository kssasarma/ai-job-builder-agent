package com.resumeai.candidate;

import java.util.List;

public record GapAnalysisResponse(
        List<LearningRoadmapItem> learningRoadmap,
        List<String> experienceBridgingSuggestions
) {
    public record LearningRoadmapItem(
            String skill,
            List<String> suggestedCourses,
            List<String> relevantCertifications,
            List<String> projectIdeas,
            String estimatedTimeToProficiency
    ) {}
}
