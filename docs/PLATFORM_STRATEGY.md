# Platform Strategy

ResumeAI's differentiation isn't "an ATS with a chatbot bolted on" — it's a trust
engine: every AI score is explainable, every gap has a prescribed next action,
matching stays cheap and fast no matter how many candidates are on the platform, and
the relationship keeps producing value after a single match instead of ending there.
This document tracks the roadmap and what's actually shipped against it.

Every feature from the original strategy doc (P0 through P2) now has a working v1.
What follows is organized by who it's for, with the scope limitations each one
actually has — not just what it does.

## Architecture

- **Vector-first matching** — `EmbeddingService` embeds every resume and job posting
  (pgvector, `text-embedding-3-small`). `AiService#selectCandidatesForMatching` ranks
  candidates by cosine distance and only spends an LLM call on the closest 30, instead
  of the old per-candidate loop over every open candidate. Falls back to the original
  keyword-overlap heuristic when embeddings aren't available yet.
- **Real-time status over WebSocket** — `WebSocketConfig` + `SimpMessagingTemplate`
  push `AsyncOperation` state changes to `/topic/operations/{referenceId}`, replacing
  polling for resume scoring, profile extraction, and candidate matching.
- **Tiered model routing** — matching pre-rank, interview-kit generation, skill
  challenges, and the trajectory simulator run on a cheaper/faster model
  (`app.ai.fast-model`, default `gpt-4o-mini`); gap analysis, tailoring, and ATS
  scoring keep the default GPT-4o for reasoning quality.
- **Prompt regression tests** — `AiServiceMatchingAndGenerationTest` locks in the
  JSON-shape and persistence behavior of the AI flows against a mocked `ChatClient`.

## Candidate-facing

- **Score Explainability Panel** — every ATS score, compatibility analysis, gap
  analysis, and tailoring result has a "why this result?" panel reading the AI
  decision ledger, plus a one-click "request human review."
- **Gap-Aware Mock Interview** (`/api/candidate/mock-interview`) — turns an
  AMBER-tier compatibility analysis into practice questions targeting exactly the
  candidate's missing skills, with AI-scored feedback per answer.
- **Proof-of-Skill Micro-Credentials** (`/api/candidate/skill-challenges`) — an
  AI-graded scenario challenge per skill; passing (score ≥ 70) appends the skill to
  `CandidateProfile.verifiedSkills`, shown as a badge on the profile.
- **Career Trajectory Simulator** (`/api/candidate/trajectory`) — aggregates a
  candidate's `TailoringHistory` (score + missing skills per check) and has the LLM
  project the score uplift from closing the most frequently missing skill. Needs at
  least 2 compatibility checks to have enough history to reason over.
- **Market Reality Check** (`/api/candidate/market-reality`) — a compensation
  benchmark sourced entirely from live postings' free-text `salaryRange` field via
  `SalaryRangeParser`, a best-effort regex parser (numbers + optional k/K suffix).
  It's a heuristic, not exact — postings aren't structured salary data.
- **Gap-to-Learning Bridge** (`/api/candidate/tailoring/{id}/learning-progress`) —
  marks a missing skill from the gap-analysis roadmap as learned; closes the loop
  the gap analysis opens.
- **Career Record** (`/api/candidate/career-events`) — a candidate-maintained
  timeline (promotions, skills gained, certifications) that persists across job
  searches instead of resetting to a blank resume upload.
- **Trust-Weighted Referrals** (`/api/candidate/referrals`) — a candidate refers a
  contact into an open role; their trust score is computed on read as the percentage
  of their past referrals with a terminal outcome that were `SELECTED`.

## Recruiter-facing

- **Auto-generated interview kit** (`/api/recruiter/matches/{id}/interview-kit`) —
  standardized, gap-targeted interview questions per shortlisted candidate.
- **Funnel & time-to-hire analytics** (`/api/recruiter/jobs/{id}/analytics`) — counts
  by application status, average days spent in each stage (derived from
  `JobApplicationStatusHistory`, appended on every status change), and average
  time-to-hire for applications that reached `SELECTED`.
- **Collaborative hiring rooms** (`/api/recruiter/matches/{id}/comments`) —
  threaded, timestamped feedback on one candidate match. **Known scope limit**: a
  `RecruiterProfile` is still 1:1 with a `User`, so today every match's owning
  recruiter is the only author. The comment model is per-user and ready for
  multi-recruiter teams the moment that data model exists — this wasn't built now
  because it needs a `RecruiterTeam`/org-membership model, a bigger schema change
  than this pass scoped for.
- **Fairness / score-distribution dashboard** (`/api/recruiter/jobs/{id}/fairness`)
  — flags when a job's match scores cluster too tightly (std dev below threshold)
  to actually differentiate candidates. **Known scope limit**: this is *not*
  demographic bias auditing (the NYC Local Law 144 kind) — the platform doesn't
  collect candidate demographic data, so that specific compliance feature would
  require a deliberate, separate product decision to start collecting sensitive
  data, not an oversight in this pass.
- **Referral visibility** — recruiters see referred candidates on their job's
  referral list with the referrer's trust score, and can update referral status.

## Trust

- **AI decision ledger** (`AiDecisionLog`) — every scoring/compatibility/gap-analysis/
  tailoring/matching/interview-kit/skill-challenge/trajectory decision is logged with
  model, prompt version, and a plain-language summary, scoped by ownership so only
  the candidate or recruiter with legitimate access to the underlying resource can
  read it.
- **Review requests** (`ReviewRequest`) — the other half of explainability: flag any
  AI result for a human to look at.
- **Two-sided reputation** (`/api/candidate/ratings`, `/api/recruiter/ratings`) —
  candidates rate recruiters/employers and vice versa after a hiring process
  (1-5 stars + comment), one rating per rater per job application. Average shown via
  `RatingSummaryDto`.
- **Consent-based AI data use** (`CandidateProfile.aiConsent`) — a candidate can turn
  off ATS scoring, compatibility analysis, tailoring, profile extraction, and being
  included in candidate-matching runs entirely; `AiService#requireAiConsent` gates
  every entry point.
- **Anonymized-first discovery** (`CandidateProfile.anonymizedDiscovery`,
  `ProfileRevealRequest`) — a candidate can opt into recruiters seeing a skills-only
  card first; name/contact stay masked in `RecruiterCandidateController` until the
  candidate approves a reveal request.

## What's genuinely still open

- Multi-recruiter teams/orgs (the data-model change collaborative hiring rooms is
  waiting on).
- Demographic-data collection + compliance-grade bias auditing (a deliberate,
  separate decision — see the fairness dashboard note above).
- Everything here is a v1: none of these have been run against a live
  Postgres+pgvector instance with a real OpenAI key yet (see the README's pgvector
  prerequisite note) — that's manual verification, not a code gap.
