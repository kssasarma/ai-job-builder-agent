package com.resumeai.candidate;

import com.resumeai.auth.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/candidate/profile")
public class CandidateProfileController {

    private final CandidateProfileRepository candidateProfileRepository;
    private final ResumeRepository resumeRepository;

    public CandidateProfileController(CandidateProfileRepository candidateProfileRepository, ResumeRepository resumeRepository) {
        this.candidateProfileRepository = candidateProfileRepository;
        this.resumeRepository = resumeRepository;
    }

    @GetMapping
    public ResponseEntity<CandidateProfileDto> getProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        CandidateProfile profile = candidateProfileRepository.findByUserId(userDetails.getUser().getId())
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));
        return ResponseEntity.ok(CandidateProfileDto.fromEntity(profile));
    }

    @GetMapping("/completeness")
    public ResponseEntity<java.util.Map<String, Object>> getCompleteness(@AuthenticationPrincipal CustomUserDetails userDetails) {
        CandidateProfile profile = candidateProfileRepository.findByUserId(userDetails.getUser().getId())
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));

        int completionPercentage = 0;
        java.util.List<String> missingFields = new java.util.ArrayList<>();

        if (profile.getHeadline() != null && !profile.getHeadline().isBlank()) completionPercentage += 20;
        else missingFields.add("headline");

        if (profile.getSkills() != null && !profile.getSkills().isEmpty()) completionPercentage += 20;
        else missingFields.add("skills");

        if (profile.getLinkedinUrl() != null && !profile.getLinkedinUrl().isBlank()) completionPercentage += 10;
        else missingFields.add("linkedinUrl");

        if (profile.getPreferredContactEmail() != null && !profile.getPreferredContactEmail().isBlank()) completionPercentage += 10;
        else missingFields.add("preferredContactEmail");

        if (Boolean.TRUE.equals(profile.getOpenToOpportunities())) completionPercentage += 15;

        java.util.Optional<Resume> primaryResume = resumeRepository.findFirstByCandidateIdAndIsPrimaryTrue(profile.getId());
        boolean hasResume = primaryResume.isPresent();
        boolean hasScore = hasResume && primaryResume.get().getAtsScore() != null;

        if (hasResume) completionPercentage += 15;
        if (hasScore) completionPercentage += 10;

        return ResponseEntity.ok(java.util.Map.of(
                "completionPercentage", completionPercentage,
                "missingFields", missingFields,
                "hasResume", hasResume,
                "hasScore", hasScore,
                "openToOpportunities", Boolean.TRUE.equals(profile.getOpenToOpportunities())
        ));
    }

    @PutMapping
    public ResponseEntity<CandidateProfileDto> updateProfile(
            @RequestBody CandidateProfileUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        CandidateProfile profile = candidateProfileRepository.findByUserId(userDetails.getUser().getId())
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));

        if (request.headline() != null) profile.setHeadline(request.headline());
        if (request.linkedinUrl() != null) profile.setLinkedinUrl(request.linkedinUrl());
        if (request.preferredContactEmail() != null) profile.setPreferredContactEmail(request.preferredContactEmail());
        if (request.openToOpportunities() != null) profile.setOpenToOpportunities(request.openToOpportunities());
        if (request.skills() != null) profile.setSkills(request.skills());

        return ResponseEntity.ok(CandidateProfileDto.fromEntity(candidateProfileRepository.save(profile)));
    }
}
