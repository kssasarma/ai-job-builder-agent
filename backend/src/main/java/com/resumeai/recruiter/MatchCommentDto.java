package com.resumeai.recruiter;

import java.time.LocalDateTime;
import java.util.UUID;

public record MatchCommentDto(
        UUID id,
        String authorName,
        String body,
        LocalDateTime createdAt
) {
    public static MatchCommentDto fromEntity(MatchComment entity) {
        return new MatchCommentDto(
                entity.getId(),
                entity.getAuthor() != null ? entity.getAuthor().getName() : "Unknown",
                entity.getBody(),
                entity.getCreatedAt()
        );
    }
}
