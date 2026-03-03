package com.resumeai.candidate;

import java.util.UUID;
import java.time.LocalDateTime;

public record ResumeDto(
        UUID id,
        UUID candidateId,
        Integer atsScore,
        String scoreBreakdown,
        Boolean isPrimary,
        LocalDateTime createdAt
) {
    public static ResumeDto fromEntity(Resume entity) {
        if (entity == null) return null;
        return new ResumeDto(
                entity.getId(),
                entity.getCandidate() != null ? entity.getCandidate().getId() : null,
                entity.getAtsScore(),
                entity.getScoreBreakdown(),
                entity.getPrimary(),
                entity.getCreatedAt()
        );
    }
}
