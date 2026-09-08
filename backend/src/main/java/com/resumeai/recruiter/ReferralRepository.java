package com.resumeai.recruiter;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReferralRepository extends JpaRepository<Referral, UUID> {
    List<Referral> findByReferrerIdOrderByCreatedAtDesc(UUID referrerCandidateId);
    List<Referral> findByJobPostingIdOrderByCreatedAtDesc(UUID jobPostingId);
    long countByReferrerId(UUID referrerCandidateId);
    long countByReferrerIdAndStatus(UUID referrerCandidateId, String status);
}
