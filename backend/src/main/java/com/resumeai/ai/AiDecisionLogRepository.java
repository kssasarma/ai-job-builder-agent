package com.resumeai.ai;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AiDecisionLogRepository extends JpaRepository<AiDecisionLog, UUID> {
    List<AiDecisionLog> findByReferenceIdOrderByCreatedAtDesc(UUID referenceId);
}
