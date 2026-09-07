package com.resumeai.candidate;

import java.util.List;
import java.util.UUID;

public record MockInterviewSessionDto(
        UUID id,
        String status,
        List<MockInterviewQuestionDto> questions
) {
    public static MockInterviewSessionDto fromEntities(MockInterviewSession session, List<MockInterviewQuestion> questions) {
        return new MockInterviewSessionDto(
                session.getId(),
                session.getStatus(),
                questions.stream().map(MockInterviewQuestionDto::fromEntity).toList()
        );
    }
}
