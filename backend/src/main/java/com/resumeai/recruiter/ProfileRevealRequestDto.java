package com.resumeai.recruiter;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProfileRevealRequestDto(
        UUID id,
        UUID recruiterId,
        String companyName,
        String status,
        LocalDateTime createdAt
) {
    public static ProfileRevealRequestDto fromEntity(ProfileRevealRequest entity) {
        return new ProfileRevealRequestDto(
                entity.getId(),
                entity.getRecruiter().getId(),
                entity.getRecruiter().getCompanyName(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
