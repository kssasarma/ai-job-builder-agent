package com.resumeai.recruiter;

import com.resumeai.candidate.CandidateProfile;
import jakarta.persistence.*;
import java.util.UUID;
import java.util.List;
import java.time.LocalDateTime;

@Entity
@Table(name = "candidate_matches")
public class CandidateMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "job_posting_id", nullable = false)
    private JobPosting jobPosting;

    @ManyToOne
    @JoinColumn(name = "candidate_id", nullable = false)
    private CandidateProfile candidate;

    @Column(name = "match_score")
    private Integer matchScore;

    @Column(name = "match_reasoning", columnDefinition = "TEXT")
    private String matchReasoning;

    @ElementCollection
    @CollectionTable(name = "match_matching_skills", joinColumns = @JoinColumn(name = "match_id"))
    @Column(name = "skill")
    private List<String> matchingSkills;

    @ElementCollection
    @CollectionTable(name = "match_identified_gaps", joinColumns = @JoinColumn(name = "match_id"))
    @Column(name = "gap")
    private List<String> identifiedGaps;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public JobPosting getJobPosting() { return jobPosting; }
    public void setJobPosting(JobPosting jobPosting) { this.jobPosting = jobPosting; }
    public CandidateProfile getCandidate() { return candidate; }
    public void setCandidate(CandidateProfile candidate) { this.candidate = candidate; }
    public Integer getMatchScore() { return matchScore; }
    public void setMatchScore(Integer matchScore) { this.matchScore = matchScore; }
    public String getMatchReasoning() { return matchReasoning; }
    public void setMatchReasoning(String matchReasoning) { this.matchReasoning = matchReasoning; }
    public List<String> getMatchingSkills() { return matchingSkills; }
    public void setMatchingSkills(List<String> matchingSkills) { this.matchingSkills = matchingSkills; }
    public List<String> getIdentifiedGaps() { return identifiedGaps; }
    public void setIdentifiedGaps(List<String> identifiedGaps) { this.identifiedGaps = identifiedGaps; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
