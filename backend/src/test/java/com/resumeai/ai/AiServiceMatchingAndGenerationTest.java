package com.resumeai.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.auth.User;
import com.resumeai.candidate.CandidateProfile;
import com.resumeai.candidate.CandidateProfileRepository;
import com.resumeai.candidate.CompatibilityAnalysisDto;
import com.resumeai.candidate.MockInterviewFeedbackResponse;
import com.resumeai.candidate.MockInterviewQuestion;
import com.resumeai.candidate.MockInterviewQuestionGenDto;
import com.resumeai.candidate.MockInterviewQuestionRepository;
import com.resumeai.candidate.MockInterviewQuestionsResponse;
import com.resumeai.candidate.MockInterviewSessionRepository;
import com.resumeai.candidate.Resume;
import com.resumeai.candidate.ResumeRepository;
import com.resumeai.candidate.TailoringHistory;
import com.resumeai.candidate.TailoringHistoryRepository;
import com.resumeai.recruiter.CandidateMatch;
import com.resumeai.recruiter.CandidateMatchRepository;
import com.resumeai.recruiter.CandidateMatchResultDto;
import com.resumeai.recruiter.InterviewKitRepository;
import com.resumeai.recruiter.InterviewKitResponse;
import com.resumeai.recruiter.InterviewQuestionDto;
import com.resumeai.recruiter.JobPosting;
import com.resumeai.recruiter.JobPostingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Regression tests locking in the JSON-shape and persistence behavior of the P0
 * roadmap flows: vector-first candidate matching, the auto-generated interview kit,
 * and the gap-aware mock interview. Each stubs {@link ChatClient} the same way
 * {@link AiServiceProfileExtractionTest} does, so a change to a prompt's response
 * schema or to what gets persisted breaks a test here instead of surfacing as a
 * silent production bug.
 */
@ExtendWith(MockitoExtension.class)
class AiServiceMatchingAndGenerationTest {

    @Mock private ChatClient.Builder chatClientBuilder;
    @Mock private ChatClient chatClient;
    @Mock private ResumeRepository resumeRepository;
    @Mock private TailoringHistoryRepository tailoringHistoryRepository;
    @Mock private JobPostingRepository jobPostingRepository;
    @Mock private CandidateProfileRepository candidateProfileRepository;
    @Mock private CandidateMatchRepository candidateMatchRepository;
    @Mock private com.resumeai.candidate.ProfileSuggestionRepository profileSuggestionRepository;
    @Mock private com.resumeai.common.AsyncOperationRepository asyncOperationRepository;
    @Mock private EmbeddingService embeddingService;
    @Mock private AiDecisionLogRepository aiDecisionLogRepository;
    @Mock private InterviewKitRepository interviewKitRepository;
    @Mock private MockInterviewRepositories mockInterviewRepositories;
    @Mock private MockInterviewSessionRepository mockInterviewSessionRepository;
    @Mock private MockInterviewQuestionRepository mockInterviewQuestionRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @Mock private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock private ChatClient.CallResponseSpec callResponseSpec;

    private AiService aiService;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);

        aiService = new AiService(
                chatClientBuilder, resumeRepository, new ObjectMapper(),
                tailoringHistoryRepository, jobPostingRepository, candidateProfileRepository,
                candidateMatchRepository, profileSuggestionRepository, asyncOperationRepository,
                embeddingService, aiDecisionLogRepository, interviewKitRepository,
                mockInterviewRepositories, messagingTemplate
        );
        aiService.setSelf(aiService);
    }

    private void stubChatChain() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(java.util.function.Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.user(any(java.util.function.Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.options(any())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
    }

    private CandidateProfile candidateWithUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        CandidateProfile candidate = new CandidateProfile();
        candidate.setId(UUID.randomUUID());
        candidate.setUser(user);
        candidate.setHeadline("Backend Engineer");
        candidate.setSkills(List.of("Java", "Spring Boot"));
        return candidate;
    }

    @Test
    void matching_usesVectorPrefilter_andPersistsMatchPerCandidate() throws Exception {
        JobPosting job = new JobPosting();
        job.setId(UUID.randomUUID());
        job.setTitle("Senior Java Engineer");
        job.setRequiredSkills(List.of("Java", "Spring Boot"));
        job.setDescription("Build backend services.");
        when(jobPostingRepository.findById(job.getId())).thenReturn(Optional.of(job));

        CandidateProfile candidate = candidateWithUser();
        when(embeddingService.jobHasEmbedding(job.getId())).thenReturn(true);
        when(embeddingService.findNearestCandidateIds(job.getId(), 30)).thenReturn(List.of(candidate.getId()));
        when(candidateProfileRepository.findAllById(List.of(candidate.getId())))
                .thenReturn(new java.util.ArrayList<>(List.of(candidate)));

        Resume resume = new Resume();
        resume.setId(UUID.randomUUID());
        resume.setExtractedText("5 years of Java and Spring Boot experience.");
        when(resumeRepository.findFirstByCandidateIdAndIsPrimaryTrue(candidate.getId())).thenReturn(Optional.of(resume));

        stubChatChain();
        CandidateMatchResultDto result = new CandidateMatchResultDto(
                82, "Strong Java/Spring Boot background.", List.of("Java", "Spring Boot"), List.of("Kubernetes"), "5 years backend");
        when(callResponseSpec.entity(CandidateMatchResultDto.class)).thenReturn(result);

        when(candidateMatchRepository.findByJobPostingIdAndCandidateId(job.getId(), candidate.getId())).thenReturn(Optional.empty());

        aiService.doMatchCandidatesWithRetry(job.getId());

        // The keyword-overlap fallback must NOT run once vector candidates are found.
        verify(candidateProfileRepository, never()).findByOpenToOpportunitiesTrue();

        ArgumentCaptor<CandidateMatch> matchCaptor = ArgumentCaptor.forClass(CandidateMatch.class);
        verify(candidateMatchRepository).save(matchCaptor.capture());
        assertEquals(82, matchCaptor.getValue().getMatchScore());
        assertEquals(List.of("Kubernetes"), matchCaptor.getValue().getIdentifiedGaps());

        ArgumentCaptor<AiDecisionLog> logCaptor = ArgumentCaptor.forClass(AiDecisionLog.class);
        verify(aiDecisionLogRepository).save(logCaptor.capture());
        assertEquals("MATCHING", logCaptor.getValue().getDecisionType());
        assertEquals(job.getId(), logCaptor.getValue().getReferenceId());
    }

    @Test
    void matching_fallsBackToKeywordOverlap_whenNoEmbeddingsExist() throws Exception {
        JobPosting job = new JobPosting();
        job.setId(UUID.randomUUID());
        job.setTitle("Senior Java Engineer");
        job.setRequiredSkills(List.of("Java"));
        job.setDescription("Build backend services.");
        when(jobPostingRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(embeddingService.jobHasEmbedding(job.getId())).thenReturn(false);
        when(candidateProfileRepository.findByOpenToOpportunitiesTrue()).thenReturn(List.of());

        aiService.doMatchCandidatesWithRetry(job.getId());

        verify(candidateProfileRepository).findByOpenToOpportunitiesTrue();
        verify(candidateMatchRepository, never()).save(any());
    }

    @Test
    void generateInterviewKit_persistsQuestionsAndLogsDecision() throws Exception {
        JobPosting job = new JobPosting();
        job.setId(UUID.randomUUID());
        job.setTitle("Senior Java Engineer");
        job.setRequiredSkills(List.of("Java", "Kubernetes"));
        job.setDescription("Build backend services.");

        CandidateProfile candidate = candidateWithUser();
        CandidateMatch match = new CandidateMatch();
        match.setId(UUID.randomUUID());
        match.setJobPosting(job);
        match.setCandidate(candidate);
        match.setMatchingSkills(List.of("Java"));
        match.setIdentifiedGaps(List.of("Kubernetes"));
        when(candidateMatchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(interviewKitRepository.findByCandidateMatchId(match.getId())).thenReturn(Optional.empty());

        stubChatChain();
        InterviewKitResponse response = new InterviewKitResponse(List.of(
                new InterviewQuestionDto("Kubernetes", "Describe rolling out a stateful workload on Kubernetes.", "Mentions StatefulSets, PDBs, rollout strategy")
        ));
        when(callResponseSpec.entity(InterviewKitResponse.class)).thenReturn(response);

        InterviewKitResponse returned = aiService.generateInterviewKit(match.getId());

        assertEquals(1, returned.questions().size());
        assertEquals("Kubernetes", returned.questions().get(0).targetSkill());

        ArgumentCaptor<com.resumeai.recruiter.InterviewKit> kitCaptor = ArgumentCaptor.forClass(com.resumeai.recruiter.InterviewKit.class);
        verify(interviewKitRepository).save(kitCaptor.capture());
        assertTrue(kitCaptor.getValue().getQuestions().contains("Kubernetes"));

        verify(aiDecisionLogRepository).save(argThat(log -> "INTERVIEW_KIT".equals(log.getDecisionType())));
    }

    @Test
    void startMockInterview_rejectsNonAmberTier() {
        TailoringHistory history = new TailoringHistory();
        history.setId(UUID.randomUUID());
        history.setCompatibilityTier("GREEN");
        when(tailoringHistoryRepository.findById(history.getId())).thenReturn(Optional.of(history));

        assertThrows(IllegalStateException.class, () -> aiService.startMockInterview(history.getId()));
        verifyNoInteractions(chatClient);
    }

    @Test
    void startMockInterview_generatesOneQuestionPerMissingSkill() throws Exception {
        CandidateProfile candidate = candidateWithUser();
        Resume resume = new Resume();
        resume.setId(UUID.randomUUID());
        resume.setCandidate(candidate);

        TailoringHistory history = new TailoringHistory();
        history.setId(UUID.randomUUID());
        history.setCompatibilityTier("AMBER");
        history.setResume(resume);
        history.setJobDescription("Build backend services requiring Kubernetes.");
        history.setCompatibilityAnalysis(new ObjectMapper().writeValueAsString(
                new CompatibilityAnalysisDto(55, List.of("Java"), List.of("Kubernetes"), List.of(), 1, true, "Solid core, missing ops depth.")
        ));
        when(tailoringHistoryRepository.findById(history.getId())).thenReturn(Optional.of(history));

        when(mockInterviewRepositories.sessionRepository()).thenReturn(mockInterviewSessionRepository);
        when(mockInterviewRepositories.questionRepository()).thenReturn(mockInterviewQuestionRepository);

        stubChatChain();
        MockInterviewQuestionsResponse generated = new MockInterviewQuestionsResponse(List.of(
                new MockInterviewQuestionGenDto("Kubernetes", "How would you debug a CrashLoopBackOff pod?", List.of("Checks logs", "Checks resource limits"))
        ));
        when(callResponseSpec.entity(MockInterviewQuestionsResponse.class)).thenReturn(generated);

        when(mockInterviewSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mockInterviewQuestionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var sessionDto = aiService.startMockInterview(history.getId());

        assertEquals(1, sessionDto.questions().size());
        assertEquals("Kubernetes", sessionDto.questions().get(0).targetSkill());
        verify(mockInterviewQuestionRepository).save(argThat(q -> q.getTargetSkill().equals("Kubernetes")));
    }

    @Test
    void submitMockInterviewAnswer_persistsScoreAndFeedback() throws Exception {
        MockInterviewQuestion question = new MockInterviewQuestion();
        question.setId(UUID.randomUUID());
        question.setQuestion("How would you debug a CrashLoopBackOff pod?");
        question.setIdealTalkingPoints(new ObjectMapper().writeValueAsString(List.of("Checks logs", "Checks resource limits")));
        com.resumeai.candidate.MockInterviewSession session = new com.resumeai.candidate.MockInterviewSession();
        session.setCandidate(candidateWithUser());
        question.setSession(session);
        when(mockInterviewRepositories.questionRepository()).thenReturn(mockInterviewQuestionRepository);
        when(mockInterviewQuestionRepository.findById(question.getId())).thenReturn(Optional.of(question));
        when(mockInterviewQuestionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        stubChatChain();
        when(callResponseSpec.entity(MockInterviewFeedbackResponse.class))
                .thenReturn(new MockInterviewFeedbackResponse(40, "You didn't mention checking resource limits or recent deploys."));

        var dto = aiService.submitMockInterviewAnswer(question.getId(), "I'd check the pod logs.");

        assertEquals(40, dto.score());
        assertEquals("I'd check the pod logs.", dto.candidateAnswer());
        verify(aiDecisionLogRepository).save(argThat(log -> "MOCK_INTERVIEW_FEEDBACK".equals(log.getDecisionType())));
    }
}
