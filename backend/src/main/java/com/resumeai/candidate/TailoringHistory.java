package com.resumeai.candidate;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.UUID;
import java.time.LocalDateTime;

@Entity
@Table(name = "tailoring_history")
public class TailoringHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @Column(name = "job_description", nullable = false, columnDefinition = "TEXT")
    private String jobDescription;

    @Column(name = "compatibility_score")
    private Integer compatibilityScore;

    @Column(name = "compatibility_tier")
    private String compatibilityTier; // GREEN, AMBER, RED

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "compatibility_analysis", columnDefinition = "jsonb")
    private String compatibilityAnalysis;

    @Column(name = "tailored_content", columnDefinition = "TEXT")
    private String tailoredContent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "changes_made", columnDefinition = "jsonb")
    private String changesMade;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Resume getResume() { return resume; }
    public void setResume(Resume resume) { this.resume = resume; }
    public String getJobDescription() { return jobDescription; }
    public void setJobDescription(String jobDescription) { this.jobDescription = jobDescription; }
    public Integer getCompatibilityScore() { return compatibilityScore; }
    public void setCompatibilityScore(Integer compatibilityScore) { this.compatibilityScore = compatibilityScore; }
    public String getCompatibilityTier() { return compatibilityTier; }
    public void setCompatibilityTier(String compatibilityTier) { this.compatibilityTier = compatibilityTier; }
    public String getCompatibilityAnalysis() { return compatibilityAnalysis; }
    public void setCompatibilityAnalysis(String compatibilityAnalysis) { this.compatibilityAnalysis = compatibilityAnalysis; }
    public String getTailoredContent() { return tailoredContent; }
    public void setTailoredContent(String tailoredContent) { this.tailoredContent = tailoredContent; }
    public String getChangesMade() { return changesMade; }
    public void setChangesMade(String changesMade) { this.changesMade = changesMade; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
