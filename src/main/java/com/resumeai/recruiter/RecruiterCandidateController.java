package com.resumeai.recruiter;

import com.resumeai.candidate.CandidateProfile;
import com.resumeai.candidate.CandidateProfileRepository;
import com.resumeai.candidate.Resume;
import com.resumeai.candidate.ResumeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.UUID;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recruiter/candidates")
public class RecruiterCandidateController {

    private final CandidateProfileRepository candidateProfileRepository;
    private final ResumeRepository resumeRepository;

    public RecruiterCandidateController(CandidateProfileRepository candidateProfileRepository, ResumeRepository resumeRepository) {
        this.candidateProfileRepository = candidateProfileRepository;
        this.resumeRepository = resumeRepository;
    }

    @GetMapping
    public ResponseEntity<Page<CandidateDto>> getCandidates(
            @RequestParam(required = false) String skills,
            @RequestParam(required = false) Integer minAtsScore,
            Pageable pageable) {

        String skillsText = (skills != null && !skills.isBlank())
                ? Arrays.stream(skills.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.joining(","))
                : null;

        Page<CandidateProfile> profiles = candidateProfileRepository.findCandidates(skillsText, minAtsScore, pageable);

        Page<CandidateDto> candidateDtos = profiles.map(profile -> {
            Integer latestAtsScore = resumeRepository.findFirstByCandidateIdAndIsPrimaryTrue(profile.getId())
                    .map(Resume::getAtsScore)
                    .orElse(null);

            return new CandidateDto(
                    profile.getId(),
                    profile.getUser() != null ? profile.getUser().getName() : null,
                    profile.getHeadline(),
                    profile.getSkills(),
                    profile.getLinkedinUrl(),
                    profile.getPreferredContactEmail(),
                    latestAtsScore
            );
        });

        return ResponseEntity.ok(candidateDtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CandidateDto> getCandidate(@PathVariable UUID id) {
        CandidateProfile profile = candidateProfileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found"));

        Integer latestAtsScore = resumeRepository.findFirstByCandidateIdAndIsPrimaryTrue(profile.getId())
                .map(Resume::getAtsScore)
                .orElse(null);

        CandidateDto dto = new CandidateDto(
                profile.getId(),
                profile.getUser() != null ? profile.getUser().getName() : null,
                profile.getHeadline(),
                profile.getSkills(),
                profile.getLinkedinUrl(),
                profile.getPreferredContactEmail(),
                latestAtsScore
        );

        return ResponseEntity.ok(dto);
    }
}
