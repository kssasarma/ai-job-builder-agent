package com.resumeai.candidate;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.resumeai.ai.AiService;
import com.resumeai.auth.CustomUserDetails;

/** Career trajectory simulator: projects the score uplift from closing the candidate's most common gap. */
@RestController
@RequestMapping("/api/candidate/trajectory")
public class TrajectoryController {

    private final AiService aiService;
    private final CandidateProfileRepository candidateProfileRepository;

    public TrajectoryController(AiService aiService, CandidateProfileRepository candidateProfileRepository) {
        this.aiService = aiService;
        this.candidateProfileRepository = candidateProfileRepository;
    }

    @GetMapping
    public ResponseEntity<?> get(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID candidateId = candidateProfileRepository.findByUserId(userDetails.getUser().getId())
                .orElseThrow(() -> new IllegalArgumentException("Candidate profile not found"))
                .getId();
        try {
            return ResponseEntity.ok(aiService.generateTrajectorySimulation(candidateId));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to generate trajectory simulation: " + e.getMessage());
        }
    }
}
