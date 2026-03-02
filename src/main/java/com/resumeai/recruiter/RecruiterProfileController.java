package com.resumeai.recruiter;

import com.resumeai.auth.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recruiter/profile")
public class RecruiterProfileController {

    private final RecruiterProfileRepository recruiterProfileRepository;

    public RecruiterProfileController(RecruiterProfileRepository recruiterProfileRepository) {
        this.recruiterProfileRepository = recruiterProfileRepository;
    }

    @GetMapping
    public ResponseEntity<RecruiterProfileDto> getProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        RecruiterProfile profile = recruiterProfileRepository.findByUserId(userDetails.getUser().getId())
                .orElseGet(() -> {
                    RecruiterProfile newProfile = new RecruiterProfile();
                    newProfile.setUser(userDetails.getUser());
                    return newProfile;
                });
        return ResponseEntity.ok(RecruiterProfileDto.fromEntity(profile));
    }

    @PostMapping
    public ResponseEntity<RecruiterProfileDto> createProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody RecruiterProfileUpdateRequest request) {
        return updateProfile(userDetails, request);
    }

    @PutMapping
    public ResponseEntity<RecruiterProfileDto> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody RecruiterProfileUpdateRequest request) {

        RecruiterProfile profile = recruiterProfileRepository.findByUserId(userDetails.getUser().getId())
                .orElseGet(() -> {
                    RecruiterProfile newProfile = new RecruiterProfile();
                    newProfile.setUser(userDetails.getUser());
                    return newProfile;
                });

        if (request.companyName() != null) {
            profile.setCompanyName(request.companyName());
        }
        if (request.companyWebsite() != null) {
            profile.setCompanyWebsite(request.companyWebsite());
        }

        RecruiterProfile updatedProfile = recruiterProfileRepository.save(profile);
        return ResponseEntity.ok(RecruiterProfileDto.fromEntity(updatedProfile));
    }
}

record RecruiterProfileUpdateRequest(String companyName, String companyWebsite) {}
