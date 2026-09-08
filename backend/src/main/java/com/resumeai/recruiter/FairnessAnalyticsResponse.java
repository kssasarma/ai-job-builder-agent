package com.resumeai.recruiter;

public record FairnessAnalyticsResponse(
        int sampleSize,
        Double minScore,
        Double maxScore,
        Double avgScore,
        Double medianScore,
        Double stdDeviation,
        boolean lowDifferentiation,
        String note
) {}
