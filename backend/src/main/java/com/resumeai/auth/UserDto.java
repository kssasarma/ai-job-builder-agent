package com.resumeai.auth;

import com.resumeai.common.Role;
import java.util.UUID;
import java.time.LocalDateTime;

public record UserDto(
        UUID id,
        String email,
        String name,
        Role role,
        String profilePictureUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UserDto fromEntity(User entity) {
        if (entity == null) return null;
        return new UserDto(
                entity.getId(),
                entity.getEmail(),
                entity.getName(),
                entity.getRole(),
                entity.getProfilePictureUrl(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
