package com.resumeai.recruiter;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.resumeai.auth.CustomUserDetails;
import com.resumeai.candidate.CandidateProfile;
import com.resumeai.candidate.CandidateProfileRepository;
import com.resumeai.candidate.Resume;
import com.resumeai.candidate.ResumeRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/recruiter/candidates")
public class RecruiterCandidateController {

    private final CandidateProfileRepository candidateProfileRepository;
    private final ResumeRepository resumeRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final ProfileRevealRequestRepository profileRevealRequestRepository;

    @Value("${app.upload.dir:uploads/resumes}")
    private String uploadDir;

    public RecruiterCandidateController(CandidateProfileRepository candidateProfileRepository, ResumeRepository resumeRepository,
                                         RecruiterProfileRepository recruiterProfileRepository,
                                         ProfileRevealRequestRepository profileRevealRequestRepository) {
        this.candidateProfileRepository = candidateProfileRepository;
        this.resumeRepository = resumeRepository;
        this.recruiterProfileRepository = recruiterProfileRepository;
        this.profileRevealRequestRepository = profileRevealRequestRepository;
    }

    private RecruiterProfile currentRecruiter(UUID userId) {
        return recruiterProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Recruiter profile not found"));
    }

    /**
     * Anonymized-first discovery: if the candidate has opted in and this recruiter
     * hasn't had a reveal request approved yet, mask name/contact — the recruiter
     * still sees skills, headline, ATS score, and experience summary to judge fit.
     */
    private CandidateDto toDto(CandidateProfile profile, Integer latestAtsScore, UUID recruiterId) {
        boolean anonymized = Boolean.TRUE.equals(profile.getAnonymizedDiscovery())
                && !profileRevealRequestRepository.existsByCandidateIdAndRecruiterIdAndStatus(profile.getId(), recruiterId, "APPROVED");

        return new CandidateDto(
                profile.getId(),
                anonymized ? null : (profile.getUser() != null ? profile.getUser().getName() : null),
                profile.getHeadline(),
                profile.getSkills(),
                anonymized ? null : profile.getLinkedinUrl(),
                anonymized ? null : profile.getPreferredContactEmail(),
                latestAtsScore,
                profile.getExperienceSummary(),
                anonymized
        );
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<Page<CandidateDto>> getCandidates(
            @RequestParam(required = false) String skills,
            @RequestParam(required = false) Integer minAtsScore,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Pageable pageable) {
        RecruiterProfile recruiter = currentRecruiter(userDetails.getUser().getId());

        String skillsText = (skills != null && !skills.isBlank())
                ? Arrays.stream(skills.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.joining(","))
                : null;

        Page<CandidateProfile> profiles = candidateProfileRepository.findCandidates(skillsText, minAtsScore, pageable);

        Page<CandidateDto> candidateDtos = profiles.map(profile -> {
            Integer latestAtsScore = resumeRepository.findFirstByCandidateIdAndIsPrimaryTrue(profile.getId())
                    .map(Resume::getAtsScore)
                    .orElse(null);
            return toDto(profile, latestAtsScore, recruiter.getId());
        });

        return ResponseEntity.ok(candidateDtos);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<CandidateDto> getCandidate(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        RecruiterProfile recruiter = currentRecruiter(userDetails.getUser().getId());
        CandidateProfile profile = candidateProfileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found"));

        Integer latestAtsScore = resumeRepository.findFirstByCandidateIdAndIsPrimaryTrue(profile.getId())
                .map(Resume::getAtsScore)
                .orElse(null);

        return ResponseEntity.ok(toDto(profile, latestAtsScore, recruiter.getId()));
    }

    @PostMapping("/{id}/reveal-request")
    @Transactional
    public ResponseEntity<?> requestReveal(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        RecruiterProfile recruiter = currentRecruiter(userDetails.getUser().getId());
        CandidateProfile candidate = candidateProfileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found"));

        ProfileRevealRequest request = profileRevealRequestRepository.findByCandidateIdAndRecruiterId(id, recruiter.getId())
                .orElseGet(() -> {
                    ProfileRevealRequest r = new ProfileRevealRequest();
                    r.setCandidate(candidate);
                    r.setRecruiter(recruiter);
                    return r;
                });
        // Re-requesting after a DENIED decision puts it back to PENDING for the candidate to reconsider.
        if (!"APPROVED".equals(request.getStatus())) {
            request.setStatus("PENDING");
        }
        profileRevealRequestRepository.save(request);
        return ResponseEntity.ok(java.util.Map.of("status", request.getStatus()));
    }

    @GetMapping("/{id}/resume/download")
    @Transactional(readOnly = true)
    public ResponseEntity<?> downloadResume(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        RecruiterProfile recruiter = currentRecruiter(userDetails.getUser().getId());
        CandidateProfile profile = candidateProfileRepository.findById(id).orElse(null);
        if (profile == null) return ResponseEntity.notFound().build();

        boolean anonymized = Boolean.TRUE.equals(profile.getAnonymizedDiscovery())
                && !profileRevealRequestRepository.existsByCandidateIdAndRecruiterIdAndStatus(id, recruiter.getId(), "APPROVED");
        if (anonymized) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("This candidate's profile is anonymized. Request a reveal first.");
        }

        Resume resume = resumeRepository.findFirstByCandidateIdAndIsPrimaryTrue(id).orElse(null);
        if (resume == null || resume.getFilePath() == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            byte[] fileBytes = Files.readAllBytes(Paths.get(resume.getFilePath()));
            ByteArrayResource resource = new ByteArrayResource(fileBytes);

            String candidateName = (profile.getUser() != null && profile.getUser().getName() != null)
                    ? profile.getUser().getName().replaceAll("\\s+", "_")
                    : "candidate";
            String filename = "resume_" + candidateName + ".pdf";

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentLength(fileBytes.length)
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Failed to read resume file");
        }
    }
}
