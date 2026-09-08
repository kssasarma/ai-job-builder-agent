package com.resumeai.recruiter;

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

/**
 * Collaborative hiring rooms: a threaded comment feed on one CandidateMatch so
 * interviewers leave structured feedback in one place instead of an email chain.
 */
@RestController
@RequestMapping("/api/recruiter/matches/{candidateMatchId}/comments")
public class MatchCommentController {

    private final MatchCommentRepository matchCommentRepository;
    private final CandidateMatchRepository candidateMatchRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;

    public MatchCommentController(MatchCommentRepository matchCommentRepository,
                                   CandidateMatchRepository candidateMatchRepository,
                                   RecruiterProfileRepository recruiterProfileRepository) {
        this.matchCommentRepository = matchCommentRepository;
        this.candidateMatchRepository = candidateMatchRepository;
        this.recruiterProfileRepository = recruiterProfileRepository;
    }

    private boolean ownsMatch(UUID candidateMatchId, UUID userId) {
        RecruiterProfile recruiter = recruiterProfileRepository.findByUserId(userId).orElse(null);
        if (recruiter == null) return false;
        CandidateMatch match = candidateMatchRepository.findById(candidateMatchId).orElse(null);
        return match != null && match.getJobPosting().getRecruiter().getId().equals(recruiter.getId());
    }

    @GetMapping
    public ResponseEntity<?> list(@PathVariable UUID candidateMatchId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (!ownsMatch(candidateMatchId, userDetails.getUser().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(matchCommentRepository.findByCandidateMatchIdOrderByCreatedAtAsc(candidateMatchId).stream()
                .map(MatchCommentDto::fromEntity)
                .toList());
    }

    @PostMapping
    public ResponseEntity<?> add(@PathVariable UUID candidateMatchId, @RequestBody MatchCommentCreateRequest request,
                                  @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (!ownsMatch(candidateMatchId, userDetails.getUser().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (request.body() == null || request.body().isBlank()) {
            return ResponseEntity.badRequest().body("Comment body is required");
        }

        CandidateMatch match = candidateMatchRepository.findById(candidateMatchId).orElseThrow();
        MatchComment comment = new MatchComment();
        comment.setCandidateMatch(match);
        comment.setAuthor(userDetails.getUser());
        comment.setBody(request.body());

        return ResponseEntity.ok(MatchCommentDto.fromEntity(matchCommentRepository.save(comment)));
    }
}
