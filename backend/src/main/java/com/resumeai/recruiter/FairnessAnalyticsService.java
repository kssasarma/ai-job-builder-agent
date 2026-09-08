package com.resumeai.recruiter;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

/**
 * Score-distribution fairness check for one job's matches: flags when scores cluster
 * too tightly to actually differentiate candidates, which is the auditable signal
 * available without collecting demographic data. Genuine bias-by-demographic
 * auditing (the kind NYC Local Law 144-style reporting expects) would need
 * candidates to self-report protected-class data, which this platform does not
 * collect — doing so is a deliberate, separate product decision, not an oversight,
 * so this dashboard stays scoped to what's measurable today.
 */
@Service
public class FairnessAnalyticsService {

    private static final double LOW_DIFFERENTIATION_STDDEV_THRESHOLD = 8.0;

    private final CandidateMatchRepository candidateMatchRepository;

    public FairnessAnalyticsService(CandidateMatchRepository candidateMatchRepository) {
        this.candidateMatchRepository = candidateMatchRepository;
    }

    public FairnessAnalyticsResponse computeFairness(UUID jobPostingId) {
        List<CandidateMatch> matches = candidateMatchRepository.findByJobPostingIdOrderByMatchScoreDesc(jobPostingId);
        List<Integer> scores = matches.stream().map(CandidateMatch::getMatchScore).filter(s -> s != null).toList();

        if (scores.isEmpty()) {
            return new FairnessAnalyticsResponse(0, null, null, null, null, null, false,
                    "No scored matches yet — run candidate matching first.");
        }

        double avg = scores.stream().mapToInt(Integer::intValue).average().orElse(0);
        double variance = scores.stream().mapToDouble(s -> Math.pow(s - avg, 2)).average().orElse(0);
        double stdDev = Math.sqrt(variance);

        List<Integer> sorted = scores.stream().sorted().toList();
        double median = sorted.size() % 2 == 0
                ? (sorted.get(sorted.size() / 2 - 1) + sorted.get(sorted.size() / 2)) / 2.0
                : sorted.get(sorted.size() / 2);

        boolean lowDifferentiation = scores.size() >= 5 && stdDev < LOW_DIFFERENTIATION_STDDEV_THRESHOLD;
        String note = lowDifferentiation
                ? "Scores are tightly clustered (std dev " + String.format("%.1f", stdDev)
                        + ") — the model may not be differentiating candidates well for this role. Consider a manual review pass."
                : "Score spread looks healthy for ranking candidates.";

        return new FairnessAnalyticsResponse(
                scores.size(),
                (double) Collections.min(scores),
                (double) Collections.max(scores),
                avg,
                median,
                stdDev,
                lowDifferentiation,
                note
        );
    }
}
