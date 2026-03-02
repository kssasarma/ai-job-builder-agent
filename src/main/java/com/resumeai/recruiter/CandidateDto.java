package com.resumeai.recruiter;

import java.util.List;
import java.util.UUID;

public record CandidateDto(
        UUID candidateId,
        String name,
        String headline,
        List<String> skills,
        String linkedinUrl,
        String preferredContactEmail,
        Integer latestAtsScore
) {
}
