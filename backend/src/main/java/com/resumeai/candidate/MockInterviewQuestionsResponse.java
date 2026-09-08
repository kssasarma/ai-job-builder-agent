package com.resumeai.candidate;

import java.util.List;

public record MockInterviewQuestionsResponse(
        List<MockInterviewQuestionGenDto> questions
) {}
