package com.resumeai.candidate;

import java.util.List;

public record MockInterviewQuestionGenDto(
        String targetSkill,
        String question,
        List<String> idealTalkingPoints
) {}
