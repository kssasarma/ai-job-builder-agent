package com.resumeai.recruiter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;

public interface JobPostingRepository extends JpaRepository<JobPosting, UUID> {
    Page<JobPosting> findByRecruiterId(UUID recruiterId, Pageable pageable);

    @Query(value = """
        SELECT j.* FROM job_postings j
        WHERE j.status IN ('OPEN', 'ACTIVE')
        AND (
            CAST(:keyword AS TEXT) IS NULL OR
            j.title ILIKE '%' || CAST(:keyword AS TEXT) || '%' OR
            j.company ILIKE '%' || CAST(:keyword AS TEXT) || '%' OR
            j.description ILIKE '%' || CAST(:keyword AS TEXT) || '%'
        )
        AND (
            CAST(:location AS TEXT) IS NULL OR
            j.location ILIKE '%' || CAST(:location AS TEXT) || '%'
        )
        AND (
            CAST(:skillsText AS TEXT) IS NULL OR
            EXISTS (
                SELECT 1 FROM unnest(j.required_skills) s
                WHERE btrim(s) ILIKE ANY(
                    ARRAY(SELECT btrim(elem) FROM unnest(string_to_array(CAST(:skillsText AS TEXT), ',')) AS elem)
                )
            )
        )
        """,
        countQuery = """
        SELECT count(*) FROM job_postings j
        WHERE j.status IN ('OPEN', 'ACTIVE')
        AND (
            CAST(:keyword AS TEXT) IS NULL OR
            j.title ILIKE '%' || CAST(:keyword AS TEXT) || '%' OR
            j.company ILIKE '%' || CAST(:keyword AS TEXT) || '%' OR
            j.description ILIKE '%' || CAST(:keyword AS TEXT) || '%'
        )
        AND (
            CAST(:location AS TEXT) IS NULL OR
            j.location ILIKE '%' || CAST(:location AS TEXT) || '%'
        )
        AND (
            CAST(:skillsText AS TEXT) IS NULL OR
            EXISTS (
                SELECT 1 FROM unnest(j.required_skills) s
                WHERE btrim(s) ILIKE ANY(
                    ARRAY(SELECT btrim(elem) FROM unnest(string_to_array(CAST(:skillsText AS TEXT), ',')) AS elem)
                )
            )
        )
        """,
        nativeQuery = true)
    Page<JobPosting> findOpenJobs(
        @Param("keyword") String keyword,
        @Param("location") String location,
        @Param("skillsText") String skillsText,
        Pageable pageable
    );
}
