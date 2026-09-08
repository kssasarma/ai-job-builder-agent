package com.resumeai.ai;

import org.springframework.stereotype.Component;

import com.resumeai.candidate.MockInterviewQuestionRepository;
import com.resumeai.candidate.MockInterviewSessionRepository;

/**
 * Small constructor-injection bundle so AiService doesn't grow two more individual
 * repository parameters just for the mock-interview feature.
 */
@Component
public class MockInterviewRepositories {

    private final MockInterviewSessionRepository sessionRepository;
    private final MockInterviewQuestionRepository questionRepository;

    public MockInterviewRepositories(MockInterviewSessionRepository sessionRepository,
                                      MockInterviewQuestionRepository questionRepository) {
        this.sessionRepository = sessionRepository;
        this.questionRepository = questionRepository;
    }

    public MockInterviewSessionRepository sessionRepository() { return sessionRepository; }
    public MockInterviewQuestionRepository questionRepository() { return questionRepository; }
}
