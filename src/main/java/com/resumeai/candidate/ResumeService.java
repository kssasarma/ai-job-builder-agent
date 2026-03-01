package com.resumeai.candidate;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.List;

@Service
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final CandidateProfileRepository candidateProfileRepository;

    @Value("${app.upload.dir:uploads/resumes}")
    private String uploadDir;

    public UUID getCandidateId(UUID userId) {
        return candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate profile not found"))
                .getId();
    }

    public List<Resume> getResumes(UUID candidateId) {
        return resumeRepository.findByCandidateId(candidateId);
    }

    public ResumeService(ResumeRepository resumeRepository, CandidateProfileRepository candidateProfileRepository) {
        this.resumeRepository = resumeRepository;
        this.candidateProfileRepository = candidateProfileRepository;
    }

    @Transactional
    public ResumeUploadResponse uploadResume(UUID userId, MultipartFile file) throws IOException {
        if (file.isEmpty() || !file.getContentType().equals("application/pdf")) {
            throw new IllegalArgumentException("Invalid file format. Only PDF is allowed.");
        }

        if (file.getSize() > 5 * 1024 * 1024) { // 5MB
            throw new IllegalArgumentException("File size exceeds 5MB limit.");
        }

        CandidateProfile candidate = candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate profile not found"));

        UUID resumeId = UUID.randomUUID();
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".") ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".pdf";
        String filename = resumeId.toString() + extension;

        Path userUploadPath = Paths.get(uploadDir, userId.toString());
        if (!Files.exists(userUploadPath)) {
            Files.createDirectories(userUploadPath);
        }

        Path filePath = userUploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        String extractedText = extractTextFromPdf(filePath.toFile());

        // Set previous primary resumes to false
        List<Resume> existingResumes = resumeRepository.findByCandidateId(candidate.getId());
        for (Resume r : existingResumes) {
            if (r.getPrimary()) {
                r.setPrimary(false);
                resumeRepository.save(r);
            }
        }

        Resume resume = new Resume();
        resume.setId(resumeId);
        resume.setCandidate(candidate);
        resume.setFilePath(filePath.toString());
        resume.setExtractedText(extractedText);
        resume.setPrimary(true);

        resumeRepository.save(resume);

        String preview = extractedText.length() > 500 ? extractedText.substring(0, 500) : extractedText;
        return new ResumeUploadResponse(resume.getId(), preview);
    }

    private String extractTextFromPdf(File file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
}
