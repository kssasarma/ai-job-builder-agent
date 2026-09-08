import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "../../components/ui/card";
import { Button } from "../../components/ui/button";
import { Badge } from "../../components/ui/badge";
import { Loader2, ArrowLeft } from "lucide-react";
import apiClient from "../../lib/axios";
import { toast } from "sonner";

interface MockInterviewQuestion {
  id: string;
  targetSkill: string;
  question: string;
  candidateAnswer: string | null;
  feedback: string | null;
  score: number | null;
}

export default function MockInterviewPage() {
  const { historyId } = useParams<{ historyId: string }>();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [questions, setQuestions] = useState<MockInterviewQuestion[]>([]);
  const [drafts, setDrafts] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState<Record<string, boolean>>({});

  useEffect(() => {
    if (!historyId) return;
    apiClient
      .post("/candidate/mock-interview/start", { tailoringHistoryId: historyId })
      .then((res) => setQuestions(res.data.questions))
      .catch((error) => toast.error(error.response?.data || "Couldn't start the practice interview"))
      .finally(() => setLoading(false));
  }, [historyId]);

  const submitAnswer = async (questionId: string) => {
    const answer = drafts[questionId];
    if (!answer?.trim()) return toast.error("Write an answer first");

    setSubmitting((prev) => ({ ...prev, [questionId]: true }));
    try {
      const res = await apiClient.post(`/candidate/mock-interview/questions/${questionId}/answer`, { answer });
      setQuestions((prev) => prev.map((q) => (q.id === questionId ? res.data : q)));
    } catch {
      toast.error("Couldn't score that answer, try again.");
    } finally {
      setSubmitting((prev) => ({ ...prev, [questionId]: false }));
    }
  };

  const scoreColor = (score: number) => {
    if (score >= 70) return "text-green-600 dark:text-green-400";
    if (score >= 40) return "text-amber-600 dark:text-amber-400";
    return "text-red-600 dark:text-red-400";
  };

  return (
    <div className="container mx-auto p-6 max-w-3xl space-y-6">
      <Button variant="ghost" size="sm" onClick={() => navigate(-1)}>
        <ArrowLeft className="mr-2 h-4 w-4" /> Back
      </Button>

      <div>
        <h1 className="text-3xl font-bold tracking-tight">Practice Interview</h1>
        <p className="text-muted-foreground mt-2">
          Questions drilling exactly the skills this job flagged as a gap for you — answer each one to get direct feedback.
        </p>
      </div>

      {loading ? (
        <div className="flex justify-center p-12">
          <Loader2 className="h-8 w-8 animate-spin text-primary" />
        </div>
      ) : questions.length === 0 ? (
        <Card className="p-8 text-center text-muted-foreground">No practice questions available.</Card>
      ) : (
        <div className="space-y-4">
          {questions.map((q) => (
            <Card key={q.id}>
              <CardHeader>
                <Badge variant="outline" className="w-fit mb-2">
                  {q.targetSkill}
                </Badge>
                <CardTitle className="text-lg">{q.question}</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <textarea
                  className="w-full min-h-[120px] p-3 border rounded-md resize-y bg-background text-sm"
                  placeholder="Type your answer here..."
                  defaultValue={q.candidateAnswer ?? ""}
                  onChange={(e) => setDrafts((prev) => ({ ...prev, [q.id]: e.target.value }))}
                  disabled={q.score !== null}
                />
                {q.score === null ? (
                  <Button onClick={() => submitAnswer(q.id)} disabled={submitting[q.id]}>
                    {submitting[q.id] ? (
                      <>
                        <Loader2 className="mr-2 h-4 w-4 animate-spin" /> Scoring...
                      </>
                    ) : (
                      "Submit answer"
                    )}
                  </Button>
                ) : (
                  <CardDescription className="space-y-1">
                    <p className={`font-semibold ${scoreColor(q.score)}`}>Score: {q.score}/100</p>
                    <p>{q.feedback}</p>
                  </CardDescription>
                )}
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
