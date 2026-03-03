package com.resumeai.candidate;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ProfileSuggestionRepository extends JpaRepository<ProfileSuggestion, UUID> {
    Optional<ProfileSuggestion> findByResumeId(UUID resumeId);
}
