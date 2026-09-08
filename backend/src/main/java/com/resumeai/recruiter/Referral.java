package com.resumeai.recruiter;

import java.time.LocalDateTime;
import java.util.UUID;

import com.resumeai.candidate.CandidateProfile;

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
 * Trust-weighted referrals: a candidate refers someone from their network into an
 * open role. The referrer's trust score (see ReferralRepository) is computed on read
 * from how many of their past referrals were actually SELECTED — no separate
 * reputation table needed, it falls out of this table's own history.
 */
@Entity
@Table(name = "referrals")
public class Referral {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "referrer_candidate_id", nullable = false)
    private CandidateProfile referrer;

    @ManyToOne
    @JoinColumn(name = "job_posting_id", nullable = false)
    private JobPosting jobPosting;

    @Column(name = "referred_name", nullable = false)
    private String referredName;

    @Column(name = "referred_email", nullable = false)
    private String referredEmail;

    @Column(nullable = false)
    private String status = "REFERRED";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public CandidateProfile getReferrer() { return referrer; }
    public void setReferrer(CandidateProfile referrer) { this.referrer = referrer; }
    public JobPosting getJobPosting() { return jobPosting; }
    public void setJobPosting(JobPosting jobPosting) { this.jobPosting = jobPosting; }
    public String getReferredName() { return referredName; }
    public void setReferredName(String referredName) { this.referredName = referredName; }
    public String getReferredEmail() { return referredEmail; }
    public void setReferredEmail(String referredEmail) { this.referredEmail = referredEmail; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
