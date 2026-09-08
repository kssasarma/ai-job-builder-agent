package com.resumeai.candidate;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillChallengeRepository extends JpaRepository<SkillChallenge, UUID> {
    List<SkillChallenge> findByCandidateIdOrderByCreatedAtDesc(UUID candidateId);
}
