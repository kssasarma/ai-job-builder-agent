package com.resumeai.candidate;

import java.util.List;

public record CandidateProfileUpdateRequest(
        String headline,
        String linkedinUrl,
        String preferredContactEmail,
        Boolean openToOpportunities,
        List<String> skills
) {
}
