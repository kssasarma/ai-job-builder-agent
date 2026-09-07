package com.resumeai.candidate;

import java.util.UUID;

public record MockInterviewQuestionDto(
        UUID id,
        String targetSkill,
        String question,
        String candidateAnswer,
        String feedback,
        Integer score
) {
    public static MockInterviewQuestionDto fromEntity(MockInterviewQuestion q) {
        return new MockInterviewQuestionDto(q.getId(), q.getTargetSkill(), q.getQuestion(),
                q.getCandidateAnswer(), q.getFeedback(), q.getScore());
    }
}
