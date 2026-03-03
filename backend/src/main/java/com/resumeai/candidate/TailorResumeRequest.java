package com.resumeai.candidate;

import java.util.UUID;

public record TailorResumeRequest(
        String jobDescription,
        UUID compatibilityId
) {
}
