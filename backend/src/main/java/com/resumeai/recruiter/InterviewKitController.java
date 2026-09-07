package com.resumeai.recruiter;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.resumeai.ai.AiService;
import com.resumeai.auth.CustomUserDetails;

@RestController
@RequestMapping("/api/recruiter/matches/{candidateMatchId}/interview-kit")
public class InterviewKitController {

    private final AiService aiService;
    private final CandidateMatchRepository candidateMatchRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;

    public InterviewKitController(AiService aiService, CandidateMatchRepository candidateMatchRepository,
                                   RecruiterProfileRepository recruiterProfileRepository) {
        this.aiService = aiService;
        this.candidateMatchRepository = candidateMatchRepository;
        this.recruiterProfileRepository = recruiterProfileRepository;
    }

    private boolean ownsMatch(UUID candidateMatchId, UUID userId) {
        RecruiterProfile recruiter = recruiterProfileRepository.findByUserId(userId).orElse(null);
        if (recruiter == null) return false;
        CandidateMatch match = candidateMatchRepository.findById(candidateMatchId).orElse(null);
        return match != null && match.getJobPosting().getRecruiter().getId().equals(recruiter.getId());
    }

    @PostMapping
    public ResponseEntity<?> generate(@PathVariable UUID candidateMatchId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (!ownsMatch(candidateMatchId, userDetails.getUser().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            return ResponseEntity.ok(aiService.generateInterviewKit(candidateMatchId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> get(@PathVariable UUID candidateMatchId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (!ownsMatch(candidateMatchId, userDetails.getUser().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            return ResponseEntity.ok(aiService.getInterviewKit(candidateMatchId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
