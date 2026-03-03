package com.resumeai.recruiter;

import com.resumeai.candidate.CandidateProfileDto;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

public record CandidateMatchDto(
        UUID id,
        CandidateProfileDto candidate,
        Integer matchScore,
        String matchReasoning,
        List<String> matchingSkills,
        List<String> identifiedGaps,
        LocalDateTime createdAt
) {
    public static CandidateMatchDto fromEntity(CandidateMatch entity) {
        if (entity == null) return null;
        return new CandidateMatchDto(
                entity.getId(),
                CandidateProfileDto.fromEntity(entity.getCandidate()),
                entity.getMatchScore(),
                entity.getMatchReasoning(),
                entity.getMatchingSkills(),
                entity.getIdentifiedGaps(),
                entity.getCreatedAt()
        );
    }
}
