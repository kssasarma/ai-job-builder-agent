package com.resumeai.candidate;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, UUID> {
    Optional<CandidateProfile> findByUserId(UUID userId);
    List<CandidateProfile> findByOpenToOpportunitiesTrue();

    @Query(value = """
        SELECT c.* FROM candidate_profiles c
        WHERE COALESCE(c.open_to_opportunities, false) = true
        AND (
            CAST(:skillsText AS TEXT) IS NULL OR
            EXISTS (
                SELECT 1 FROM unnest(c.skills) s
                WHERE btrim(s) ILIKE ANY(
                    ARRAY(SELECT btrim(elem) FROM unnest(string_to_array(CAST(:skillsText AS TEXT), ',')) AS elem)
                )
            )
        )
        AND (
            :minAtsScore IS NULL OR
            (SELECT max(r.ats_score) FROM resumes r WHERE r.candidate_id = c.id) >= :minAtsScore
        )
        ORDER BY c.created_at DESC
        """,
        countQuery = """
        SELECT count(*) FROM candidate_profiles c
        WHERE COALESCE(c.open_to_opportunities, false) = true
        AND (
            CAST(:skillsText AS TEXT) IS NULL OR
            EXISTS (
                SELECT 1 FROM unnest(c.skills) s
                WHERE btrim(s) ILIKE ANY(
                    ARRAY(SELECT btrim(elem) FROM unnest(string_to_array(CAST(:skillsText AS TEXT), ',')) AS elem)
                )
            )
        )
        AND (
            :minAtsScore IS NULL OR
            (SELECT max(r.ats_score) FROM resumes r WHERE r.candidate_id = c.id) >= :minAtsScore
        )
        """,
        nativeQuery = true)
    Page<CandidateProfile> findCandidates(
        @Param("skillsText") String skillsText,
        @Param("minAtsScore") Integer minAtsScore,
        Pageable pageable
    );
}
