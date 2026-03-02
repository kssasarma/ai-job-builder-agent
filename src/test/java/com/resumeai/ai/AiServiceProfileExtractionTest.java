package com.resumeai.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.candidate.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AiServiceProfileExtractionTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private CandidateProfileRepository candidateProfileRepository;

    @Mock
    private ProfileSuggestionRepository profileSuggestionRepository;

    private AiService aiService;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);

        aiService = new AiService(
                chatClientBuilder,
                resumeRepository,
                new ObjectMapper(),
                mock(com.resumeai.candidate.TailoringHistoryRepository.class),
                mock(com.resumeai.recruiter.JobPostingRepository.class),
                candidateProfileRepository,
                mock(com.resumeai.recruiter.CandidateMatchRepository.class),
                profileSuggestionRepository,
                mock(com.resumeai.common.AsyncOperationRepository.class)
        );
        aiService.setSelf(aiService);
    }

    @Test
    void doExtractProfileWithRetry_ShouldPopulateEmptyFieldsAndSaveSuggestion() throws Exception {
        UUID resumeId = UUID.randomUUID();
        CandidateProfile profile = new CandidateProfile();
        profile.setId(UUID.randomUUID());
        // Headline and Skills are intentionally null/empty

        Resume resume = new Resume();
        resume.setId(resumeId);
        resume.setExtractedText("Experienced Java developer");
        resume.setCandidate(profile);

        when(resumeRepository.findById(resumeId)).thenReturn(Optional.of(resume));

        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(Consumer.class))).thenReturn(requestSpec);

        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        when(requestSpec.call()).thenReturn(callResponseSpec);

        ProfileExtractionResponse response = new ProfileExtractionResponse(
                "Java Dev",
                List.of("Java", "Spring"),
                "https://linkedin.com/in/test"
        );
        when(callResponseSpec.entity(ProfileExtractionResponse.class)).thenReturn(response);

        when(profileSuggestionRepository.findByResumeId(resumeId)).thenReturn(Optional.empty());

        aiService.doExtractProfileWithRetry(resumeId);

        // Verify profile was updated
        ArgumentCaptor<CandidateProfile> profileCaptor = ArgumentCaptor.forClass(CandidateProfile.class);
        verify(candidateProfileRepository).save(profileCaptor.capture());
        CandidateProfile savedProfile = profileCaptor.getValue();
        assertEquals("Java Dev", savedProfile.getHeadline());
        assertEquals("https://linkedin.com/in/test", savedProfile.getLinkedinUrl());
        assertEquals(List.of("Java", "Spring"), savedProfile.getSkills());

        // Verify suggestion was saved
        ArgumentCaptor<ProfileSuggestion> suggestionCaptor = ArgumentCaptor.forClass(ProfileSuggestion.class);
        verify(profileSuggestionRepository).save(suggestionCaptor.capture());
        ProfileSuggestion savedSuggestion = suggestionCaptor.getValue();
        assertEquals("Java Dev", savedSuggestion.getSuggestedHeadline());
        assertEquals(List.of("Java", "Spring"), savedSuggestion.getSuggestedSkills());
        assertEquals("PENDING", savedSuggestion.getStatus());
    }

    @Test
    void doExtractProfileWithRetry_ShouldNotOverwriteExistingProfileFields() throws Exception {
        UUID resumeId = UUID.randomUUID();
        CandidateProfile profile = new CandidateProfile();
        profile.setId(UUID.randomUUID());
        profile.setHeadline("Existing Headline");
        profile.setLinkedinUrl("https://existing.url");
        profile.setSkills(List.of("Existing Skill"));

        Resume resume = new Resume();
        resume.setId(resumeId);
        resume.setExtractedText("Experienced Java developer");
        resume.setCandidate(profile);

        when(resumeRepository.findById(resumeId)).thenReturn(Optional.of(resume));

        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(Consumer.class))).thenReturn(requestSpec);

        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        when(requestSpec.call()).thenReturn(callResponseSpec);

        ProfileExtractionResponse response = new ProfileExtractionResponse(
                "Java Dev",
                List.of("Java", "Spring"),
                "https://linkedin.com/in/test"
        );
        when(callResponseSpec.entity(ProfileExtractionResponse.class)).thenReturn(response);

        when(profileSuggestionRepository.findByResumeId(resumeId)).thenReturn(Optional.empty());

        aiService.doExtractProfileWithRetry(resumeId);

        // Verify profile was NOT updated
        verify(candidateProfileRepository, never()).save(any());

        // Verify suggestion WAS saved
        ArgumentCaptor<ProfileSuggestion> suggestionCaptor = ArgumentCaptor.forClass(ProfileSuggestion.class);
        verify(profileSuggestionRepository).save(suggestionCaptor.capture());
        ProfileSuggestion savedSuggestion = suggestionCaptor.getValue();
        assertEquals("Java Dev", savedSuggestion.getSuggestedHeadline());
        assertEquals(List.of("Java", "Spring"), savedSuggestion.getSuggestedSkills());
        assertEquals("PENDING", savedSuggestion.getStatus());
    }
}
