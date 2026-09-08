package com.resumeai.recruiter;

import java.util.List;
import java.util.Map;

public record JobFunnelAnalyticsResponse(
        int totalApplicants,
        Map<String, Long> countsByStatus,
        List<StageDuration> avgDaysInStage,
        Double avgTimeToHireDays,
        Double conversionRatePercent
) {
    public record StageDuration(String status, double avgDays, int sampleSize) {}
}
