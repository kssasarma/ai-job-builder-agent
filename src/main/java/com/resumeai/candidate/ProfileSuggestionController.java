package com.resumeai.candidate;

import com.resumeai.auth.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/candidate/profile/suggestions")
public class ProfileSuggestionController {

    private final ProfileSuggestionRepository profileSuggestionRepository;
    private final CandidateProfileRepository candidateProfileRepository;

    public ProfileSuggestionController(ProfileSuggestionRepository profileSuggestionRepository, CandidateProfileRepository candidateProfileRepository) {
        this.profileSuggestionRepository = profileSuggestionRepository;
        this.candidateProfileRepository = candidateProfileRepository;
    }

    @GetMapping("/{resumeId}")
    public ResponseEntity<?> getSuggestion(@PathVariable UUID resumeId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        CandidateProfile candidate = candidateProfileRepository.findByUserId(userDetails.getUser().getId())
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found"));

        ProfileSuggestion suggestion = profileSuggestionRepository.findByResumeId(resumeId)
                .orElseThrow(() -> new IllegalArgumentException("Suggestion not found"));

        if (!suggestion.getCandidate().getId().equals(candidate.getId())) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(suggestion);
    }

    @PostMapping("/{resumeId}/apply")
    public ResponseEntity<?> applySuggestion(@PathVariable UUID resumeId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        CandidateProfile candidate = candidateProfileRepository.findByUserId(userDetails.getUser().getId())
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found"));

        ProfileSuggestion suggestion = profileSuggestionRepository.findByResumeId(resumeId)
                .orElseThrow(() -> new IllegalArgumentException("Suggestion not found"));

        if (!suggestion.getCandidate().getId().equals(candidate.getId())) {
            return ResponseEntity.status(403).build();
        }

        if (suggestion.getSuggestedHeadline() != null) candidate.setHeadline(suggestion.getSuggestedHeadline());
        if (suggestion.getSuggestedLinkedinUrl() != null) candidate.setLinkedinUrl(suggestion.getSuggestedLinkedinUrl());
        if (suggestion.getSuggestedSkills() != null) candidate.setSkills(suggestion.getSuggestedSkills());

        candidateProfileRepository.save(candidate);

        suggestion.setStatus("APPLIED");
        profileSuggestionRepository.save(suggestion);

        return ResponseEntity.ok(Map.of("message", "Suggestion applied successfully"));
    }

    @PostMapping("/{resumeId}/dismiss")
    public ResponseEntity<?> dismissSuggestion(@PathVariable UUID resumeId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        CandidateProfile candidate = candidateProfileRepository.findByUserId(userDetails.getUser().getId())
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found"));

        ProfileSuggestion suggestion = profileSuggestionRepository.findByResumeId(resumeId)
                .orElseThrow(() -> new IllegalArgumentException("Suggestion not found"));

        if (!suggestion.getCandidate().getId().equals(candidate.getId())) {
            return ResponseEntity.status(403).build();
        }

        suggestion.setStatus("DISMISSED");
        profileSuggestionRepository.save(suggestion);

        return ResponseEntity.ok(Map.of("message", "Suggestion dismissed"));
    }
}
