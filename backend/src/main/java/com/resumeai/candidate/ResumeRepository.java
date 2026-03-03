package com.resumeai.candidate;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

public interface ResumeRepository extends JpaRepository<Resume, UUID> {
    List<Resume> findByCandidateId(UUID candidateId);
    Optional<Resume> findFirstByCandidateIdAndIsPrimaryTrue(UUID candidateId);
}
