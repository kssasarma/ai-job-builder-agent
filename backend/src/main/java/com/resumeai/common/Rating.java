package com.resumeai.common;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Two-sided reputation: a candidate rates a recruiter/employer, or a recruiter rates
 * a candidate, after a hiring process — most platforms only let one side rate the
 * other. {@code rateeType}/{@code rateeId} name who's being rated the same way
 * {@link com.resumeai.ai.AiDecisionLog} names its subject.
 */
@Entity
@Table(name = "ratings")
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "rater_user_id", nullable = false)
    private UUID raterUserId;

    @Column(name = "ratee_type", nullable = false)
    private String rateeType; // CANDIDATE or RECRUITER

    @Column(name = "ratee_id", nullable = false)
    private UUID rateeId;

    @Column(name = "job_application_id")
    private UUID jobApplicationId;

    @Column(nullable = false)
    private int score;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getRaterUserId() { return raterUserId; }
    public void setRaterUserId(UUID raterUserId) { this.raterUserId = raterUserId; }
    public String getRateeType() { return rateeType; }
    public void setRateeType(String rateeType) { this.rateeType = rateeType; }
    public UUID getRateeId() { return rateeId; }
    public void setRateeId(UUID rateeId) { this.rateeId = rateeId; }
    public UUID getJobApplicationId() { return jobApplicationId; }
    public void setJobApplicationId(UUID jobApplicationId) { this.jobApplicationId = jobApplicationId; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
