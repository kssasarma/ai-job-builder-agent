# Platform Strategy

ResumeAI's differentiation isn't "an ATS with a chatbot bolted on" — it's a trust
engine: every AI score is explainable, every gap has a prescribed next action, and
matching stays cheap and fast no matter how many candidates are on the platform. This
document tracks the roadmap and what's actually shipped against it.

## Shipped in this PR (Q4 2026 / Foundation phase)

**Architecture**
- **Vector-first matching** — `EmbeddingService` embeds every resume and job posting
  (pgvector, `text-embedding-3-small`). `AiService#selectCandidatesForMatching` ranks
  candidates by cosine distance and only spends an LLM call on the closest 30, instead
  of the old per-candidate loop over every open candidate. Falls back to the original
  keyword-overlap heuristic when embeddings aren't available yet (e.g. resumes
  uploaded before this migration).
- **Real-time status over WebSocket** — `WebSocketConfig` + `SimpMessagingTemplate`
  push `AsyncOperation` state changes to `/topic/operations/{referenceId}`. The
  frontend (`useOperationStatus` in `frontend/src/lib/websocket.ts`) subscribes
  instead of polling `getScoringStatus()`/`getMatchingStatus()` on an interval.
- **Tiered model routing** — matching pre-rank and interview-kit generation run on a
  cheaper/faster model (`app.ai.fast-model`, default `gpt-4o-mini`); gap analysis,
  tailoring, and ATS scoring keep the default GPT-4o for reasoning quality.
- **Prompt regression tests** — `AiServiceMatchingAndGenerationTest` locks in the
  JSON-shape and persistence behavior of matching, interview-kit generation, and the
  mock interview flows against a mocked `ChatClient`.

**Candidate-facing**
- **Score Explainability Panel** — every ATS score, compatibility analysis, gap
  analysis, and tailoring result has a "why this result?" panel (`ExplainabilityPanel`
  in the frontend) reading the AI decision ledger, plus a one-click "request human
  review."
- **Gap-Aware Mock Interview** — `/api/candidate/mock-interview` turns an AMBER-tier
  compatibility analysis into practice questions targeting exactly the candidate's
  missing skills, with AI-scored feedback per answer.

**Recruiter-facing**
- **Auto-generated interview kit** — `/api/recruiter/matches/{id}/interview-kit`
  generates standardized, gap-targeted interview questions per shortlisted candidate.

**Trust**
- **AI decision ledger** (`AiDecisionLog`) — every scoring/compatibility/gap-analysis/
  tailoring/matching/interview-kit decision is logged with model, prompt version, and
  a plain-language summary, scoped by ownership so only the candidate or recruiter
  who has legitimate access to the underlying resource can read it.
- **Review requests** (`ReviewRequest`) — the other half of explainability: flag any
  AI result for a human to look at.

## Not yet built (P1 / P2 — next)

- Funnel & time-to-hire analytics dashboard
- Collaborative hiring rooms (multi-interviewer threaded feedback)
- Proof-of-skill micro-credentials
- Two-sided reputation (candidate rates employer, and vice versa)
- Fairness dashboard with compliance export
- Anonymized-first candidate discovery
- Career record / gap-to-learning bridge / trust-weighted referrals (the longer-term
  "Career OS" vision)

See the roadmap doc shared with the team for the full feature rationale and phased
timeline; this file tracks only what's true of the code today.
