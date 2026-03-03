package com.resumeai.recruiter;

import com.resumeai.candidate.CandidateProfileDto;

import java.time.LocalDateTime;
import java.util.UUID;

public record JobApplicationDto(
        UUID id,
        CandidateProfileDto candidate,
        UUID jobPostingId,
        String status,
        LocalDateTime appliedAt,
        LocalDateTime updatedAt
) {
    public static JobApplicationDto fromEntity(JobApplication entity) {
        if (entity == null) return null;
        return new JobApplicationDto(
                entity.getId(),
                CandidateProfileDto.fromEntity(entity.getCandidate()),
                entity.getJobPosting().getId(),
                entity.getStatus(),
                entity.getAppliedAt(),
                entity.getUpdatedAt()
        );
    }
}
