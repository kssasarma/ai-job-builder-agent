package com.resumeai.candidate;

import com.resumeai.recruiter.JobPostingDto;
import com.resumeai.recruiter.JobPostingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/candidate/jobs")
public class CandidateJobController {

    private final JobPostingRepository jobPostingRepository;

    public CandidateJobController(JobPostingRepository jobPostingRepository) {
        this.jobPostingRepository = jobPostingRepository;
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
                .filter(job -> "OPEN".equals(job.getStatus()))
                .map(job -> ResponseEntity.ok(JobPostingDto.fromEntity(job)))
                .orElse(ResponseEntity.notFound().build());
    }
}
