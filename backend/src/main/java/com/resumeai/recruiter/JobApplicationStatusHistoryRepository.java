package com.resumeai.recruiter;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JobApplicationStatusHistoryRepository extends JpaRepository<JobApplicationStatusHistory, UUID> {
    List<JobApplicationStatusHistory> findByJobApplicationIdOrderByChangedAtAsc(UUID jobApplicationId);
    List<JobApplicationStatusHistory> findByJobApplicationJobPostingIdOrderByChangedAtAsc(UUID jobPostingId);
}
