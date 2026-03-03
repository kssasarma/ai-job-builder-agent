package com.resumeai.candidate;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.UUID;
import java.util.List;
import java.time.LocalDateTime;

@Entity
@Table(name = "profile_suggestions")
public class ProfileSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "resume_id", nullable = false, unique = true)
    private Resume resume;

    @ManyToOne
    @JoinColumn(name = "candidate_id", nullable = false)
    private CandidateProfile candidate;

    @Column(name = "suggested_headline")
    private String suggestedHeadline;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "suggested_skills", columnDefinition = "jsonb")
    private List<String> suggestedSkills;

    @Column(name = "suggested_linkedin_url")
    private String suggestedLinkedinUrl;

    @Column(name = "status")
    private String status = "PENDING"; // PENDING, APPLIED, DISMISSED

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Resume getResume() { return resume; }
    public void setResume(Resume resume) { this.resume = resume; }
    public CandidateProfile getCandidate() { return candidate; }
    public void setCandidate(CandidateProfile candidate) { this.candidate = candidate; }
    public String getSuggestedHeadline() { return suggestedHeadline; }
    public void setSuggestedHeadline(String suggestedHeadline) { this.suggestedHeadline = suggestedHeadline; }
    public List<String> getSuggestedSkills() { return suggestedSkills; }
    public void setSuggestedSkills(List<String> suggestedSkills) { this.suggestedSkills = suggestedSkills; }
    public String getSuggestedLinkedinUrl() { return suggestedLinkedinUrl; }
    public void setSuggestedLinkedinUrl(String suggestedLinkedinUrl) { this.suggestedLinkedinUrl = suggestedLinkedinUrl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
