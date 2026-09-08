package com.resumeai.recruiter;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.resumeai.auth.CustomUserDetails;
import com.resumeai.candidate.CandidateProfile;
import com.resumeai.candidate.CandidateProfileRepository;

/** Trust-weighted referrals: a candidate refers their network into an open role. */
@RestController
public class ReferralController {

    private final ReferralRepository referralRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final JobPostingRepository jobPostingRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;

    public ReferralController(ReferralRepository referralRepository, CandidateProfileRepository candidateProfileRepository,
                               JobPostingRepository jobPostingRepository, RecruiterProfileRepository recruiterProfileRepository) {
        this.referralRepository = referralRepository;
        this.candidateProfileRepository = candidateProfileRepository;
        this.jobPostingRepository = jobPostingRepository;
        this.recruiterProfileRepository = recruiterProfileRepository;
    }

    /** Trust score: % of this candidate's past referrals with a terminal outcome that were SELECTED. Null with no terminal outcomes yet. */
    private Double trustScore(UUID referrerCandidateId) {
        long selected = referralRepository.countByReferrerIdAndStatus(referrerCandidateId, "SELECTED");
        long rejected = referralRepository.countByReferrerIdAndStatus(referrerCandidateId, "REJECTED");
        long terminal = selected + rejected;
        return terminal == 0 ? null : (selected * 100.0) / terminal;
    }

    private ReferralDto toDto(Referral referral) {
        CandidateProfile referrer = referral.getReferrer();
        return new ReferralDto(
                referral.getId(),
                referral.getJobPosting().getId(),
                referral.getJobPosting().getTitle(),
                referral.getReferredName(),
                referral.getReferredEmail(),
                referral.getStatus(),
                referral.getCreatedAt(),
                referrer.getId(),
                referrer.getUser() != null ? referrer.getUser().getName() : null,
                trustScore(referrer.getId())
        );
    }

    @PostMapping("/api/candidate/referrals")
    public ResponseEntity<?> refer(@RequestBody ReferralCreateRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (request.referredName() == null || request.referredName().isBlank()
                || request.referredEmail() == null || request.referredEmail().isBlank()) {
            return ResponseEntity.badRequest().body("referredName and referredEmail are required");
        }
        CandidateProfile candidate = candidateProfileRepository.findByUserId(userDetails.getUser().getId()).orElseThrow();
        JobPosting job = jobPostingRepository.findById(request.jobPostingId())
                .filter(j -> "OPEN".equals(j.getStatus()) || "ACTIVE".equals(j.getStatus()))
                .orElse(null);
        if (job == null) {
            return ResponseEntity.badRequest().body("Job not found or not open");
        }

        Referral referral = new Referral();
        referral.setReferrer(candidate);
        referral.setJobPosting(job);
        referral.setReferredName(request.referredName());
        referral.setReferredEmail(request.referredEmail());

        return ResponseEntity.ok(toDto(referralRepository.save(referral)));
    }

    @GetMapping("/api/candidate/referrals")
    public ResponseEntity<?> myReferrals(@AuthenticationPrincipal CustomUserDetails userDetails) {
        CandidateProfile candidate = candidateProfileRepository.findByUserId(userDetails.getUser().getId()).orElseThrow();
        return ResponseEntity.ok(referralRepository.findByReferrerIdOrderByCreatedAtDesc(candidate.getId()).stream()
                .map(this::toDto)
                .toList());
    }

    @GetMapping("/api/recruiter/jobs/{id}/referrals")
    public ResponseEntity<?> jobReferrals(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        RecruiterProfile recruiter = recruiterProfileRepository.findByUserId(userDetails.getUser().getId()).orElseThrow();
        JobPosting job = jobPostingRepository.findById(id).orElseThrow();
        if (!job.getRecruiter().getId().equals(recruiter.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(referralRepository.findByJobPostingIdOrderByCreatedAtDesc(id).stream()
                .map(this::toDto)
                .toList());
    }

    @PatchMapping("/api/recruiter/referrals/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable UUID id, @RequestBody ReferralStatusUpdate update,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        RecruiterProfile recruiter = recruiterProfileRepository.findByUserId(userDetails.getUser().getId()).orElseThrow();
        Referral referral = referralRepository.findById(id).orElse(null);
        if (referral == null || !referral.getJobPosting().getRecruiter().getId().equals(recruiter.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        java.util.Set<String> valid = java.util.Set.of("REFERRED", "CONTACTED", "INTERVIEW", "REJECTED", "SELECTED");
        if (update.status() == null || !valid.contains(update.status())) {
            return ResponseEntity.badRequest().body("Invalid status");
        }
        referral.setStatus(update.status());
        return ResponseEntity.ok(toDto(referralRepository.save(referral)));
    }
}
