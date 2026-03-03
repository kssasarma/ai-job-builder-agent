package com.resumeai.candidate;

import com.resumeai.auth.CustomUserDetails;
import com.resumeai.recruiter.JobApplication;
import com.resumeai.recruiter.JobApplicationDto;
import com.resumeai.recruiter.JobApplicationRepository;
import com.resumeai.recruiter.JobPosting;
import com.resumeai.recruiter.JobPostingDto;
import com.resumeai.recruiter.JobPostingRepository;
import com.resumeai.ai.AiService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/candidate/jobs")
public class CandidateJobController {

    private final JobPostingRepository jobPostingRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final AiService aiService;

    public CandidateJobController(JobPostingRepository jobPostingRepository,
                                   JobApplicationRepository jobApplicationRepository,
                                   CandidateProfileRepository candidateProfileRepository,
                                   AiService aiService) {
        this.jobPostingRepository = jobPostingRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.candidateProfileRepository = candidateProfileRepository;
        this.aiService = aiService;
    }

    private CandidateProfile getCandidateProfile(UUID userId) {
        return candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate profile not found"));
    }

    @GetMapping
    public ResponseEntity<Page<JobPostingDto>> findJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String skills,
            Pageable pageable) {

        String keywordParam = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        String locationParam = (location != null && !location.isBlank()) ? location.trim() : null;
        String skillsText = (skills != null && !skills.isBlank())
                ? Arrays.stream(skills.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.joining(","))
                : null;

        Page<JobPostingDto> jobs = jobPostingRepository
                .findOpenJobs(keywordParam, locationParam, skillsText, pageable)
                .map(JobPostingDto::fromEntity);

        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobPostingDto> getJob(@PathVariable UUID id) {
        return jobPostingRepository.findById(id)
                .filter(job -> "OPEN".equals(job.getStatus()) || "ACTIVE".equals(job.getStatus()))
                .map(job -> ResponseEntity.ok(JobPostingDto.fromEntity(job)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/applied")
    public ResponseEntity<List<UUID>> getAppliedJobIds(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        CandidateProfile candidate = getCandidateProfile(userDetails.getUser().getId());
        List<UUID> appliedIds = jobApplicationRepository.findByCandidateId(candidate.getId())
                .stream()
                .map(app -> app.getJobPosting().getId())
                .collect(Collectors.toList());
        return ResponseEntity.ok(appliedIds);
    }

    @PostMapping("/{id}/apply")
    public ResponseEntity<?> applyToJob(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        CandidateProfile candidate = getCandidateProfile(userDetails.getUser().getId());

        JobPosting job = jobPostingRepository.findById(id)
                .filter(j -> "OPEN".equals(j.getStatus()) || "ACTIVE".equals(j.getStatus()))
                .orElse(null);
        if (job == null) {
            return ResponseEntity.badRequest().body("Job not found or not open");
        }

        if (jobApplicationRepository.findByCandidateIdAndJobPostingId(candidate.getId(), id).isPresent()) {
            return ResponseEntity.status(409).body("Already applied to this job");
        }

        JobApplication application = new JobApplication();
        application.setCandidate(candidate);
        application.setJobPosting(job);
        application.setStatus("APPLIED");

        JobApplication saved = jobApplicationRepository.save(application);
        return ResponseEntity.status(201).body(JobApplicationDto.fromEntity(saved));
    }

    @GetMapping("/{id}/compatibility")
    public ResponseEntity<?> checkCompatibility(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        CandidateProfile candidate = getCandidateProfile(userDetails.getUser().getId());

        JobPosting job = jobPostingRepository.findById(id)
                .filter(j -> "OPEN".equals(j.getStatus()) || "ACTIVE".equals(j.getStatus()))
                .orElse(null);
        if (job == null) {
            return ResponseEntity.badRequest().body("Job not found or not open");
        }

        try {
            JobCompatibilityResponse result = aiService.checkJobCompatibility(job, candidate);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to check compatibility: " + e.getMessage());
        }
    }
}
