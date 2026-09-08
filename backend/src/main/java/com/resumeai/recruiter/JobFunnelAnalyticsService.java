package com.resumeai.recruiter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

/**
 * Funnel & time-to-hire analytics for one job posting: counts by status, average
 * time spent in each stage (derived from {@link JobApplicationStatusHistory}, since
 * a JobApplication row alone only ever shows its *current* status), and overall
 * time-to-hire for applications that reached SELECTED.
 */
@Service
public class JobFunnelAnalyticsService {

    private final JobApplicationRepository jobApplicationRepository;
    private final JobApplicationStatusHistoryRepository statusHistoryRepository;

    public JobFunnelAnalyticsService(JobApplicationRepository jobApplicationRepository,
                                      JobApplicationStatusHistoryRepository statusHistoryRepository) {
        this.jobApplicationRepository = jobApplicationRepository;
        this.statusHistoryRepository = statusHistoryRepository;
    }

    public JobFunnelAnalyticsResponse computeAnalytics(UUID jobPostingId) {
        List<JobApplication> applications = jobApplicationRepository.findByJobPostingId(jobPostingId);

        Map<String, Long> countsByStatus = new LinkedHashMap<>();
        for (String status : List.of("APPLIED", "CONTACTED", "INTERVIEW", "REJECTED", "SELECTED")) {
            countsByStatus.put(status, 0L);
        }
        for (JobApplication app : applications) {
            countsByStatus.merge(app.getStatus(), 1L, Long::sum);
        }

        // Stage duration: for each application's ordered history, the time between
        // entering a status and leaving it counts toward that status's total.
        Map<String, List<Double>> daysByStage = new LinkedHashMap<>();
        List<Double> timeToHireDays = new ArrayList<>();

        List<JobApplicationStatusHistory> history = statusHistoryRepository
                .findByJobApplicationJobPostingIdOrderByChangedAtAsc(jobPostingId);
        Map<UUID, List<JobApplicationStatusHistory>> byApplication = new LinkedHashMap<>();
        for (JobApplicationStatusHistory entry : history) {
            byApplication.computeIfAbsent(entry.getJobApplication().getId(), k -> new ArrayList<>()).add(entry);
        }

        for (List<JobApplicationStatusHistory> entries : byApplication.values()) {
            for (int i = 0; i < entries.size() - 1; i++) {
                JobApplicationStatusHistory current = entries.get(i);
                JobApplicationStatusHistory next = entries.get(i + 1);
                double days = Duration.between(current.getChangedAt(), next.getChangedAt()).toMinutes() / (60.0 * 24.0);
                daysByStage.computeIfAbsent(current.getStatus(), k -> new ArrayList<>()).add(days);
            }

            JobApplicationStatusHistory first = entries.get(0);
            JobApplicationStatusHistory last = entries.get(entries.size() - 1);
            if ("SELECTED".equals(last.getStatus())) {
                LocalDateTime appliedAt = first.getChangedAt();
                double days = Duration.between(appliedAt, last.getChangedAt()).toMinutes() / (60.0 * 24.0);
                timeToHireDays.add(days);
            }
        }

        List<JobFunnelAnalyticsResponse.StageDuration> avgDaysInStage = daysByStage.entrySet().stream()
                .map(e -> new JobFunnelAnalyticsResponse.StageDuration(
                        e.getKey(),
                        e.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0),
                        e.getValue().size()))
                .toList();

        Double avgTimeToHire = timeToHireDays.isEmpty() ? null
                : timeToHireDays.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        long selected = countsByStatus.getOrDefault("SELECTED", 0L);
        Double conversionRate = applications.isEmpty() ? null : (selected * 100.0) / applications.size();

        return new JobFunnelAnalyticsResponse(applications.size(), countsByStatus, avgDaysInStage, avgTimeToHire, conversionRate);
    }
}
