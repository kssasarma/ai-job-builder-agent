package com.resumeai.candidate;

import com.resumeai.auth.CustomUserDetails;
import com.resumeai.recruiter.JobApplicationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/candidate/applications")
public class CandidateApplicationController {

    private final JobApplicationRepository jobApplicationRepository;
    private final CandidateProfileRepository candidateProfileRepository;

    public CandidateApplicationController(JobApplicationRepository jobApplicationRepository,
                                           CandidateProfileRepository candidateProfileRepository) {
        this.jobApplicationRepository = jobApplicationRepository;
        this.candidateProfileRepository = candidateProfileRepository;
    }

    public record ApplicationSummary(
            UUID applicationId,
            String status,
            LocalDateTime appliedAt,
            LocalDateTime updatedAt,
            UUID jobPostingId,
            String jobTitle,
            String company,
            String location,
            String jobType,
            String salaryRange
    ) {}

    @GetMapping
    public ResponseEntity<List<ApplicationSummary>> getMyApplications(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        CandidateProfile candidate = candidateProfileRepository
                .findByUserId(userDetails.getUser().getId())
                .orElseThrow(() -> new IllegalArgumentException("Candidate profile not found"));

        List<ApplicationSummary> result = jobApplicationRepository
                .findByCandidateId(candidate.getId())
                .stream()
                .sorted((a, b) -> b.getAppliedAt().compareTo(a.getAppliedAt()))
                .map(app -> new ApplicationSummary(
                        app.getId(),
                        app.getStatus(),
                        app.getAppliedAt(),
                        app.getUpdatedAt(),
                        app.getJobPosting().getId(),
                        app.getJobPosting().getTitle(),
                        app.getJobPosting().getCompany(),
                        app.getJobPosting().getLocation(),
                        app.getJobPosting().getJobType(),
                        app.getJobPosting().getSalaryRange()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}
