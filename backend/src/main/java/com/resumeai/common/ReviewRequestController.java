package com.resumeai.common;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.resumeai.auth.CustomUserDetails;

/**
 * Backs the explainability panel's "request human review" action, and the AI decision
 * ledger read by that same panel — the two things a candidate or recruiter needs to
 * trust (or contest) an AI score instead of just seeing a number.
 */
@RestController
@RequestMapping("/api/ai")
public class ReviewRequestController {

    private static final Set<String> VALID_SUBJECT_TYPES = Set.of(
            "SCORING", "COMPATIBILITY", "GAP_ANALYSIS", "TAILORING", "MATCHING", "INTERVIEW_KIT");

    private final ReviewRequestRepository reviewRequestRepository;
    private final com.resumeai.ai.AiDecisionLogRepository aiDecisionLogRepository;
    private final com.resumeai.candidate.ResumeRepository resumeRepository;
    private final com.resumeai.candidate.TailoringHistoryRepository tailoringHistoryRepository;
    private final com.resumeai.recruiter.JobPostingRepository jobPostingRepository;
    private final com.resumeai.recruiter.RecruiterProfileRepository recruiterProfileRepository;
    private final com.resumeai.recruiter.CandidateMatchRepository candidateMatchRepository;

    public ReviewRequestController(ReviewRequestRepository reviewRequestRepository,
                                    com.resumeai.ai.AiDecisionLogRepository aiDecisionLogRepository,
                                    com.resumeai.candidate.ResumeRepository resumeRepository,
                                    com.resumeai.candidate.TailoringHistoryRepository tailoringHistoryRepository,
                                    com.resumeai.recruiter.JobPostingRepository jobPostingRepository,
                                    com.resumeai.recruiter.RecruiterProfileRepository recruiterProfileRepository,
                                    com.resumeai.recruiter.CandidateMatchRepository candidateMatchRepository) {
        this.reviewRequestRepository = reviewRequestRepository;
        this.aiDecisionLogRepository = aiDecisionLogRepository;
        this.resumeRepository = resumeRepository;
        this.tailoringHistoryRepository = tailoringHistoryRepository;
        this.jobPostingRepository = jobPostingRepository;
        this.recruiterProfileRepository = recruiterProfileRepository;
        this.candidateMatchRepository = candidateMatchRepository;
    }

    @PostMapping("/review-requests")
    public ResponseEntity<?> requestReview(@RequestBody ReviewRequestCreateRequest request,
                                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (request.subjectType() == null || !VALID_SUBJECT_TYPES.contains(request.subjectType()) || request.subjectId() == null) {
            return ResponseEntity.badRequest().body("subjectType must be one of " + VALID_SUBJECT_TYPES + " and subjectId is required");
        }
        ReviewRequest reviewRequest = new ReviewRequest();
        reviewRequest.setRequestedByUserId(userDetails.getUser().getId());
        reviewRequest.setSubjectType(request.subjectType());
        reviewRequest.setSubjectId(request.subjectId());
        reviewRequest.setReason(request.reason());
        return ResponseEntity.ok(reviewRequestRepository.save(reviewRequest));
    }

    /**
     * Replays the "why" for one AI decision. {@code decisionType} decides how ownership
     * is checked, since {@code referenceId} means something different per type (a resume
     * for SCORING/COMPATIBILITY, a tailoring-history row for GAP_ANALYSIS/TAILORING, a job
     * posting for MATCHING, a candidate match for INTERVIEW_KIT) — this is what stops one
     * user from reading another user's AI reasoning by guessing a UUID.
     */
    @GetMapping("/decisions/{decisionType}/{referenceId}")
    public ResponseEntity<?> getDecisionLog(@PathVariable String decisionType, @PathVariable UUID referenceId,
                                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (!VALID_SUBJECT_TYPES.contains(decisionType)) {
            return ResponseEntity.badRequest().body("Unknown decisionType: " + decisionType);
        }
        UUID userId = userDetails.getUser().getId();
        boolean authorized = switch (decisionType) {
            case "SCORING", "COMPATIBILITY" -> ownsResume(referenceId, userId);
            case "GAP_ANALYSIS", "TAILORING" -> ownsTailoringHistory(referenceId, userId);
            case "MATCHING" -> ownsJobPosting(referenceId, userId);
            case "INTERVIEW_KIT" -> ownsCandidateMatch(referenceId, userId);
            default -> false;
        };
        if (!authorized) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<com.resumeai.ai.AiDecisionLog> logs = aiDecisionLogRepository.findByReferenceIdOrderByCreatedAtDesc(referenceId).stream()
                .filter(l -> decisionType.equals(l.getDecisionType()))
                .toList();
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/review-requests/mine")
    public ResponseEntity<?> myReviewRequests(@AuthenticationPrincipal CustomUserDetails userDetails,
                                               @RequestParam(required = false) String status) {
        var requests = reviewRequestRepository.findByRequestedByUserIdOrderByCreatedAtDesc(userDetails.getUser().getId());
        if (status != null && !status.isBlank()) {
            requests = requests.stream().filter(r -> status.equalsIgnoreCase(r.getStatus())).toList();
        }
        return ResponseEntity.ok(requests);
    }

    private boolean ownsResume(UUID resumeId, UUID userId) {
        return resumeRepository.findById(resumeId)
                .map(r -> r.getCandidate().getUser().getId().equals(userId))
                .orElse(false);
    }

    private boolean ownsTailoringHistory(UUID historyId, UUID userId) {
        return tailoringHistoryRepository.findById(historyId)
                .map(h -> h.getResume().getCandidate().getUser().getId().equals(userId))
                .orElse(false);
    }

    private boolean ownsJobPosting(UUID jobPostingId, UUID userId) {
        var recruiter = recruiterProfileRepository.findByUserId(userId).orElse(null);
        if (recruiter == null) return false;
        return jobPostingRepository.findById(jobPostingId)
                .map(j -> j.getRecruiter().getId().equals(recruiter.getId()))
                .orElse(false);
    }

    private boolean ownsCandidateMatch(UUID candidateMatchId, UUID userId) {
        var recruiter = recruiterProfileRepository.findByUserId(userId).orElse(null);
        if (recruiter == null) return false;
        return candidateMatchRepository.findById(candidateMatchId)
                .map(m -> m.getJobPosting().getRecruiter().getId().equals(recruiter.getId()))
                .orElse(false);
    }
}
