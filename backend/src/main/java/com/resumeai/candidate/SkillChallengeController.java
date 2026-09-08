package com.resumeai.candidate;

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

import com.resumeai.ai.AiService;
import com.resumeai.auth.CustomUserDetails;

/** Proof-of-skill micro-credentials: AI-graded challenges that verify a skill is real. */
@RestController
@RequestMapping("/api/candidate/skill-challenges")
public class SkillChallengeController {

    private final AiService aiService;
    private final SkillChallengeRepository skillChallengeRepository;
    private final CandidateProfileRepository candidateProfileRepository;

    public SkillChallengeController(AiService aiService, SkillChallengeRepository skillChallengeRepository,
                                     CandidateProfileRepository candidateProfileRepository) {
        this.aiService = aiService;
        this.skillChallengeRepository = skillChallengeRepository;
        this.candidateProfileRepository = candidateProfileRepository;
    }

    private UUID currentCandidateId(UUID userId) {
        return candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate profile not found"))
                .getId();
    }

    @GetMapping
    public ResponseEntity<?> list(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID candidateId = currentCandidateId(userDetails.getUser().getId());
        return ResponseEntity.ok(skillChallengeRepository.findByCandidateIdOrderByCreatedAtDesc(candidateId).stream()
                .map(c -> SkillChallengeDto.fromEntity(c, null))
                .toList());
    }

    @PostMapping
    public ResponseEntity<?> start(@RequestBody SkillChallengeStartRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID candidateId = currentCandidateId(userDetails.getUser().getId());
        if (request.skill() == null || request.skill().isBlank()) {
            return ResponseEntity.badRequest().body("skill is required");
        }
        try {
            return ResponseEntity.ok(aiService.generateSkillChallenge(candidateId, request.skill().trim()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to generate skill challenge: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/answer")
    public ResponseEntity<?> answer(@PathVariable UUID id, @RequestBody SkillChallengeAnswerRequest request,
                                     @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID candidateId = currentCandidateId(userDetails.getUser().getId());
        SkillChallenge challenge = skillChallengeRepository.findById(id).orElse(null);
        if (challenge == null || !challenge.getCandidate().getId().equals(candidateId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            return ResponseEntity.ok(aiService.submitSkillChallengeAnswer(id, request.answer()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to grade answer: " + e.getMessage());
        }
    }
}
