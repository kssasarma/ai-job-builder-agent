package com.resumeai.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.resumeai.common.Role;

public record CompleteOAuthRequest(
        @NotBlank(message = "Email is required") @Email(message = "Email format is invalid") String email,
        @NotBlank(message = "Name is required") String name,
        @NotNull(message = "Role is required") Role role,
        @NotBlank(message = "Temporary Token is required") String tempToken
) {
}
