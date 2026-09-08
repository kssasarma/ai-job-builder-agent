package com.resumeai.candidate;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
 * Proof-of-skill micro-credential: an AI-graded scenario challenge for one skill.
 * Passing (score >= {@link #PASSING_SCORE}) appends the skill to
 * CandidateProfile#verifiedSkills — turning a self-reported skill into scored evidence.
 */
@Entity
@Table(name = "skill_challenges")
public class SkillChallenge {

    public static final int PASSING_SCORE = 70;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "candidate_id", nullable = false)
    private CandidateProfile candidate;

    @Column(nullable = false)
    private String skill;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ideal_answer_points", columnDefinition = "jsonb")
    private String idealAnswerPoints;

    @Column(columnDefinition = "TEXT")
    private String answer;

    private Integer score;

    private Boolean passed;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public CandidateProfile getCandidate() { return candidate; }
    public void setCandidate(CandidateProfile candidate) { this.candidate = candidate; }
    public String getSkill() { return skill; }
    public void setSkill(String skill) { this.skill = skill; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getIdealAnswerPoints() { return idealAnswerPoints; }
    public void setIdealAnswerPoints(String idealAnswerPoints) { this.idealAnswerPoints = idealAnswerPoints; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public Boolean getPassed() { return passed; }
    public void setPassed(Boolean passed) { this.passed = passed; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
