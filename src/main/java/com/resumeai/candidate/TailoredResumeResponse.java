package com.resumeai.candidate;

import java.util.List;

public record TailoredResumeResponse(
        String tailoredContent,
        List<ChangeMade> changesMade,
        List<String> keywordsIncorporated
) {
    public record ChangeMade(String original, String revised, String reason) {}
}
