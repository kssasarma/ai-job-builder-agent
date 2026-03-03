package com.resumeai.recruiter;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

public interface CandidateMatchRepository extends JpaRepository<CandidateMatch, UUID> {
    List<CandidateMatch> findByJobPostingIdOrderByMatchScoreDesc(UUID jobPostingId);
    Optional<CandidateMatch> findByJobPostingIdAndCandidateId(UUID jobPostingId, UUID candidateId);
}
