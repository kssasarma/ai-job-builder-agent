package com.resumeai.candidate;

import com.resumeai.auth.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/candidate/profile")
public class CandidateProfileController {

    private final CandidateProfileRepository candidateProfileRepository;

    public CandidateProfileController(CandidateProfileRepository candidateProfileRepository) {
        this.candidateProfileRepository = candidateProfileRepository;
    }

    @GetMapping
    public ResponseEntity<CandidateProfile> getProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        CandidateProfile profile = candidateProfileRepository.findByUserId(userDetails.getUser().getId())
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));
        return ResponseEntity.ok(profile);
    }

    @PutMapping
    public ResponseEntity<CandidateProfile> updateProfile(
            @RequestBody CandidateProfileUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        CandidateProfile profile = candidateProfileRepository.findByUserId(userDetails.getUser().getId())
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));

        if (request.headline() != null) profile.setHeadline(request.headline());
        if (request.linkedinUrl() != null) profile.setLinkedinUrl(request.linkedinUrl());
        if (request.preferredContactEmail() != null) profile.setPreferredContactEmail(request.preferredContactEmail());
        if (request.openToOpportunities() != null) profile.setOpenToOpportunities(request.openToOpportunities());
        if (request.skills() != null) profile.setSkills(request.skills());

        return ResponseEntity.ok(candidateProfileRepository.save(profile));
    }
}
