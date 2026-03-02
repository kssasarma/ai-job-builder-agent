package com.resumeai.candidate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CandidateProfileDto(
        UUID id,
        String name,
        String headline,
        String linkedinUrl,
        String preferredContactEmail,
        Boolean openToOpportunities,
        List<String> skills,
        String experienceSummary,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CandidateProfileDto fromEntity(CandidateProfile entity) {
        if (entity == null) return null;
        return new CandidateProfileDto(
                entity.getId(),
                entity.getUser() != null ? entity.getUser().getName() : null,
                entity.getHeadline(),
                entity.getLinkedinUrl(),
                entity.getPreferredContactEmail(),
                entity.getOpenToOpportunities(),
                entity.getSkills(),
                entity.getExperienceSummary(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
