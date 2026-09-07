import { useState } from "react";
import { ChevronDown, ChevronUp, ShieldQuestion } from "lucide-react";
import { Button } from "../ui/button";
import apiClient from "../../lib/axios";
import { toast } from "sonner";

export type DecisionType = "SCORING" | "COMPATIBILITY" | "GAP_ANALYSIS" | "TAILORING" | "MATCHING" | "INTERVIEW_KIT";

interface DecisionLog {
  id: string;
  model: string;
  promptVersion: string;
  summary: string;
  createdAt: string;
}

interface ExplainabilityPanelProps {
  decisionType: DecisionType;
  referenceId: string;
}

/**
 * "Why this result?" — reads the AI decision ledger for one score/match/generated
 * document, and lets the viewer flag it for a human to look at instead of just
 * accepting the number. Reusable across ATS scoring, compatibility, gap analysis,
 * tailoring, candidate matching, and interview kits — decisionType picks which.
 */
export function ExplainabilityPanel({ decisionType, referenceId }: ExplainabilityPanelProps) {
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [logs, setLogs] = useState<DecisionLog[] | null>(null);
  const [reason, setReason] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [requested, setRequested] = useState(false);

  const toggle = async () => {
    const next = !open;
    setOpen(next);
    if (next && logs === null) {
      setLoading(true);
      try {
        const res = await apiClient.get(`/ai/decisions/${decisionType}/${referenceId}`);
        setLogs(res.data);
      } catch {
        toast.error("Couldn't load the reasoning behind this result.");
        setLogs([]);
      } finally {
        setLoading(false);
      }
    }
  };

  const requestReview = async () => {
    setSubmitting(true);
    try {
      await apiClient.post("/ai/review-requests", { subjectType: decisionType, subjectId: referenceId, reason });
      setRequested(true);
      toast.success("Sent for human review.");
    } catch {
      toast.error("Couldn't submit the review request.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="border rounded-md text-sm">
      <button
        onClick={toggle}
        className="w-full flex items-center justify-between px-3 py-2 text-muted-foreground hover:text-foreground transition-colors"
      >
        <span className="flex items-center gap-2">
          <ShieldQuestion className="h-4 w-4" /> Why this result?
        </span>
        {open ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
      </button>
      {open && (
        <div className="px-3 pb-3 space-y-3 border-t pt-3">
          {loading && <p className="text-muted-foreground">Loading...</p>}
          {!loading && logs && logs.length === 0 && (
            <p className="text-muted-foreground">No reasoning has been recorded for this yet.</p>
          )}
          {!loading &&
            logs?.map((log) => (
              <div key={log.id} className="bg-muted rounded-md p-3 space-y-1">
                <p>{log.summary}</p>
                <p className="text-xs text-muted-foreground">
                  {log.model} · {log.promptVersion} · {new Date(log.createdAt).toLocaleString()}
                </p>
              </div>
            ))}

          {!requested ? (
            <div className="space-y-2 pt-2 border-t">
              <textarea
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                placeholder="Optional: tell us what looks wrong (helps a human review it faster)"
                rows={2}
                className="flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
              />
              <Button size="sm" variant="outline" onClick={requestReview} disabled={submitting}>
                {submitting ? "Submitting..." : "Request human review"}
              </Button>
            </div>
          ) : (
            <p className="text-green-600 dark:text-green-400">A human will take a look — thanks for flagging it.</p>
          )}
        </div>
      )}
    </div>
  );
}
