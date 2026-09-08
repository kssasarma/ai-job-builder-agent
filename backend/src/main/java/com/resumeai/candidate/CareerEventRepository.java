package com.resumeai.candidate;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CareerEventRepository extends JpaRepository<CareerEvent, UUID> {
    List<CareerEvent> findByCandidateIdOrderByEventDateDesc(UUID candidateId);
}
