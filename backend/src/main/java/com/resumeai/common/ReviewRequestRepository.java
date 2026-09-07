package com.resumeai.common;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRequestRepository extends JpaRepository<ReviewRequest, UUID> {
    List<ReviewRequest> findBySubjectTypeAndSubjectIdOrderByCreatedAtDesc(String subjectType, UUID subjectId);
    List<ReviewRequest> findByRequestedByUserIdOrderByCreatedAtDesc(UUID requestedByUserId);
}
