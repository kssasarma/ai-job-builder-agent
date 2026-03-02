package com.resumeai.auth;

import java.util.UUID;

public record AuthResponse(
        String token,
        String refreshToken,
        Long expiresIn,
        String tokenType,
        UUID id,
        String name,
        String email,
        String role
) {
}
