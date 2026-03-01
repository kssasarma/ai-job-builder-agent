package com.resumeai.recruiter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface JobPostingRepository extends JpaRepository<JobPosting, UUID> {
    Page<JobPosting> findByRecruiterId(UUID recruiterId, Pageable pageable);
}
