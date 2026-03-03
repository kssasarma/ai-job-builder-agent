package com.resumeai.candidate;

import java.io.IOException;
import java.util.Map;
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
import org.springframework.web.multipart.MultipartFile;

import com.resumeai.ai.AiService;
import com.resumeai.auth.CustomUserDetails;

@RestController
@RequestMapping("/api/candidate/resume")
public class ResumeController {

    private final ResumeService resumeService;
    private final AiService aiService;

    public ResumeController(ResumeService resumeService, AiService aiService) {
        this.resumeService = resumeService;
        this.aiService = aiService;
    }

    @GetMapping
    public ResponseEntity<?> getResumes(@AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            java.util.UUID candidateId = resumeService.getCandidateId(userDetails.getUser().getId());
            return ResponseEntity.ok(resumeService.getResumes(candidateId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}/profile-extraction/status")
    public ResponseEntity<?> getProfileExtractionStatus(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (!resumeService.verifyResumeOwnership(id, userDetails.getUser().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String status = aiService.getProfileExtractionStatus(id);
        return ResponseEntity.ok(Map.of("status", status));
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadResume(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            ResumeUploadResponse response = resumeService.uploadResume(userDetails.getUser().getId(), file);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to process file upload.");
        }
    }

    @PostMapping("/{id}/score")
    public ResponseEntity<?> scoreResume(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (!resumeService.verifyResumeOwnership(id, userDetails.getUser().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            aiService.scoreResumeAsync(id);
            return ResponseEntity.accepted().body(Map.of("message", "Scoring started", "resumeId", id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}/score/status")
    public ResponseEntity<?> getScoreStatus(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (!resumeService.verifyResumeOwnership(id, userDetails.getUser().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String status = aiService.getScoringStatus(id);
        if ("COMPLETED".equals(status)) {
            boolean profileUpdated = aiService.wasProfileUpdated(id);
            return ResponseEntity.ok(Map.of("status", status, "result", aiService.getScore(id), "profileUpdated", profileUpdated));
        }
        return ResponseEntity.ok(Map.of("status", status));
    }

    @PostMapping("/{id}/analyze-compatibility")
    public ResponseEntity<?> analyzeCompatibility(@PathVariable UUID id, @RequestBody JobDescriptionRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (!resumeService.verifyResumeOwnership(id, userDetails.getUser().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            CompatibilityResponse response = aiService.analyzeCompatibility(id, request.jobDescription());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/tailor")
    public ResponseEntity<?> tailorResume(@PathVariable UUID id, @RequestBody TailorResumeRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (!resumeService.verifyResumeOwnership(id, userDetails.getUser().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            TailoredResumeResponse response = aiService.tailorResume(id, request);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage()); // 400 with explanation
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to tailor resume: " + e.getMessage());
        }
    }
}
