package com.resumeai.candidate;

public record TrajectorySimulationResponse(
        int currentAverageScore,
        String topGapSkill,
        String gapFrequency,
        int projectedAverageScore,
        String explanation
) {}
