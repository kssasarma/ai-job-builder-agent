package com.resumeai.common;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.resumeai.auth.CustomUserDetails;
import com.resumeai.candidate.CandidateProfileRepository;
import com.resumeai.recruiter.JobApplication;
import com.resumeai.recruiter.JobApplicationRepository;
import com.resumeai.recruiter.RecruiterProfileRepository;

/** Two-sided reputation: candidates rate recruiters/employers, and vice versa. */
@RestController
public class RatingController {

    private final RatingRepository ratingRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;

    public RatingController(RatingRepository ratingRepository, JobApplicationRepository jobApplicationRepository,
                             CandidateProfileRepository candidateProfileRepository, RecruiterProfileRepository recruiterProfileRepository) {
        this.ratingRepository = ratingRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.candidateProfileRepository = candidateProfileRepository;
        this.recruiterProfileRepository = recruiterProfileRepository;
    }

    private ResponseEntity<?> validateScore(int score) {
        if (score < 1 || score > 5) {
            return ResponseEntity.badRequest().body("score must be between 1 and 5");
        }
        return null;
    }

    @PostMapping("/api/candidate/ratings")
    public ResponseEntity<?> rateRecruiter(@RequestBody RatingCreateRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        ResponseEntity<?> invalid = validateScore(request.score());
        if (invalid != null) return invalid;

        var candidate = candidateProfileRepository.findByUserId(userDetails.getUser().getId()).orElseThrow();
        JobApplication application = jobApplicationRepository.findById(request.jobApplicationId()).orElse(null);
        if (application == null || !application.getCandidate().getId().equals(candidate.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        UUID recruiterId = application.getJobPosting().getRecruiter().getId();
        if (ratingRepository.existsByRaterUserIdAndRateeTypeAndRateeIdAndJobApplicationId(
                userDetails.getUser().getId(), "RECRUITER", recruiterId, application.getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("You've already rated this employer for this application");
        }

        Rating rating = new Rating();
        rating.setRaterUserId(userDetails.getUser().getId());
        rating.setRateeType("RECRUITER");
        rating.setRateeId(recruiterId);
        rating.setJobApplicationId(application.getId());
        rating.setScore(request.score());
        rating.setComment(request.comment());
        return ResponseEntity.ok(ratingRepository.save(rating));
    }

    @PostMapping("/api/recruiter/ratings")
    public ResponseEntity<?> rateCandidate(@RequestBody RatingCreateRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        ResponseEntity<?> invalid = validateScore(request.score());
        if (invalid != null) return invalid;

        var recruiter = recruiterProfileRepository.findByUserId(userDetails.getUser().getId()).orElseThrow();
        JobApplication application = jobApplicationRepository.findById(request.jobApplicationId()).orElse(null);
        if (application == null || !application.getJobPosting().getRecruiter().getId().equals(recruiter.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        UUID candidateId = application.getCandidate().getId();
        if (ratingRepository.existsByRaterUserIdAndRateeTypeAndRateeIdAndJobApplicationId(
                userDetails.getUser().getId(), "CANDIDATE", candidateId, application.getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("You've already rated this candidate for this application");
        }

        Rating rating = new Rating();
        rating.setRaterUserId(userDetails.getUser().getId());
        rating.setRateeType("CANDIDATE");
        rating.setRateeId(candidateId);
        rating.setJobApplicationId(application.getId());
        rating.setScore(request.score());
        rating.setComment(request.comment());
        return ResponseEntity.ok(ratingRepository.save(rating));
    }

    @GetMapping("/api/recruiter/candidates/{id}/rating")
    public ResponseEntity<RatingSummaryDto> getCandidateRating(@PathVariable UUID id) {
        return ResponseEntity.ok(new RatingSummaryDto(
                ratingRepository.averageScore("CANDIDATE", id),
                ratingRepository.countByRateeTypeAndRateeId("CANDIDATE", id)));
    }

    @GetMapping("/api/recruiter/profile/rating")
    public ResponseEntity<RatingSummaryDto> getMyRecruiterRating(@AuthenticationPrincipal CustomUserDetails userDetails) {
        var recruiter = recruiterProfileRepository.findByUserId(userDetails.getUser().getId()).orElseThrow();
        return ResponseEntity.ok(new RatingSummaryDto(
                ratingRepository.averageScore("RECRUITER", recruiter.getId()),
                ratingRepository.countByRateeTypeAndRateeId("RECRUITER", recruiter.getId())));
    }
}
