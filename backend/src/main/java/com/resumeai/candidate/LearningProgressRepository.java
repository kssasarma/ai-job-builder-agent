package com.resumeai.candidate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningProgressRepository extends JpaRepository<LearningProgress, UUID> {
    List<LearningProgress> findByTailoringHistoryId(UUID tailoringHistoryId);
    Optional<LearningProgress> findByTailoringHistoryIdAndSkill(UUID tailoringHistoryId, String skill);
}
