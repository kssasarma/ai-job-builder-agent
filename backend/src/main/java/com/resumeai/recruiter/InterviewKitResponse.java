package com.resumeai.recruiter;

import java.util.List;

public record InterviewKitResponse(
        List<InterviewQuestionDto> questions
) {}
