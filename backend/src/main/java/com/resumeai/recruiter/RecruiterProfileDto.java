package com.resumeai.recruiter;

import com.resumeai.auth.UserDto;
import java.util.UUID;
import java.time.LocalDateTime;

public record RecruiterProfileDto(
        UUID id,
        UserDto user,
        String companyName,
        String companyWebsite,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static RecruiterProfileDto fromEntity(RecruiterProfile entity) {
        if (entity == null) return null;
        return new RecruiterProfileDto(
                entity.getId(),
                UserDto.fromEntity(entity.getUser()),
                entity.getCompanyName(),
                entity.getCompanyWebsite(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
