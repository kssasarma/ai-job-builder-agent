package com.resumeai.candidate;

import com.resumeai.ai.AiService;
import com.resumeai.auth.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/candidate/tailoring")
public class TailoringController {

    private final AiService aiService;
    private final TailoringHistoryRepository tailoringHistoryRepository;
    private final LearningProgressRepository learningProgressRepository;
    private final CandidateProfileRepository candidateProfileRepository;

    public TailoringController(AiService aiService, TailoringHistoryRepository tailoringHistoryRepository,
                                LearningProgressRepository learningProgressRepository,
                                CandidateProfileRepository candidateProfileRepository) {
        this.aiService = aiService;
        this.tailoringHistoryRepository = tailoringHistoryRepository;
        this.learningProgressRepository = learningProgressRepository;
        this.candidateProfileRepository = candidateProfileRepository;
    }

    @GetMapping("/resume/{resumeId}")
    public ResponseEntity<?> getHistoryByResume(@PathVariable UUID resumeId) {
        return ResponseEntity.ok(aiService.getTailoringHistory(resumeId));
    }

    @GetMapping("/{historyId}/gap-analysis")
    public ResponseEntity<?> getGapAnalysis(@PathVariable UUID historyId) {
        try {
            GapAnalysisResponse response = aiService.generateGapAnalysis(historyId);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    private boolean ownsHistory(UUID historyId, UUID userId) {
        return tailoringHistoryRepository.findById(historyId)
                .map(h -> h.getResume().getCandidate().getUser().getId().equals(userId))
                .orElse(false);
    }

    @GetMapping("/{historyId}/learning-progress")
    public ResponseEntity<?> getLearningProgress(@PathVariable UUID historyId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (!ownsHistory(historyId, userDetails.getUser().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(learningProgressRepository.findByTailoringHistoryId(historyId));
    }

    /** Gap-to-learning bridge: mark one missing skill from the roadmap as learned. */
    @PostMapping("/{historyId}/learning-progress")
    public ResponseEntity<?> markSkillLearned(@PathVariable UUID historyId, @RequestBody LearningProgressRequest request,
                                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (!ownsHistory(historyId, userDetails.getUser().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (request.skill() == null || request.skill().isBlank()) {
            return ResponseEntity.badRequest().body("skill is required");
        }

        TailoringHistory history = tailoringHistoryRepository.findById(historyId).orElseThrow();
        LearningProgress progress = learningProgressRepository.findByTailoringHistoryIdAndSkill(historyId, request.skill())
                .orElseGet(() -> {
                    LearningProgress p = new LearningProgress();
                    p.setTailoringHistory(history);
                    p.setSkill(request.skill());
                    return p;
                });
        progress.setStatus("COMPLETED");
        progress.setCompletedAt(java.time.LocalDateTime.now());
        return ResponseEntity.ok(learningProgressRepository.save(progress));
    }
}
