package com.resumeai.recruiter;

import java.util.UUID;

public record ReferralCreateRequest(UUID jobPostingId, String referredName, String referredEmail) {}
