package com.resumeai.candidate;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MockInterviewQuestionRepository extends JpaRepository<MockInterviewQuestion, UUID> {
    List<MockInterviewQuestion> findBySessionIdOrderByDisplayOrderAsc(UUID sessionId);
}
