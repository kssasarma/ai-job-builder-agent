import { useState } from "react";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "../ui/card";
import { Button } from "../ui/button";
import { Badge } from "../ui/badge";
import { Loader2, TrendingUp, ShieldCheck } from "lucide-react";
import apiClient from "../../lib/axios";
import { toast } from "sonner";

interface GrowthPanelProps {
  skills: string[];
}

interface Challenge {
  id: string;
  skill: string;
  question: string;
  score: number | null;
  passed: boolean | null;
  feedback: string | null;
}

export function GrowthPanel({ skills }: GrowthPanelProps) {
  const [selectedSkill, setSelectedSkill] = useState("");
  const [challenge, setChallenge] = useState<Challenge | null>(null);
  const [answer, setAnswer] = useState("");
  const [starting, setStarting] = useState(false);
  const [grading, setGrading] = useState(false);

  const [trajectory, setTrajectory] = useState<any>(null);
  const [trajectoryLoading, setTrajectoryLoading] = useState(false);
  const [trajectoryError, setTrajectoryError] = useState<string | null>(null);

  const startChallenge = async (skill: string) => {
    setSelectedSkill(skill);
    setStarting(true);
    setChallenge(null);
    setAnswer("");
    try {
      const res = await apiClient.post("/candidate/skill-challenges", { skill });
      setChallenge(res.data);
    } catch {
      toast.error("Couldn't generate a challenge for that skill.");
    } finally {
      setStarting(false);
    }
  };

  const submitAnswer = async () => {
    if (!challenge || !answer.trim()) return;
    setGrading(true);
    try {
      const res = await apiClient.post(`/candidate/skill-challenges/${challenge.id}/answer`, { answer });
      setChallenge(res.data);
    } catch {
      toast.error("Couldn't grade that answer.");
    } finally {
      setGrading(false);
    }
  };

  const loadTrajectory = async () => {
    setTrajectoryLoading(true);
    setTrajectoryError(null);
    try {
      const res = await apiClient.get("/candidate/trajectory");
      setTrajectory(res.data);
    } catch (error: any) {
      setTrajectoryError(error.response?.data || "Run a couple more compatibility checks first.");
    } finally {
      setTrajectoryLoading(false);
    }
  };

  return (
    <div className="grid md:grid-cols-2 gap-6">
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2"><ShieldCheck className="h-4 w-4" /> Prove a Skill</CardTitle>
          <CardDescription>Pass an AI-graded challenge to turn a listed skill into a verified badge.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {!challenge && (
            <div className="flex flex-wrap gap-2">
              {skills.length === 0 && <p className="text-sm text-muted-foreground">Add skills to your profile first.</p>}
              {skills.map(s => (
                <Button key={s} size="sm" variant="outline" onClick={() => startChallenge(s)} disabled={starting}>
                  {starting && selectedSkill === s ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : s}
                </Button>
              ))}
            </div>
          )}

          {challenge && (
            <div className="space-y-3">
              <div>
                <Badge variant="outline" className="mb-2">{challenge.skill}</Badge>
                <p className="text-sm font-medium">{challenge.question}</p>
              </div>
              {challenge.score === null ? (
                <>
                  <textarea
                    className="w-full min-h-[100px] p-3 border rounded-md resize-y bg-background text-sm"
                    value={answer}
                    onChange={e => setAnswer(e.target.value)}
                    placeholder="Answer the scenario above..."
                  />
                  <div className="flex gap-2">
                    <Button size="sm" onClick={submitAnswer} disabled={grading || !answer.trim()}>
                      {grading ? <><Loader2 className="mr-2 h-3.5 w-3.5 animate-spin" /> Grading...</> : "Submit answer"}
                    </Button>
                    <Button size="sm" variant="ghost" onClick={() => setChallenge(null)}>Cancel</Button>
                  </div>
                </>
              ) : (
                <div className="space-y-2">
                  <p className={`font-semibold text-sm ${challenge.passed ? "text-green-600 dark:text-green-400" : "text-amber-600 dark:text-amber-400"}`}>
                    {challenge.passed ? "Passed — skill verified!" : `Not yet — scored ${challenge.score}/100`}
                  </p>
                  <p className="text-sm text-muted-foreground">{challenge.feedback}</p>
                  <Button size="sm" variant="outline" onClick={() => setChallenge(null)}>Try another skill</Button>
                </div>
              )}
            </div>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2"><TrendingUp className="h-4 w-4" /> Career Trajectory</CardTitle>
          <CardDescription>What closing your most common skill gap would be worth, based on your compatibility history.</CardDescription>
        </CardHeader>
        <CardContent>
          {!trajectory && !trajectoryLoading && !trajectoryError && (
            <Button size="sm" variant="outline" onClick={loadTrajectory}>Simulate my trajectory</Button>
          )}
          {trajectoryLoading && <div className="flex justify-center p-4"><Loader2 className="h-6 w-6 animate-spin text-primary" /></div>}
          {trajectoryError && <p className="text-sm text-muted-foreground">{trajectoryError}</p>}
          {trajectory && (
            <div className="space-y-3">
              <div className="flex items-center gap-4">
                <div className="text-center">
                  <p className="text-3xl font-bold tabular-nums text-muted-foreground">{trajectory.currentAverageScore}</p>
                  <p className="text-xs text-muted-foreground">today</p>
                </div>
                <TrendingUp className="h-5 w-5 text-green-600 dark:text-green-400" />
                <div className="text-center">
                  <p className="text-3xl font-bold tabular-nums text-green-600 dark:text-green-400">{trajectory.projectedAverageScore}</p>
                  <p className="text-xs text-muted-foreground">if closed</p>
                </div>
              </div>
              <p className="text-sm"><span className="font-medium">Top gap:</span> {trajectory.topGapSkill} ({trajectory.gapFrequency})</p>
              <p className="text-sm text-muted-foreground">{trajectory.explanation}</p>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
