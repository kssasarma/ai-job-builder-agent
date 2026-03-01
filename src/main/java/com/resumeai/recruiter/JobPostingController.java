package com.resumeai.recruiter;

import com.resumeai.auth.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/recruiter/jobs")
public class JobPostingController {

    private final JobPostingRepository jobPostingRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;

    private final com.resumeai.ai.AiService aiService;
    private final CandidateMatchRepository candidateMatchRepository;

    public JobPostingController(JobPostingRepository jobPostingRepository, RecruiterProfileRepository recruiterProfileRepository, com.resumeai.ai.AiService aiService, CandidateMatchRepository candidateMatchRepository) {
        this.jobPostingRepository = jobPostingRepository;
        this.recruiterProfileRepository = recruiterProfileRepository;
        this.aiService = aiService;
        this.candidateMatchRepository = candidateMatchRepository;
    }

    private RecruiterProfile getRecruiterProfile(UUID userId) {
        return recruiterProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Recruiter profile not found"));
    }

    @PostMapping
    public ResponseEntity<JobPosting> createJobPosting(
            @Valid @RequestBody JobPostingRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        RecruiterProfile recruiter = getRecruiterProfile(userDetails.getUser().getId());

        JobPosting job = new JobPosting();
        job.setRecruiter(recruiter);
        updateJobFromRequest(job, request);
        if (request.status() != null) job.setStatus(request.status());

        return ResponseEntity.ok(jobPostingRepository.save(job));
    }

    @GetMapping
    public ResponseEntity<Page<JobPosting>> getJobPostings(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Pageable pageable) {
        RecruiterProfile recruiter = getRecruiterProfile(userDetails.getUser().getId());
        return ResponseEntity.ok(jobPostingRepository.findByRecruiterId(recruiter.getId(), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobPosting> getJobPosting(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        RecruiterProfile recruiter = getRecruiterProfile(userDetails.getUser().getId());
        JobPosting job = jobPostingRepository.findById(id).orElseThrow();
        if (!job.getRecruiter().getId().equals(recruiter.getId())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(job);
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobPosting> updateJobPosting(
            @PathVariable UUID id,
            @Valid @RequestBody JobPostingRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        RecruiterProfile recruiter = getRecruiterProfile(userDetails.getUser().getId());
        JobPosting job = jobPostingRepository.findById(id).orElseThrow();
        if (!job.getRecruiter().getId().equals(recruiter.getId())) {
            return ResponseEntity.status(403).build();
        }

        updateJobFromRequest(job, request);
        if (request.status() != null) job.setStatus(request.status());

        return ResponseEntity.ok(jobPostingRepository.save(job));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<JobPosting> updateJobStatus(
            @PathVariable UUID id,
            @RequestBody JobPostingStatusUpdate statusUpdate,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        RecruiterProfile recruiter = getRecruiterProfile(userDetails.getUser().getId());
        JobPosting job = jobPostingRepository.findById(id).orElseThrow();
        if (!job.getRecruiter().getId().equals(recruiter.getId())) {
            return ResponseEntity.status(403).build();
        }

        job.setStatus(statusUpdate.status());
        return ResponseEntity.ok(jobPostingRepository.save(job));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJobPosting(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        RecruiterProfile recruiter = getRecruiterProfile(userDetails.getUser().getId());
        JobPosting job = jobPostingRepository.findById(id).orElseThrow();
        if (!job.getRecruiter().getId().equals(recruiter.getId())) {
            return ResponseEntity.status(403).build();
        }

        job.setStatus("CLOSED"); // Soft delete
        jobPostingRepository.save(job);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/find-candidates")
    public ResponseEntity<?> findCandidates(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        RecruiterProfile recruiter = getRecruiterProfile(userDetails.getUser().getId());
        JobPosting job = jobPostingRepository.findById(id).orElseThrow();
        if (!job.getRecruiter().getId().equals(recruiter.getId())) {
            return ResponseEntity.status(403).build();
        }

        try {
            aiService.matchCandidatesAsync(id);
            return ResponseEntity.accepted().body(java.util.Map.of("message", "Matching started"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}/matches")
    public ResponseEntity<?> getMatches(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        RecruiterProfile recruiter = getRecruiterProfile(userDetails.getUser().getId());
        JobPosting job = jobPostingRepository.findById(id).orElseThrow();
        if (!job.getRecruiter().getId().equals(recruiter.getId())) {
            return ResponseEntity.status(403).build();
        }

        String status = aiService.getMatchingStatus(id);
        if ("COMPLETED".equals(status)) {
            List<CandidateMatch> matches = candidateMatchRepository.findByJobPostingIdOrderByMatchScoreDesc(id);
            return ResponseEntity.ok(java.util.Map.of("status", status, "matches", matches));
        }
        return ResponseEntity.ok(java.util.Map.of("status", status));
    }

    private void updateJobFromRequest(JobPosting job, JobPostingRequest request) {
        job.setTitle(request.title());
        job.setCompany(request.company());
        job.setDescription(request.description());
        job.setRequiredSkills(request.requiredSkills());
        job.setExperienceMin(request.experienceMin());
        job.setExperienceMax(request.experienceMax());
        job.setLocation(request.location());
        job.setSalaryRange(request.salaryRange());
        job.setJobType(request.jobType());
    }
}
