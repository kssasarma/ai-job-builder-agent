package com.resumeai.candidate;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.resumeai.auth.CustomUserDetails;
import com.resumeai.recruiter.ProfileRevealRequest;
import com.resumeai.recruiter.ProfileRevealRequestDto;
import com.resumeai.recruiter.ProfileRevealRequestRepository;

/** The candidate's side of anonymized-first discovery: see and decide reveal requests. */
@RestController
@RequestMapping("/api/candidate/reveal-requests")
public class ProfileRevealController {

    private final ProfileRevealRequestRepository profileRevealRequestRepository;
    private final CandidateProfileRepository candidateProfileRepository;

    public ProfileRevealController(ProfileRevealRequestRepository profileRevealRequestRepository,
                                    CandidateProfileRepository candidateProfileRepository) {
        this.profileRevealRequestRepository = profileRevealRequestRepository;
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
        return ResponseEntity.ok(profileRevealRequestRepository.findByCandidateIdOrderByCreatedAtDesc(candidateId).stream()
                .map(ProfileRevealRequestDto::fromEntity)
                .toList());
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        return resolve(id, userDetails, "APPROVED");
    }

    @PostMapping("/{id}/deny")
    public ResponseEntity<?> deny(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        return resolve(id, userDetails, "DENIED");
    }

    private ResponseEntity<?> resolve(UUID id, CustomUserDetails userDetails, String status) {
        UUID candidateId = currentCandidateId(userDetails.getUser().getId());
        ProfileRevealRequest request = profileRevealRequestRepository.findById(id).orElse(null);
        if (request == null || !request.getCandidate().getId().equals(candidateId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        request.setStatus(status);
        request.setResolvedAt(java.time.LocalDateTime.now());
        return ResponseEntity.ok(ProfileRevealRequestDto.fromEntity(profileRevealRequestRepository.save(request)));
    }
}
