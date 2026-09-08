package com.resumeai.candidate;

import java.util.List;

public record SkillChallengeQuestionResponse(
        String question,
        List<String> idealAnswerPoints
) {}
