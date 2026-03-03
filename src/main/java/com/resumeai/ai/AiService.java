package com.resumeai.ai;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.candidate.ATSScoreResponse;
import com.resumeai.candidate.Resume;
import com.resumeai.candidate.ResumeRepository;

@Service
public class AiService {

    // Track which resumeIds had their candidate profile updated during ATS scoring
    private final Set<UUID> profileUpdatedResumeIds = ConcurrentHashMap.newKeySet();

    private final ChatClient chatClient;
    private final ResumeRepository resumeRepository;
    private final ObjectMapper objectMapper;

    @Value("classpath:/prompts/ats-scoring.st")
    private Resource atsScoringPromptTemplate;

    @Value("classpath:/prompts/compatibility-analysis.st")
    private Resource compatibilityPromptTemplate;

    @Value("classpath:/prompts/gap-analysis.st")
    private Resource gapAnalysisPromptTemplate;

    @Value("classpath:/prompts/resume-tailor.st")
    private Resource resumeTailorPromptTemplate;

    @Value("classpath:/prompts/candidate-matching.st")
    private Resource candidateMatchingPromptTemplate;

    @Value("classpath:/prompts/profile-extraction.st")
    private Resource profileExtractionPromptTemplate;

    @Value("classpath:/prompts/job-compatibility.st")
    private Resource jobCompatibilityPromptTemplate;

    private final com.resumeai.candidate.TailoringHistoryRepository tailoringHistoryRepository;
    private final com.resumeai.recruiter.JobPostingRepository jobPostingRepository;
    private final com.resumeai.candidate.CandidateProfileRepository candidateProfileRepository;
    private final com.resumeai.recruiter.CandidateMatchRepository candidateMatchRepository;
    private final com.resumeai.candidate.ProfileSuggestionRepository profileSuggestionRepository;
    private final com.resumeai.common.AsyncOperationRepository asyncOperationRepository;
    private AiService self;

    public AiService(ChatClient.Builder chatClientBuilder, ResumeRepository resumeRepository, ObjectMapper objectMapper,
                     com.resumeai.candidate.TailoringHistoryRepository tailoringHistoryRepository,
                     com.resumeai.recruiter.JobPostingRepository jobPostingRepository,
                     com.resumeai.candidate.CandidateProfileRepository candidateProfileRepository,
                     com.resumeai.recruiter.CandidateMatchRepository candidateMatchRepository,
                     com.resumeai.candidate.ProfileSuggestionRepository profileSuggestionRepository,
                     com.resumeai.common.AsyncOperationRepository asyncOperationRepository) {
        this.chatClient = chatClientBuilder.build();
        this.resumeRepository = resumeRepository;
        this.objectMapper = objectMapper;
        this.tailoringHistoryRepository = tailoringHistoryRepository;
        this.jobPostingRepository = jobPostingRepository;
        this.candidateProfileRepository = candidateProfileRepository;
        this.candidateMatchRepository = candidateMatchRepository;
        this.profileSuggestionRepository = profileSuggestionRepository;
        this.asyncOperationRepository = asyncOperationRepository;
    }

    private void updateStatus(UUID referenceId, String type, String status, String errorMessage) {
        com.resumeai.common.AsyncOperation operation = asyncOperationRepository.findByReferenceIdAndType(referenceId, type)
                .orElseGet(() -> {
                    com.resumeai.common.AsyncOperation newOp = new com.resumeai.common.AsyncOperation();
                    newOp.setReferenceId(referenceId);
                    newOp.setType(type);
                    return newOp;
                });
        operation.setStatus(status);
        operation.setErrorMessage(errorMessage);
        asyncOperationRepository.save(operation);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void setSelf(@org.springframework.context.annotation.Lazy AiService self) {
        this.self = self;
    }

    @Async
    public void extractProfileAsync(UUID resumeId) {
        try {
            updateStatus(resumeId, "PROFILE_EXTRACTION", "PROCESSING", null);
            self.doExtractProfileWithRetry(resumeId);
            updateStatus(resumeId, "PROFILE_EXTRACTION", "COMPLETED", null);
        } catch (Exception e) {
            updateStatus(resumeId, "PROFILE_EXTRACTION", "FAILED", e.getMessage());
        }
    }

    public String getProfileExtractionStatus(UUID resumeId) {
        return asyncOperationRepository.findByReferenceIdAndType(resumeId, "PROFILE_EXTRACTION")
                .map(op -> "FAILED".equals(op.getStatus()) ? "FAILED: " + op.getErrorMessage() : op.getStatus())
                .orElse("UNKNOWN");
    }

    @Transactional
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void doExtractProfileWithRetry(UUID resumeId) throws Exception {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new IllegalArgumentException("Resume not found"));

        if (resume.getExtractedText() == null || resume.getExtractedText().isBlank()) {
            throw new IllegalStateException("Resume has no extracted text to analyze");
        }

        com.resumeai.candidate.ProfileExtractionResponse response = chatClient.prompt()
                .system(profileExtractionPromptTemplate)
                .user(u -> u.text(resume.getExtractedText()))
                .call()
                .entity(com.resumeai.candidate.ProfileExtractionResponse.class);

        com.resumeai.candidate.CandidateProfile profile = candidateProfileRepository.findById(resume.getCandidate().getId())
                .orElseThrow(() -> new IllegalStateException("Candidate profile not found for resume " + resumeId));
        boolean profileUpdated = false;

        if ((profile.getHeadline() == null || profile.getHeadline().isBlank()) && response.suggestedHeadline() != null) {
            profile.setHeadline(response.suggestedHeadline());
            profileUpdated = true;
        }
        if ((profile.getLinkedinUrl() == null || profile.getLinkedinUrl().isBlank()) && response.linkedinUrl() != null) {
            profile.setLinkedinUrl(response.linkedinUrl());
            profileUpdated = true;
        }
        if ((profile.getSkills() == null || profile.getSkills().isEmpty()) && response.skills() != null && !response.skills().isEmpty()) {
            profile.setSkills(response.skills());
            profileUpdated = true;
        }

        if (profileUpdated) {
            candidateProfileRepository.save(profile);
        }

        com.resumeai.candidate.ProfileSuggestion suggestion = profileSuggestionRepository.findByResumeId(resumeId)
                .orElse(new com.resumeai.candidate.ProfileSuggestion());

        suggestion.setResume(resume);
        suggestion.setCandidate(profile);
        suggestion.setSuggestedHeadline(response.suggestedHeadline());
        suggestion.setSuggestedSkills(response.skills());
        suggestion.setSuggestedLinkedinUrl(response.linkedinUrl());
        suggestion.setStatus("PENDING");

        profileSuggestionRepository.save(suggestion);
    }

    @Async
    public void scoreResumeAsync(UUID resumeId) {
        updateStatus(resumeId, "SCORING", "PROCESSING", null);
        try {
            self.doScoreResumeWithRetry(resumeId);
            updateStatus(resumeId, "SCORING", "COMPLETED", null);
        } catch (Exception e) {
            updateStatus(resumeId, "SCORING", "FAILED", e.getMessage());
        }
    }

    @Transactional
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void doScoreResumeWithRetry(UUID resumeId) throws Exception {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new IllegalArgumentException("Resume not found"));

        if (resume.getExtractedText() == null || resume.getExtractedText().isBlank()) {
            throw new IllegalStateException("Resume has no extracted text to analyze");
        }

        ATSScoreResponse response = chatClient.prompt()
                .system(s -> s.text(atsScoringPromptTemplate))
                .user(u -> u.text(resume.getExtractedText()))
                .call()
                .entity(ATSScoreResponse.class);

        resume.setAtsScore(response.overallScore());
        resume.setScoreBreakdown(objectMapper.writeValueAsString(response));
        resumeRepository.save(resume);

        // Save extracted profile data to candidate profile (populate empty fields on first ATS scoring)
        com.resumeai.candidate.CandidateProfile profile = candidateProfileRepository.findById(resume.getCandidate().getId())
                .orElse(null);
        if (profile != null) {
            boolean updated = false;

            if ((profile.getSkills() == null || profile.getSkills().isEmpty())
                    && response.detectedSkills() != null && !response.detectedSkills().isEmpty()) {
                profile.setSkills(response.detectedSkills());
                updated = true;
            }
            if ((profile.getHeadline() == null || profile.getHeadline().isBlank())
                    && response.suggestedHeadline() != null && !response.suggestedHeadline().isBlank()) {
                profile.setHeadline(response.suggestedHeadline());
                updated = true;
            }
            if ((profile.getLinkedinUrl() == null || profile.getLinkedinUrl().isBlank())
                    && response.linkedinUrl() != null && !response.linkedinUrl().isBlank()) {
                profile.setLinkedinUrl(response.linkedinUrl());
                updated = true;
            }
            // Always update experience and education summaries from the latest ATS analysis
            if (response.experienceSummary() != null && !response.experienceSummary().isBlank()) {
                profile.setExperienceSummary(response.experienceSummary());
                updated = true;
            }
            if (response.educationSummary() != null && !response.educationSummary().isBlank()) {
                profile.setEducationSummary(response.educationSummary());
                updated = true;
            }

            if (updated) {
                candidateProfileRepository.save(profile);
                profileUpdatedResumeIds.add(resumeId);
            }
        }
    }

    public boolean wasProfileUpdated(UUID resumeId) {
        return profileUpdatedResumeIds.remove(resumeId);
    }

    public String getScoringStatus(UUID resumeId) {
        return asyncOperationRepository.findByReferenceIdAndType(resumeId, "SCORING")
                .map(op -> "FAILED".equals(op.getStatus()) ? "FAILED: " + op.getErrorMessage() : op.getStatus())
                .orElse("UNKNOWN");
    }

    public ATSScoreResponse getScore(UUID resumeId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new IllegalArgumentException("Resume not found"));
        if (resume.getScoreBreakdown() == null) {
            throw new IllegalStateException("Score not available yet");
        }
        try {
            return objectMapper.readValue(resume.getScoreBreakdown(), ATSScoreResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse score breakdown", e);
        }
    }

    @Transactional
    public com.resumeai.candidate.CompatibilityResponse analyzeCompatibility(UUID resumeId, String jobDescription) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new IllegalArgumentException("Resume not found"));

        if (resume.getExtractedText() == null || resume.getExtractedText().isBlank()) {
            throw new IllegalStateException("Resume has no extracted text to analyze");
        }

        com.resumeai.candidate.CompatibilityAnalysisDto analysis = chatClient.prompt()
                .system(s -> s.text(compatibilityPromptTemplate))
                .user(u -> u.text("JOB DESCRIPTION:\n" + jobDescription + "\n\nCANDIDATE RESUME:\n" + resume.getExtractedText()))
                .call()
                .entity(com.resumeai.candidate.CompatibilityAnalysisDto.class);

        String tier;
        if (analysis.matchScore() >= 60) {
            tier = "GREEN";
        } else if (analysis.matchScore() >= 40) {
            tier = "AMBER";
        } else {
            tier = "RED";
        }

        com.resumeai.candidate.TailoringHistory history = new com.resumeai.candidate.TailoringHistory();
        history.setResume(resume);
        history.setJobDescription(jobDescription);
        history.setCompatibilityScore(analysis.matchScore());
        history.setCompatibilityTier(tier);
        try {
            history.setCompatibilityAnalysis(objectMapper.writeValueAsString(analysis));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize compatibility analysis", e);
        }

        tailoringHistoryRepository.save(history);

        return new com.resumeai.candidate.CompatibilityResponse(
                analysis.matchScore(),
                analysis.matchingSkills(),
                analysis.missingCriticalSkills(),
                analysis.partiallyMatching(),
                analysis.experienceGapYears(),
                analysis.educationMatch(),
                tier,
                analysis.detailedReasoning(),
                history.getId()
        );
    }

    public com.resumeai.candidate.GapAnalysisResponse generateGapAnalysis(UUID historyId) {
        com.resumeai.candidate.TailoringHistory history = tailoringHistoryRepository.findById(historyId)
                .orElseThrow(() -> new IllegalArgumentException("History not found"));

        if (!"AMBER".equals(history.getCompatibilityTier())) {
            throw new IllegalStateException("Gap analysis is only available for AMBER tier matches");
        }

        try {
            com.resumeai.candidate.CompatibilityAnalysisDto analysis = objectMapper.readValue(history.getCompatibilityAnalysis(), com.resumeai.candidate.CompatibilityAnalysisDto.class);
            String missingSkills = String.join(", ", analysis.missingCriticalSkills());

            return chatClient.prompt()
                    .system(s -> s.text(gapAnalysisPromptTemplate))
                    .user(u -> u.text("JOB DESCRIPTION:\n" + history.getJobDescription() + "\n\nMISSING CRITICAL SKILLS:\n" + missingSkills + "\n\nEXPERIENCE GAP (YEARS):\n" + analysis.experienceGapYears()))
                    .call()
                    .entity(com.resumeai.candidate.GapAnalysisResponse.class);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate gap analysis", e);
        }
    }

    @Async
    public void matchCandidatesAsync(UUID jobPostingId) {
        updateStatus(jobPostingId, "MATCHING", "PROCESSING", null);
        try {
            self.doMatchCandidatesWithRetry(jobPostingId);
            updateStatus(jobPostingId, "MATCHING", "COMPLETED", null);
        } catch (Exception e) {
            updateStatus(jobPostingId, "MATCHING", "FAILED", e.getMessage());
        }
    }

    @Transactional
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void doMatchCandidatesWithRetry(UUID jobPostingId) throws Exception {
        com.resumeai.recruiter.JobPosting job = jobPostingRepository.findById(jobPostingId)
                .orElseThrow(() -> new IllegalArgumentException("Job posting not found"));

        String jobDescText = "Title: " + job.getTitle() + "\nRequirements: " + String.join(", ", job.getRequiredSkills()) + "\nDescription: " + job.getDescription();

        // Note: In a real app, we'd pre-filter using DB/Elasticsearch. Here we just take open candidates.
        java.util.List<com.resumeai.candidate.CandidateProfile> candidates = candidateProfileRepository.findByOpenToOpportunitiesTrue();

        // Implement pre-filtering if more than 50 candidates, keep top 20
        if (candidates.size() > 50 && job.getRequiredSkills() != null && !job.getRequiredSkills().isEmpty()) {
            candidates.sort((c1, c2) -> {
                long c1Match = c1.getSkills() == null ? 0 : c1.getSkills().stream().filter(s -> job.getRequiredSkills().contains(s)).count();
                long c2Match = c2.getSkills() == null ? 0 : c2.getSkills().stream().filter(s -> job.getRequiredSkills().contains(s)).count();
                return Long.compare(c2Match, c1Match); // Descending
            });
            candidates = candidates.subList(0, Math.min(20, candidates.size()));
        }

        for (com.resumeai.candidate.CandidateProfile candidate : candidates) {
            java.util.Optional<Resume> optResume = resumeRepository.findFirstByCandidateIdAndIsPrimaryTrue(candidate.getId());
            if (optResume.isEmpty() || optResume.get().getExtractedText() == null) continue;

            String candidateText = "Headline: " + candidate.getHeadline() + "\nSkills: " + String.join(", ", candidate.getSkills()) + "\nResume:\n" + optResume.get().getExtractedText();

            com.resumeai.recruiter.CandidateMatchResultDto result = chatClient.prompt()
                    .system(s -> s.text(candidateMatchingPromptTemplate))
                    .user(u -> u.text("JOB POSTING:\n" + jobDescText + "\n\nCANDIDATE RESUME/PROFILE:\n" + candidateText))
                    .call()
                    .entity(com.resumeai.recruiter.CandidateMatchResultDto.class);

            // Update or create match
            com.resumeai.recruiter.CandidateMatch match = candidateMatchRepository
                    .findByJobPostingIdAndCandidateId(job.getId(), candidate.getId())
                    .orElseGet(() -> {
                        com.resumeai.recruiter.CandidateMatch newMatch = new com.resumeai.recruiter.CandidateMatch();
                        newMatch.setJobPosting(job);
                        newMatch.setCandidate(candidate);
                        return newMatch;
                    });

            match.setMatchScore(result.matchScore());
            match.setMatchReasoning(result.matchReasoning());
            match.setMatchingSkills(result.topMatchingSkills());
            match.setIdentifiedGaps(result.identifiedGaps());

            candidateMatchRepository.save(match);
        }
    }

    public String getMatchingStatus(UUID jobPostingId) {
        return asyncOperationRepository.findByReferenceIdAndType(jobPostingId, "MATCHING")
                .map(op -> "FAILED".equals(op.getStatus()) ? "FAILED: " + op.getErrorMessage() : op.getStatus())
                .orElse("UNKNOWN");
    }

    public java.util.List<com.resumeai.candidate.TailoringHistory> getTailoringHistory(UUID resumeId) {
        return tailoringHistoryRepository.findByResumeIdOrderByCreatedAtDesc(resumeId);
    }

    public com.resumeai.candidate.JobCompatibilityResponse checkJobCompatibility(
            com.resumeai.recruiter.JobPosting job,
            com.resumeai.candidate.CandidateProfile candidate) {
        String experienceRange = (job.getExperienceMin() != null || job.getExperienceMax() != null)
                ? (job.getExperienceMin() != null ? job.getExperienceMin() : 0)
                  + "-" + (job.getExperienceMax() != null ? job.getExperienceMax() : "+") + " years"
                : "Not specified";

        String requiredSkills = (job.getRequiredSkills() != null && !job.getRequiredSkills().isEmpty())
                ? String.join(", ", job.getRequiredSkills())
                : "Not specified";

        String candidateSkills = (candidate.getSkills() != null && !candidate.getSkills().isEmpty())
                ? String.join(", ", candidate.getSkills())
                : "Not specified";

        String userMessage = "JOB DETAILS:\n"
                + "Title: " + job.getTitle() + "\n"
                + "Company: " + job.getCompany() + "\n"
                + "Description: " + (job.getDescription() != null ? job.getDescription() : "Not provided") + "\n"
                + "Required Skills: " + requiredSkills + "\n"
                + "Experience Required: " + experienceRange + "\n\n"
                + "CANDIDATE PROFILE:\n"
                + "Headline: " + (candidate.getHeadline() != null ? candidate.getHeadline() : "Not specified") + "\n"
                + "Skills: " + candidateSkills + "\n"
                + "Experience Summary: " + (candidate.getExperienceSummary() != null ? candidate.getExperienceSummary() : "Not provided") + "\n"
                + "Education Summary: " + (candidate.getEducationSummary() != null ? candidate.getEducationSummary() : "Not provided");

        try {
            return chatClient.prompt()
                    .system(s -> s.text(jobCompatibilityPromptTemplate))
                    .user(u -> u.text(userMessage))
                    .call()
                    .entity(com.resumeai.candidate.JobCompatibilityResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to check job compatibility", e);
        }
    }

    @Transactional
    public com.resumeai.candidate.TailoredResumeResponse tailorResume(UUID resumeId, com.resumeai.candidate.TailorResumeRequest request) {
        com.resumeai.candidate.TailoringHistory history = tailoringHistoryRepository.findById(request.compatibilityId())
                .orElseThrow(() -> new IllegalArgumentException("Compatibility analysis not found"));

        if (!history.getResume().getId().equals(resumeId)) {
            throw new IllegalArgumentException("Compatibility analysis does not belong to this resume");
        }

        if (!"GREEN".equals(history.getCompatibilityTier())) {
            throw new IllegalStateException("Resume tailoring is only available for GREEN tier matches (Score >= 60)");
        }

        Resume resume = history.getResume();

        try {
            com.resumeai.candidate.TailoredResumeResponse response = chatClient.prompt()
                    .system(s -> s.text(resumeTailorPromptTemplate))
                    .user(u -> u.text("JOB DESCRIPTION:\n" + request.jobDescription() + "\n\nCANDIDATE RESUME:\n" + resume.getExtractedText()))
                    .call()
                    .entity(com.resumeai.candidate.TailoredResumeResponse.class);

            history.setTailoredContent(response.tailoredContent());
            history.setChangesMade(objectMapper.writeValueAsString(response.changesMade()));
            tailoringHistoryRepository.save(history);

            return response;
        } catch (Exception e) {
            throw new RuntimeException("Failed to tailor resume", e);
        }
    }
}
