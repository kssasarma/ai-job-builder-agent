package com.resumeai.recruiter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRevealRequestRepository extends JpaRepository<ProfileRevealRequest, UUID> {
    Optional<ProfileRevealRequest> findByCandidateIdAndRecruiterId(UUID candidateId, UUID recruiterId);
    List<ProfileRevealRequest> findByCandidateIdOrderByCreatedAtDesc(UUID candidateId);
    boolean existsByCandidateIdAndRecruiterIdAndStatus(UUID candidateId, UUID recruiterId, String status);
}
