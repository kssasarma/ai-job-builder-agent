package com.resumeai.recruiter;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchCommentRepository extends JpaRepository<MatchComment, UUID> {
    List<MatchComment> findByCandidateMatchIdOrderByCreatedAtAsc(UUID candidateMatchId);
}
