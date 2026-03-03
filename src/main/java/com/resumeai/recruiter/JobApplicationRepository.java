package com.resumeai.recruiter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {
    Page<JobApplication> findByJobPostingIdOrderByAppliedAtDesc(UUID jobPostingId, Pageable pageable);
    Optional<JobApplication> findByCandidateIdAndJobPostingId(UUID candidateId, UUID jobPostingId);
    List<JobApplication> findByCandidateId(UUID candidateId);
}
