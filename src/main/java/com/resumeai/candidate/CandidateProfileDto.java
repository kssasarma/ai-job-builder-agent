package com.resumeai.candidate;

import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

public record CandidateProfileDto(
        UUID id,
        String headline,
        String linkedinUrl,
        String preferredContactEmail,
        Boolean openToOpportunities,
        List<String> skills,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CandidateProfileDto fromEntity(CandidateProfile entity) {
        if (entity == null) return null;
        return new CandidateProfileDto(
                entity.getId(),
                entity.getHeadline(),
                entity.getLinkedinUrl(),
                entity.getPreferredContactEmail(),
                entity.getOpenToOpportunities(),
                entity.getSkills(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
