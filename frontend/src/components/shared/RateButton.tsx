import { useState } from "react";
import { Star, Check } from "lucide-react";
import { Button } from "../ui/button";
import apiClient from "../../lib/axios";
import { toast } from "sonner";

interface RateButtonProps {
  endpoint: string;
  jobApplicationId: string;
  label: string;
}

/** Two-sided reputation: a small star picker used both for candidate-rates-recruiter and recruiter-rates-candidate. */
export function RateButton({ endpoint, jobApplicationId, label }: RateButtonProps) {
  const [open, setOpen] = useState(false);
  const [score, setScore] = useState(0);
  const [hovered, setHovered] = useState(0);
  const [comment, setComment] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [done, setDone] = useState(false);

  const submit = async () => {
    if (score === 0) return;
    setSubmitting(true);
    try {
      await apiClient.post(endpoint, { jobApplicationId, score, comment });
      setDone(true);
      setOpen(false);
      toast.success("Thanks for the feedback!");
    } catch (error: any) {
      if (error.response?.status === 409) {
        setDone(true);
        setOpen(false);
      } else {
        toast.error("Couldn't submit your rating.");
      }
    } finally {
      setSubmitting(false);
    }
  };

  if (done) {
    return <span className="text-xs text-muted-foreground flex items-center gap-1"><Check className="h-3 w-3" /> Rated</span>;
  }

  if (!open) {
    return <Button size="sm" variant="ghost" onClick={() => setOpen(true)}>{label}</Button>;
  }

  return (
    <div className="flex flex-col gap-2 p-3 border rounded-md bg-muted/20 w-full sm:w-64">
      <div className="flex gap-1">
        {[1, 2, 3, 4, 5].map(n => (
          <button key={n} onClick={() => setScore(n)} onMouseEnter={() => setHovered(n)} onMouseLeave={() => setHovered(0)}>
            <Star className={`h-5 w-5 ${(hovered || score) >= n ? "fill-amber-400 text-amber-400" : "text-muted-foreground"}`} />
          </button>
        ))}
      </div>
      <input
        value={comment}
        onChange={e => setComment(e.target.value)}
        placeholder="Optional comment"
        className="text-xs rounded border border-input bg-background px-2 py-1"
      />
      <div className="flex gap-2">
        <Button size="sm" onClick={submit} disabled={score === 0 || submitting}>Submit</Button>
        <Button size="sm" variant="ghost" onClick={() => setOpen(false)}>Cancel</Button>
      </div>
    </div>
  );
}
