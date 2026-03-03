package com.resumeai.common;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AsyncOperationRepository extends JpaRepository<AsyncOperation, UUID> {
    Optional<AsyncOperation> findByReferenceIdAndType(UUID referenceId, String type);
}
