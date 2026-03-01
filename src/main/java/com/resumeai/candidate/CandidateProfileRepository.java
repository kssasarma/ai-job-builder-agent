package com.resumeai.candidate;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;

import java.util.List;

public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, UUID> {
    Optional<CandidateProfile> findByUserId(UUID userId);
    List<CandidateProfile> findByOpenToOpportunitiesTrue();
}
