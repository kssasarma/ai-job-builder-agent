package com.resumeai.recruiter;

import java.time.LocalDateTime;
import java.util.UUID;

import com.resumeai.auth.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Collaborative hiring rooms: structured, timestamped feedback on one CandidateMatch
 * instead of an email thread. Authored per-user so it's ready for multi-recruiter
 * teams once the data model grows one (today every match's owning recruiter is the
 * only author, since RecruiterProfile is still 1:1 with a User).
 */
@Entity
@Table(name = "match_comments")
public class MatchComment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "candidate_match_id", nullable = false)
    private CandidateMatch candidateMatch;

    @ManyToOne
    @JoinColumn(name = "author_user_id", nullable = false)
    private User author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public CandidateMatch getCandidateMatch() { return candidateMatch; }
    public void setCandidateMatch(CandidateMatch candidateMatch) { this.candidateMatch = candidateMatch; }
    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
