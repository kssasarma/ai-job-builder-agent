package com.resumeai.recruiter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewKitRepository extends JpaRepository<InterviewKit, UUID> {
    Optional<InterviewKit> findByCandidateMatchId(UUID candidateMatchId);
}
