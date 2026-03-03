package com.resumeai.candidate;

import java.util.List;

public record ProfileExtractionResponse(
        String suggestedHeadline,
        List<String> skills,
        String linkedinUrl
) {
}
