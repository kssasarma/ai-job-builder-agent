package com.resumeai.recruiter;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReferralDto(
        UUID id,
        UUID jobPostingId,
        String jobTitle,
        String referredName,
        String referredEmail,
        String status,
        LocalDateTime createdAt,
        UUID referrerCandidateId,
        String referrerName,
        Double referrerTrustScore
) {}
