package com.resumeai.candidate;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

import java.util.List;

public interface TailoringHistoryRepository extends JpaRepository<TailoringHistory, UUID> {
    List<TailoringHistory> findByResumeIdOrderByCreatedAtDesc(UUID resumeId);
}
