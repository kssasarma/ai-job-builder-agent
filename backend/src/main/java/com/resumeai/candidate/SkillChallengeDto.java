package com.resumeai.candidate;

import java.util.UUID;

public record SkillChallengeDto(
        UUID id,
        String skill,
        String question,
        String answer,
        Integer score,
        Boolean passed,
        String feedback
) {
    public static SkillChallengeDto fromEntity(SkillChallenge entity, String feedback) {
        return new SkillChallengeDto(entity.getId(), entity.getSkill(), entity.getQuestion(),
                entity.getAnswer(), entity.getScore(), entity.getPassed(), feedback);
    }
}
