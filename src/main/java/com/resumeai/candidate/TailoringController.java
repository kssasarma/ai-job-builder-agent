package com.resumeai.candidate;

import com.resumeai.ai.AiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/candidate/tailoring")
public class TailoringController {

    private final AiService aiService;

    public TailoringController(AiService aiService) {
        this.aiService = aiService;
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
}
