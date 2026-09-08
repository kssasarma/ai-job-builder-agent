package com.resumeai.ai;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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

    // Vector pre-filter keeps the expensive per-candidate LLM re-rank bounded no
    // matter how many open candidates exist (see selectCandidatesForMatching).
    private static final int VECTOR_PREFILTER_LIMIT = 30;

    // Track which resumeIds had their candidate profile updated during ATS scoring
    private final Set<UUID> profileUpdatedResumeIds = ConcurrentHashMap.newKeySet();

    private final ChatClient chatClient;
    private final ResumeRepository resumeRepository;
    private final ObjectMapper objectMapper;
    private final EmbeddingService embeddingService;
    private final AiDecisionLogRepository aiDecisionLogRepository;

    @Value("${spring.ai.openai.chat.options.model:gpt-4o}")
    private String primaryModel;

    // Cheaper/faster model reserved for high-volume, lower-stakes calls (candidate
    // pre-ranking, interview kit drafting) — GPT-4o is kept for gap analysis and
    // resume tailoring, where reasoning quality matters most to the candidate.
    @Value("${app.ai.fast-model:gpt-4o-mini}")
    private String fastModel;

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

    @Value("classpath:/prompts/interview-kit.st")
    private Resource interviewKitPromptTemplate;

    @Value("classpath:/prompts/mock-interview-questions.st")
    private Resource mockInterviewQuestionsPromptTemplate;

    @Value("classpath:/prompts/mock-interview-feedback.st")
    private Resource mockInterviewFeedbackPromptTemplate;

    @Value("classpath:/prompts/skill-challenge-question.st")
    private Resource skillChallengeQuestionPromptTemplate;

    @Value("classpath:/prompts/skill-challenge-grade.st")
    private Resource skillChallengeGradePromptTemplate;

    @Value("classpath:/prompts/trajectory-simulator.st")
    private Resource trajectorySimulatorPromptTemplate;

    private final com.resumeai.candidate.TailoringHistoryRepository tailoringHistoryRepository;
    private final com.resumeai.recruiter.JobPostingRepository jobPostingRepository;
    private final com.resumeai.candidate.CandidateProfileRepository candidateProfileRepository;
    private final com.resumeai.recruiter.CandidateMatchRepository candidateMatchRepository;
    private final com.resumeai.candidate.ProfileSuggestionRepository profileSuggestionRepository;
    private final com.resumeai.common.AsyncOperationRepository asyncOperationRepository;
    private final com.resumeai.recruiter.InterviewKitRepository interviewKitRepository;
    private final MockInterviewRepositories mockInterviewRepositories;
    private final SimpMessagingTemplate messagingTemplate;
    private final com.resumeai.candidate.SkillChallengeRepository skillChallengeRepository;
    private AiService self;

    public AiService(ChatClient.Builder chatClientBuilder, ResumeRepository resumeRepository, ObjectMapper objectMapper,
                     com.resumeai.candidate.TailoringHistoryRepository tailoringHistoryRepository,
                     com.resumeai.recruiter.JobPostingRepository jobPostingRepository,
                     com.resumeai.candidate.CandidateProfileRepository candidateProfileRepository,
                     com.resumeai.recruiter.CandidateMatchRepository candidateMatchRepository,
                     com.resumeai.candidate.ProfileSuggestionRepository profileSuggestionRepository,
                     com.resumeai.common.AsyncOperationRepository asyncOperationRepository,
                     EmbeddingService embeddingService,
                     AiDecisionLogRepository aiDecisionLogRepository,
                     com.resumeai.recruiter.InterviewKitRepository interviewKitRepository,
                     MockInterviewRepositories mockInterviewRepositories,
                     SimpMessagingTemplate messagingTemplate,
                     com.resumeai.candidate.SkillChallengeRepository skillChallengeRepository) {
        this.chatClient = chatClientBuilder.build();
        this.resumeRepository = resumeRepository;
        this.objectMapper = objectMapper;
        this.tailoringHistoryRepository = tailoringHistoryRepository;
        this.jobPostingRepository = jobPostingRepository;
        this.candidateProfileRepository = candidateProfileRepository;
        this.candidateMatchRepository = candidateMatchRepository;
        this.profileSuggestionRepository = profileSuggestionRepository;
        this.asyncOperationRepository = asyncOperationRepository;
        this.embeddingService = embeddingService;
        this.aiDecisionLogRepository = aiDecisionLogRepository;
        this.interviewKitRepository = interviewKitRepository;
        this.mockInterviewRepositories = mockInterviewRepositories;
        this.messagingTemplate = messagingTemplate;
        this.skillChallengeRepository = skillChallengeRepository;
    }

    /** Records one row in the AI decision ledger — the data behind the explainability panel. */
    private void logDecision(String decisionType, UUID referenceId, String model, String promptVersion,
                              String summary, UUID subjectUserId) {
        AiDecisionLog logEntry = new AiDecisionLog();
        logEntry.setDecisionType(decisionType);
        logEntry.setReferenceId(referenceId);
        logEntry.setModel(model);
        logEntry.setPromptVersion(promptVersion);
        logEntry.setSummary(summary);
        logEntry.setSubjectUserId(subjectUserId);
        aiDecisionLogRepository.save(logEntry);
    }

    private OpenAiChatOptions fastModelOptions() {
        return OpenAiChatOptions.builder().model(fastModel).build();
    }

    /** Consent-based data use: a candidate can opt a resume's AI flows off entirely. */
    private void requireAiConsent(Resume resume) {
        com.resumeai.candidate.CandidateProfile candidate = resume.getCandidate();
        if (candidate != null && Boolean.FALSE.equals(candidate.getAiConsent())) {
            throw new IllegalStateException("AI consent has not been granted for this candidate profile");
        }
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

        // Push instead of making the client poll getScoringStatus()/getMatchingStatus() —
        // the topic name (an unguessable UUID + operation type) is the only access
        // control here, consistent with how other resource IDs are handled in this app.
        messagingTemplate.convertAndSend("/topic/operations/" + referenceId,
                (Object) Map.of("type", type, "status", status, "errorMessage", errorMessage == null ? "" : errorMessage));
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
        requireAiConsent(resume);

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
        requireAiConsent(resume);

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

        logDecision("SCORING", resumeId, primaryModel, "ats-scoring@v1",
                "Overall ATS score " + response.overallScore() + "/100.",
                resume.getCandidate().getUser().getId());

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
        requireAiConsent(resume);

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

        logDecision("COMPATIBILITY", resumeId, primaryModel, "compatibility-analysis@v1",
                "Compatibility score " + analysis.matchScore() + "/100, tier " + tier + ".",
                resume.getCandidate().getUser().getId());

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

            com.resumeai.candidate.GapAnalysisResponse gapAnalysis = chatClient.prompt()
                    .system(s -> s.text(gapAnalysisPromptTemplate))
                    .user(u -> u.text("JOB DESCRIPTION:\n" + history.getJobDescription() + "\n\nMISSING CRITICAL SKILLS:\n" + missingSkills + "\n\nEXPERIENCE GAP (YEARS):\n" + analysis.experienceGapYears()))
                    .call()
                    .entity(com.resumeai.candidate.GapAnalysisResponse.class);

            logDecision("GAP_ANALYSIS", historyId, primaryModel, "gap-analysis@v1",
                    "Learning roadmap generated for: " + missingSkills,
                    history.getResume().getCandidate().getUser().getId());

            return gapAnalysis;
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

        java.util.List<com.resumeai.candidate.CandidateProfile> candidates = selectCandidatesForMatching(job);

        for (com.resumeai.candidate.CandidateProfile candidate : candidates) {
            if (Boolean.FALSE.equals(candidate.getAiConsent())) continue;

            java.util.Optional<Resume> optResume = resumeRepository.findFirstByCandidateIdAndIsPrimaryTrue(candidate.getId());
            if (optResume.isEmpty() || optResume.get().getExtractedText() == null) continue;

            String candidateText = "Headline: " + candidate.getHeadline() + "\nSkills: " + String.join(", ", candidate.getSkills()) + "\nResume:\n" + optResume.get().getExtractedText();

            com.resumeai.recruiter.CandidateMatchResultDto result = chatClient.prompt()
                    .system(s -> s.text(candidateMatchingPromptTemplate))
                    .user(u -> u.text("JOB POSTING:\n" + jobDescText + "\n\nCANDIDATE RESUME/PROFILE:\n" + candidateText))
                    .options(fastModelOptions())
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

            logDecision("MATCHING", job.getId(), fastModel, "candidate-matching@v1",
                    "Candidate scored " + result.matchScore() + "/100 against \"" + job.getTitle() + "\".",
                    candidate.getUser() != null ? candidate.getUser().getId() : null);
        }
    }

    /**
     * Vector-first candidate selection: rank by cosine distance between the job's and
     * each candidate's primary-resume embedding (pgvector {@code <=>}), and only spend
     * an LLM call on the closest {@value #VECTOR_PREFILTER_LIMIT}. This is what keeps a
     * matching run cheap and fast regardless of how many open candidates exist — the
     * previous keyword-overlap prefilter only kicked in above 50 candidates and still
     * called the LLM for up to 20 of them; this replaces it whenever embeddings are
     * available and falls back to the old behavior otherwise (e.g. candidates who
     * uploaded a resume before embeddings existed).
     */
    private java.util.List<com.resumeai.candidate.CandidateProfile> selectCandidatesForMatching(com.resumeai.recruiter.JobPosting job) {
        if (embeddingService.jobHasEmbedding(job.getId())) {
            List<UUID> nearestCandidateIds = embeddingService.findNearestCandidateIds(job.getId(), VECTOR_PREFILTER_LIMIT);
            if (!nearestCandidateIds.isEmpty()) {
                Map<UUID, Integer> rankById = new LinkedHashMap<>();
                for (int i = 0; i < nearestCandidateIds.size(); i++) rankById.put(nearestCandidateIds.get(i), i);
                List<com.resumeai.candidate.CandidateProfile> candidates = candidateProfileRepository.findAllById(nearestCandidateIds);
                candidates.sort(Comparator.comparingInt(c -> rankById.getOrDefault(c.getId(), Integer.MAX_VALUE)));
                return candidates;
            }
        }

        // Fallback: no embeddings yet for this job or for enough candidates — use the
        // original keyword-overlap heuristic so matching still works during migration.
        java.util.List<com.resumeai.candidate.CandidateProfile> candidates = candidateProfileRepository.findByOpenToOpportunitiesTrue();
        if (candidates.size() > 50 && job.getRequiredSkills() != null && !job.getRequiredSkills().isEmpty()) {
            candidates.sort((c1, c2) -> {
                long c1Match = c1.getSkills() == null ? 0 : c1.getSkills().stream().filter(s -> job.getRequiredSkills().contains(s)).count();
                long c2Match = c2.getSkills() == null ? 0 : c2.getSkills().stream().filter(s -> job.getRequiredSkills().contains(s)).count();
                return Long.compare(c2Match, c1Match); // Descending
            });
            candidates = candidates.subList(0, Math.min(20, candidates.size()));
        }
        return candidates;
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
        requireAiConsent(resume);

        try {
            com.resumeai.candidate.TailoredResumeResponse response = chatClient.prompt()
                    .system(s -> s.text(resumeTailorPromptTemplate))
                    .user(u -> u.text("JOB DESCRIPTION:\n" + request.jobDescription() + "\n\nCANDIDATE RESUME:\n" + resume.getExtractedText()))
                    .call()
                    .entity(com.resumeai.candidate.TailoredResumeResponse.class);

            history.setTailoredContent(response.tailoredContent());
            history.setChangesMade(objectMapper.writeValueAsString(response.changesMade()));
            tailoringHistoryRepository.save(history);

            logDecision("TAILORING", history.getId(), primaryModel, "resume-tailor@v1",
                    "Resume tailored with " + response.changesMade().size() + " changes.",
                    resume.getCandidate().getUser().getId());

            return response;
        } catch (Exception e) {
            throw new RuntimeException("Failed to tailor resume", e);
        }
    }

    @Transactional
    public com.resumeai.recruiter.InterviewKitResponse generateInterviewKit(UUID candidateMatchId) {
        com.resumeai.recruiter.CandidateMatch match = candidateMatchRepository.findById(candidateMatchId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate match not found"));

        com.resumeai.recruiter.JobPosting job = match.getJobPosting();
        String jobDescText = "Title: " + job.getTitle()
                + "\nRequirements: " + (job.getRequiredSkills() != null ? String.join(", ", job.getRequiredSkills()) : "")
                + "\nDescription: " + job.getDescription();
        String matchingSkills = match.getMatchingSkills() != null ? String.join(", ", match.getMatchingSkills()) : "Not specified";
        String identifiedGaps = match.getIdentifiedGaps() != null && !match.getIdentifiedGaps().isEmpty()
                ? String.join(", ", match.getIdentifiedGaps())
                : "None identified — focus on validating the matching skills above";

        try {
            com.resumeai.recruiter.InterviewKitResponse kit = chatClient.prompt()
                    .system(s -> s.text(interviewKitPromptTemplate))
                    .user(u -> u.text("JOB POSTING:\n" + jobDescText + "\n\nCANDIDATE'S TOP MATCHING SKILLS:\n" + matchingSkills
                            + "\n\nCANDIDATE'S IDENTIFIED GAPS:\n" + identifiedGaps))
                    .options(fastModelOptions())
                    .call()
                    .entity(com.resumeai.recruiter.InterviewKitResponse.class);

            com.resumeai.recruiter.InterviewKit entity = interviewKitRepository.findByCandidateMatchId(candidateMatchId)
                    .orElseGet(com.resumeai.recruiter.InterviewKit::new);
            entity.setCandidateMatch(match);
            entity.setQuestions(objectMapper.writeValueAsString(kit.questions()));
            interviewKitRepository.save(entity);

            logDecision("INTERVIEW_KIT", candidateMatchId, fastModel, "interview-kit@v1",
                    kit.questions().size() + " interview questions generated targeting: " + identifiedGaps,
                    match.getCandidate().getUser() != null ? match.getCandidate().getUser().getId() : null);

            return kit;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate interview kit", e);
        }
    }

    public com.resumeai.recruiter.InterviewKitResponse getInterviewKit(UUID candidateMatchId) {
        com.resumeai.recruiter.InterviewKit entity = interviewKitRepository.findByCandidateMatchId(candidateMatchId)
                .orElseThrow(() -> new IllegalArgumentException("Interview kit not generated yet"));
        try {
            java.util.List<com.resumeai.recruiter.InterviewQuestionDto> questions = objectMapper.readValue(
                    entity.getQuestions(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, com.resumeai.recruiter.InterviewQuestionDto.class));
            return new com.resumeai.recruiter.InterviewKitResponse(questions);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse stored interview kit", e);
        }
    }

    @Transactional
    public com.resumeai.candidate.MockInterviewSessionDto startMockInterview(UUID tailoringHistoryId) {
        com.resumeai.candidate.TailoringHistory history = tailoringHistoryRepository.findById(tailoringHistoryId)
                .orElseThrow(() -> new IllegalArgumentException("Compatibility analysis not found"));

        if (!"AMBER".equals(history.getCompatibilityTier())) {
            throw new IllegalStateException("Mock interview practice is only available for AMBER tier matches (a close-but-not-quite fit)");
        }

        try {
            com.resumeai.candidate.CompatibilityAnalysisDto analysis = objectMapper.readValue(
                    history.getCompatibilityAnalysis(), com.resumeai.candidate.CompatibilityAnalysisDto.class);
            String missingSkills = String.join(", ", analysis.missingCriticalSkills());

            com.resumeai.candidate.MockInterviewQuestionsResponse generated = chatClient.prompt()
                    .system(s -> s.text(mockInterviewQuestionsPromptTemplate))
                    .user(u -> u.text("JOB DESCRIPTION:\n" + history.getJobDescription() + "\n\nMISSING CRITICAL SKILLS FOR THIS CANDIDATE:\n" + missingSkills))
                    .options(fastModelOptions())
                    .call()
                    .entity(com.resumeai.candidate.MockInterviewQuestionsResponse.class);

            com.resumeai.candidate.MockInterviewSession session = new com.resumeai.candidate.MockInterviewSession();
            session.setTailoringHistory(history);
            session.setCandidate(history.getResume().getCandidate());
            session.setStatus("IN_PROGRESS");
            session = mockInterviewRepositories.sessionRepository().save(session);

            java.util.List<com.resumeai.candidate.MockInterviewQuestion> saved = new java.util.ArrayList<>();
            int order = 0;
            for (com.resumeai.candidate.MockInterviewQuestionGenDto q : generated.questions()) {
                com.resumeai.candidate.MockInterviewQuestion question = new com.resumeai.candidate.MockInterviewQuestion();
                question.setSession(session);
                question.setTargetSkill(q.targetSkill());
                question.setQuestion(q.question());
                question.setIdealTalkingPoints(objectMapper.writeValueAsString(q.idealTalkingPoints()));
                question.setDisplayOrder(order++);
                saved.add(mockInterviewRepositories.questionRepository().save(question));
            }

            logDecision("MOCK_INTERVIEW_QUESTIONS", session.getId(), fastModel, "mock-interview-questions@v1",
                    saved.size() + " practice questions generated for gaps: " + missingSkills,
                    history.getResume().getCandidate().getUser().getId());

            return com.resumeai.candidate.MockInterviewSessionDto.fromEntities(session, saved);
        } catch (Exception e) {
            throw new RuntimeException("Failed to start mock interview", e);
        }
    }

    @Transactional
    public com.resumeai.candidate.MockInterviewQuestionDto submitMockInterviewAnswer(UUID questionId, String answer) {
        com.resumeai.candidate.MockInterviewQuestion question = mockInterviewRepositories.questionRepository().findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Practice question not found"));

        try {
            java.util.List<String> idealPoints = objectMapper.readValue(question.getIdealTalkingPoints(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));

            com.resumeai.candidate.MockInterviewFeedbackResponse feedback = chatClient.prompt()
                    .system(s -> s.text(mockInterviewFeedbackPromptTemplate))
                    .user(u -> u.text("QUESTION:\n" + question.getQuestion() + "\n\nWHAT A STRONG ANSWER COVERS:\n"
                            + String.join("; ", idealPoints) + "\n\nCANDIDATE'S ANSWER:\n" + answer))
                    .options(fastModelOptions())
                    .call()
                    .entity(com.resumeai.candidate.MockInterviewFeedbackResponse.class);

            question.setCandidateAnswer(answer);
            question.setFeedback(feedback.feedback());
            question.setScore(feedback.score());
            com.resumeai.candidate.MockInterviewQuestion updated = mockInterviewRepositories.questionRepository().save(question);

            logDecision("MOCK_INTERVIEW_FEEDBACK", question.getId(), fastModel, "mock-interview-feedback@v1",
                    "Practice answer scored " + feedback.score() + "/100 on \"" + question.getTargetSkill() + "\".",
                    question.getSession().getCandidate().getUser().getId());

            return com.resumeai.candidate.MockInterviewQuestionDto.fromEntity(updated);
        } catch (Exception e) {
            throw new RuntimeException("Failed to score mock interview answer", e);
        }
    }

    public java.util.List<com.resumeai.candidate.MockInterviewQuestionDto> getMockInterviewQuestions(UUID sessionId) {
        return mockInterviewRepositories.questionRepository().findBySessionIdOrderByDisplayOrderAsc(sessionId).stream()
                .map(com.resumeai.candidate.MockInterviewQuestionDto::fromEntity)
                .toList();
    }

    @Transactional
    public com.resumeai.candidate.SkillChallengeDto generateSkillChallenge(UUID candidateId, String skill) {
        com.resumeai.candidate.CandidateProfile candidate = candidateProfileRepository.findById(candidateId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate profile not found"));

        try {
            com.resumeai.candidate.SkillChallengeQuestionResponse generated = chatClient.prompt()
                    .system(s -> s.text(skillChallengeQuestionPromptTemplate))
                    .user(u -> u.text("SKILL TO VERIFY:\n" + skill))
                    .options(fastModelOptions())
                    .call()
                    .entity(com.resumeai.candidate.SkillChallengeQuestionResponse.class);

            com.resumeai.candidate.SkillChallenge challenge = new com.resumeai.candidate.SkillChallenge();
            challenge.setCandidate(candidate);
            challenge.setSkill(skill);
            challenge.setQuestion(generated.question());
            challenge.setIdealAnswerPoints(objectMapper.writeValueAsString(generated.idealAnswerPoints()));
            com.resumeai.candidate.SkillChallenge saved = skillChallengeRepository.save(challenge);

            logDecision("SKILL_CHALLENGE", saved.getId(), fastModel, "skill-challenge-question@v1",
                    "Challenge question generated to verify \"" + skill + "\".",
                    candidate.getUser() != null ? candidate.getUser().getId() : null);

            return com.resumeai.candidate.SkillChallengeDto.fromEntity(saved, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate skill challenge", e);
        }
    }

    @Transactional
    public com.resumeai.candidate.SkillChallengeDto submitSkillChallengeAnswer(UUID challengeId, String answer) {
        com.resumeai.candidate.SkillChallenge challenge = skillChallengeRepository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Skill challenge not found"));

        try {
            java.util.List<String> idealPoints = objectMapper.readValue(challenge.getIdealAnswerPoints(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));

            com.resumeai.candidate.SkillChallengeGradeResponse grade = chatClient.prompt()
                    .system(s -> s.text(skillChallengeGradePromptTemplate))
                    .user(u -> u.text("SKILL BEING VERIFIED:\n" + challenge.getSkill() + "\n\nQUESTION ASKED:\n" + challenge.getQuestion()
                            + "\n\nWHAT A PROFICIENT ANSWER COVERS:\n" + String.join("; ", idealPoints)
                            + "\n\nCANDIDATE'S ANSWER:\n" + answer))
                    .options(fastModelOptions())
                    .call()
                    .entity(com.resumeai.candidate.SkillChallengeGradeResponse.class);

            boolean passed = grade.score() >= com.resumeai.candidate.SkillChallenge.PASSING_SCORE;
            challenge.setAnswer(answer);
            challenge.setScore(grade.score());
            challenge.setPassed(passed);
            com.resumeai.candidate.SkillChallenge saved = skillChallengeRepository.save(challenge);

            com.resumeai.candidate.CandidateProfile candidate = challenge.getCandidate();
            if (passed) {
                java.util.List<String> verified = candidate.getVerifiedSkills() != null
                        ? new java.util.ArrayList<>(candidate.getVerifiedSkills())
                        : new java.util.ArrayList<>();
                if (!verified.contains(challenge.getSkill())) {
                    verified.add(challenge.getSkill());
                    candidate.setVerifiedSkills(verified);
                    candidateProfileRepository.save(candidate);
                }
            }

            logDecision("SKILL_CHALLENGE", saved.getId(), fastModel, "skill-challenge-grade@v1",
                    "Answer scored " + grade.score() + "/100 on \"" + challenge.getSkill() + "\" — " + (passed ? "passed" : "not passed") + ".",
                    candidate.getUser() != null ? candidate.getUser().getId() : null);

            return com.resumeai.candidate.SkillChallengeDto.fromEntity(saved, grade.feedback());
        } catch (Exception e) {
            throw new RuntimeException("Failed to grade skill challenge answer", e);
        }
    }

    public com.resumeai.candidate.TrajectorySimulationResponse generateTrajectorySimulation(UUID candidateId) {
        java.util.List<com.resumeai.candidate.TailoringHistory> history =
                tailoringHistoryRepository.findByResumeCandidateIdOrderByCreatedAtDesc(candidateId).stream()
                        .filter(h -> h.getCompatibilityAnalysis() != null)
                        .toList();

        if (history.size() < 2) {
            throw new IllegalStateException("Run at least 2 compatibility checks before requesting a trajectory simulation");
        }

        StringBuilder historyLines = new StringBuilder();
        for (com.resumeai.candidate.TailoringHistory h : history) {
            try {
                com.resumeai.candidate.CompatibilityAnalysisDto analysis = objectMapper.readValue(
                        h.getCompatibilityAnalysis(), com.resumeai.candidate.CompatibilityAnalysisDto.class);
                historyLines.append("- Score ").append(analysis.matchScore())
                        .append(", missing skills: ").append(String.join(", ", analysis.missingCriticalSkills()))
                        .append("\n");
            } catch (Exception ignored) {
                // Skip malformed/legacy rows rather than failing the whole simulation.
            }
        }

        try {
            com.resumeai.candidate.TrajectorySimulationResponse response = chatClient.prompt()
                    .system(s -> s.text(trajectorySimulatorPromptTemplate))
                    .user(u -> u.text("HISTORY OF COMPATIBILITY CHECKS:\n" + historyLines))
                    .options(fastModelOptions())
                    .call()
                    .entity(com.resumeai.candidate.TrajectorySimulationResponse.class);

            logDecision("TRAJECTORY_SIMULATION", candidateId, fastModel, "trajectory-simulator@v1",
                    "Projected " + response.currentAverageScore() + " -> " + response.projectedAverageScore()
                            + " by closing \"" + response.topGapSkill() + "\".",
                    null);

            return response;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate trajectory simulation", e);
        }
    }
}
