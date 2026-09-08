package com.resumeai.common;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RatingRepository extends JpaRepository<Rating, UUID> {
    List<Rating> findByRateeTypeAndRateeIdOrderByCreatedAtDesc(String rateeType, UUID rateeId);
    boolean existsByRaterUserIdAndRateeTypeAndRateeIdAndJobApplicationId(UUID raterUserId, String rateeType, UUID rateeId, UUID jobApplicationId);

    @Query("SELECT AVG(r.score) FROM Rating r WHERE r.rateeType = :rateeType AND r.rateeId = :rateeId")
    Double averageScore(@Param("rateeType") String rateeType, @Param("rateeId") UUID rateeId);

    long countByRateeTypeAndRateeId(String rateeType, UUID rateeId);
}
