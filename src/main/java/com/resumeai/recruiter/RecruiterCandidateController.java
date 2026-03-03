package com.resumeai.recruiter;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @Value("${app.upload.dir:uploads/resumes}")
    private String uploadDir;

    public RecruiterCandidateController(CandidateProfileRepository candidateProfileRepository, ResumeRepository resumeRepository) {
        this.candidateProfileRepository = candidateProfileRepository;
        this.resumeRepository = resumeRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
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
                    latestAtsScore,
                    profile.getExperienceSummary()
            );
        });

        return ResponseEntity.ok(candidateDtos);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
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
                latestAtsScore,
                profile.getExperienceSummary()
        );

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}/resume/download")
    @Transactional(readOnly = true)
    public ResponseEntity<?> downloadResume(@PathVariable UUID id) {
        Resume resume = resumeRepository.findFirstByCandidateIdAndIsPrimaryTrue(id).orElse(null);
        if (resume == null || resume.getFilePath() == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            byte[] fileBytes = Files.readAllBytes(Paths.get(resume.getFilePath()));
            ByteArrayResource resource = new ByteArrayResource(fileBytes);

            CandidateProfile profile = resume.getCandidate();
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
