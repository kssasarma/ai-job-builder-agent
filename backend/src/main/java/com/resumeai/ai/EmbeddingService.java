package com.resumeai.ai;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Owns the pgvector {@code embedding} columns on {@code resumes} and {@code job_postings}.
 * These columns are deliberately NOT mapped as JPA fields — Hibernate has no built-in
 * understanding of the Postgres {@code vector} type, so all reads/writes go through
 * {@link JdbcTemplate} with an explicit {@code ::vector} cast instead of fighting the
 * ORM (and {@code hibernate.hbm2ddl}) over a type it can't represent.
 *
 * <p>This is what lets candidate matching pre-filter by cosine distance instead of
 * calling the LLM once per candidate (see AiService#findMatchCandidates).
 */
@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate jdbcTemplate;

    public EmbeddingService(EmbeddingModel embeddingModel, JdbcTemplate jdbcTemplate) {
        this.embeddingModel = embeddingModel;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Async
    public void embedResumeAsync(UUID resumeId, String extractedText) {
        try {
            storeResumeEmbedding(resumeId, extractedText);
        } catch (Exception e) {
            log.warn("Failed to compute resume embedding for {}: {}", resumeId, e.getMessage());
        }
    }

    @Async
    public void embedJobPostingAsync(UUID jobPostingId, String text) {
        try {
            storeJobPostingEmbedding(jobPostingId, text);
        } catch (Exception e) {
            log.warn("Failed to compute job posting embedding for {}: {}", jobPostingId, e.getMessage());
        }
    }

    public void storeResumeEmbedding(UUID resumeId, String extractedText) {
        if (extractedText == null || extractedText.isBlank()) return;
        String vectorLiteral = toVectorLiteral(embeddingModel.embed(truncate(extractedText)));
        jdbcTemplate.update("UPDATE resumes SET embedding = ?::vector WHERE id = ?", vectorLiteral, resumeId);
    }

    public void storeJobPostingEmbedding(UUID jobPostingId, String text) {
        if (text == null || text.isBlank()) return;
        String vectorLiteral = toVectorLiteral(embeddingModel.embed(truncate(text)));
        jdbcTemplate.update("UPDATE job_postings SET embedding = ?::vector WHERE id = ?", vectorLiteral, jobPostingId);
    }

    /**
     * Candidate IDs whose primary resume embedding is closest (cosine distance) to the
     * job posting's embedding, cheapest-first. Returns an empty list if either side has
     * no embedding yet, so callers can fall back to the keyword pre-filter.
     */
    public List<UUID> findNearestCandidateIds(UUID jobPostingId, int limit) {
        String sql = """
            SELECT r.candidate_id
            FROM resumes r
            JOIN candidate_profiles cp ON cp.id = r.candidate_id
            JOIN job_postings j ON j.id = ?
            WHERE r.is_primary = true
              AND COALESCE(cp.open_to_opportunities, false) = true
              AND r.embedding IS NOT NULL
              AND j.embedding IS NOT NULL
            ORDER BY r.embedding <=> j.embedding
            LIMIT ?
            """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> (UUID) rs.getObject("candidate_id"), jobPostingId, limit);
    }

    public boolean jobHasEmbedding(UUID jobPostingId) {
        Boolean has = jdbcTemplate.queryForObject(
                "SELECT embedding IS NOT NULL FROM job_postings WHERE id = ?", Boolean.class, jobPostingId);
        return Boolean.TRUE.equals(has);
    }

    // OpenAI embedding models cap input around 8k tokens; a resume/JD is comfortably
    // under that, but truncate defensively rather than let a huge upload hard-fail the call.
    private String truncate(String text) {
        int maxChars = 20_000;
        return text.length() > maxChars ? text.substring(0, maxChars) : text;
    }

    private String toVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder(vector.length * 8);
        sb.append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(vector[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
