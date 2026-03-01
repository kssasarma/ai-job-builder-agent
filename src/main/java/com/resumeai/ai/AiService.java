package com.resumeai.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.candidate.ATSScoreResponse;
import com.resumeai.candidate.Resume;
import com.resumeai.candidate.ResumeRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiService {

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

    // In-memory status map for simplicity (UUID -> Status String)
    private final Map<UUID, String> scoringStatusMap = new ConcurrentHashMap<>();
    private final Map<UUID, String> matchingStatusMap = new ConcurrentHashMap<>();
    private final Map<UUID, String> profileExtractionStatusMap = new ConcurrentHashMap<>();

    private final com.resumeai.candidate.TailoringHistoryRepository tailoringHistoryRepository;
    private final com.resumeai.recruiter.JobPostingRepository jobPostingRepository;
    private final com.resumeai.candidate.CandidateProfileRepository candidateProfileRepository;
    private final com.resumeai.recruiter.CandidateMatchRepository candidateMatchRepository;
    private final com.resumeai.candidate.ProfileSuggestionRepository profileSuggestionRepository;
    private AiService self;

    public AiService(ChatClient.Builder chatClientBuilder, ResumeRepository resumeRepository, ObjectMapper objectMapper,
                     com.resumeai.candidate.TailoringHistoryRepository tailoringHistoryRepository,
                     com.resumeai.recruiter.JobPostingRepository jobPostingRepository,
                     com.resumeai.candidate.CandidateProfileRepository candidateProfileRepository,
                     com.resumeai.recruiter.CandidateMatchRepository candidateMatchRepository,
                     com.resumeai.candidate.ProfileSuggestionRepository profileSuggestionRepository) {
        this.chatClient = chatClientBuilder.build();
        this.resumeRepository = resumeRepository;
        this.objectMapper = objectMapper;
        this.tailoringHistoryRepository = tailoringHistoryRepository;
        this.jobPostingRepository = jobPostingRepository;
        this.candidateProfileRepository = candidateProfileRepository;
        this.candidateMatchRepository = candidateMatchRepository;
        this.profileSuggestionRepository = profileSuggestionRepository;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void setSelf(@org.springframework.context.annotation.Lazy AiService self) {
        this.self = self;
    }

    @Async
    public void extractProfileAsync(UUID resumeId) {
        profileExtractionStatusMap.put(resumeId, "PROCESSING");
        try {
            self.doExtractProfileWithRetry(resumeId);
            profileExtractionStatusMap.put(resumeId, "COMPLETED");
        } catch (Exception e) {
            profileExtractionStatusMap.put(resumeId, "FAILED: " + e.getMessage());
        }
    }

    public String getProfileExtractionStatus(UUID resumeId) {
        return profileExtractionStatusMap.getOrDefault(resumeId, "UNKNOWN");
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
                .system(s -> s.text(profileExtractionPromptTemplate).param("resume", resume.getExtractedText()))
                .call()
                .entity(com.resumeai.candidate.ProfileExtractionResponse.class);

        com.resumeai.candidate.CandidateProfile profile = resume.getCandidate();
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
        scoringStatusMap.put(resumeId, "PROCESSING");
        try {
            self.doScoreResumeWithRetry(resumeId);
            scoringStatusMap.put(resumeId, "COMPLETED");
        } catch (Exception e) {
            scoringStatusMap.put(resumeId, "FAILED: " + e.getMessage());
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
    }

    public String getScoringStatus(UUID resumeId) {
        return scoringStatusMap.getOrDefault(resumeId, "UNKNOWN");
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
        matchingStatusMap.put(jobPostingId, "PROCESSING");
        try {
            self.doMatchCandidatesWithRetry(jobPostingId);
            matchingStatusMap.put(jobPostingId, "COMPLETED");
        } catch (Exception e) {
            matchingStatusMap.put(jobPostingId, "FAILED: " + e.getMessage());
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
        return matchingStatusMap.getOrDefault(jobPostingId, "UNKNOWN");
    }

    public java.util.List<com.resumeai.candidate.TailoringHistory> getTailoringHistory(UUID resumeId) {
        return tailoringHistoryRepository.findByResumeIdOrderByCreatedAtDesc(resumeId);
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
