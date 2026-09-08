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

@RestController
@RequestMapping("/api/candidate/mock-interview")
public class MockInterviewController {

    private final AiService aiService;
    private final TailoringHistoryRepository tailoringHistoryRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final MockInterviewSessionRepository mockInterviewSessionRepository;
    private final MockInterviewQuestionRepository mockInterviewQuestionRepository;

    public MockInterviewController(AiService aiService, TailoringHistoryRepository tailoringHistoryRepository,
                                    CandidateProfileRepository candidateProfileRepository,
                                    MockInterviewSessionRepository mockInterviewSessionRepository,
                                    MockInterviewQuestionRepository mockInterviewQuestionRepository) {
        this.aiService = aiService;
        this.tailoringHistoryRepository = tailoringHistoryRepository;
        this.candidateProfileRepository = candidateProfileRepository;
        this.mockInterviewSessionRepository = mockInterviewSessionRepository;
        this.mockInterviewQuestionRepository = mockInterviewQuestionRepository;
    }

    private UUID currentCandidateId(UUID userId) {
        return candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate profile not found"))
                .getId();
    }

    @PostMapping("/start")
    public ResponseEntity<?> start(@RequestBody MockInterviewStartRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID candidateId = currentCandidateId(userDetails.getUser().getId());
        TailoringHistory history = tailoringHistoryRepository.findById(request.tailoringHistoryId()).orElse(null);
        if (history == null || !history.getResume().getCandidate().getId().equals(candidateId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            return ResponseEntity.ok(aiService.startMockInterview(request.tailoringHistoryId()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to start mock interview: " + e.getMessage());
        }
    }

    @GetMapping("/sessions/{sessionId}/questions")
    public ResponseEntity<?> getQuestions(@PathVariable UUID sessionId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID candidateId = currentCandidateId(userDetails.getUser().getId());
        MockInterviewSession session = mockInterviewSessionRepository.findById(sessionId).orElse(null);
        if (session == null || !session.getCandidate().getId().equals(candidateId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(aiService.getMockInterviewQuestions(sessionId));
    }

    @PostMapping("/questions/{questionId}/answer")
    public ResponseEntity<?> answer(@PathVariable UUID questionId, @RequestBody MockInterviewAnswerRequest request,
                                     @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID candidateId = currentCandidateId(userDetails.getUser().getId());
        MockInterviewQuestion question = mockInterviewQuestionRepository.findById(questionId).orElse(null);
        if (question == null || !question.getSession().getCandidate().getId().equals(candidateId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            return ResponseEntity.ok(aiService.submitMockInterviewAnswer(questionId, request.answer()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to score answer: " + e.getMessage());
        }
    }
}
