package com.resumeai.candidate;

import java.util.List;

public record JobCompatibilityResponse(
        int compatibilityScore,
        String recommendation,
        String reasoning,
        List<String> matchingStrengths,
        List<String> missingRequirements
) {}
