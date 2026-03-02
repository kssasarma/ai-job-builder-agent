package com.resumeai.recruiter;

import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

public record JobPostingDto(
        UUID id,
        RecruiterProfileDto recruiter,
        String title,
        String company,
        String description,
        List<String> requiredSkills,
        Integer experienceMin,
        Integer experienceMax,
        String location,
        String salaryRange,
        String jobType,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static JobPostingDto fromEntity(JobPosting entity) {
        if (entity == null) return null;
        return new JobPostingDto(
                entity.getId(),
                RecruiterProfileDto.fromEntity(entity.getRecruiter()),
                entity.getTitle(),
                entity.getCompany(),
                entity.getDescription(),
                entity.getRequiredSkills(),
                entity.getExperienceMin(),
                entity.getExperienceMax(),
                entity.getLocation(),
                entity.getSalaryRange(),
                entity.getJobType(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
