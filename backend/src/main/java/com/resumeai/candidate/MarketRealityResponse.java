package com.resumeai.candidate;

public record MarketRealityResponse(
        String title,
        String location,
        int sampleSize,
        Integer minSalary,
        Integer medianSalary,
        Integer maxSalary
) {}
