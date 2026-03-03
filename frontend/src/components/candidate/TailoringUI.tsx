import { useState } from "react";
import { Button } from "../../components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "../../components/ui/card";
import { Badge } from "../../components/ui/badge";
import { Loader2, AlertTriangle, XCircle, CheckCircle, RefreshCcw } from "lucide-react";
import apiClient from "../../lib/axios";
import { toast } from "sonner";

interface TailoringUIProps {
  resumeId: string;
}

export default function TailoringUI({ resumeId }: TailoringUIProps) {
  const [jobDescription, setJobDescription] = useState("");
  const [loading, setLoading] = useState(false);
  const [analysis, setAnalysis] = useState<any>(null);
  const [gapAnalysis, setGapAnalysis] = useState<any>(null);
  const [tailoringResult, setTailoringResult] = useState<any>(null);
  const [loadingAction, setLoadingAction] = useState<string | null>(null);

  const handleAnalyze = async () => {
    if (!jobDescription.trim()) return toast.error("Please enter a job description");

    setLoading(true);
    try {
      const res = await apiClient.post(`/candidate/resume/${resumeId}/analyze-compatibility`, {
        jobDescription
      });
      setAnalysis(res.data);

      // Auto-fetch gap analysis if AMBER
      if (res.data.compatibilityTier === "AMBER") {
        fetchGapAnalysis(res.data.historyId);
      }
    } catch (error: any) {
      toast.error(error.response?.data || "Analysis failed");
    } finally {
      setLoading(false);
    }
  };

  const fetchGapAnalysis = async (historyId: string) => {
    try {
      const res = await apiClient.get(`/candidate/tailoring/${historyId}/gap-analysis`);
      setGapAnalysis(res.data);
    } catch (error) {
      console.error("Failed to fetch gap analysis", error);
    }
  };

  const handleTailor = async () => {
    if (!analysis || analysis.compatibilityTier !== "GREEN") return;

    setLoadingAction("tailoring");
    try {
      const res = await apiClient.post(`/candidate/resume/${resumeId}/tailor`, {
        jobDescription,
        compatibilityId: analysis.historyId
      });
      setTailoringResult(res.data);
      toast.success("Resume tailored successfully!");
    } catch (error: any) {
      toast.error(error.response?.data || "Tailoring failed");
    } finally {
      setLoadingAction(null);
    }
  };

  return (
    <div className="space-y-8 mt-12">
      <h2 className="text-2xl font-bold tracking-tight">Intelligent Resume Tailoring</h2>

      <Card>
        <CardHeader>
          <CardTitle>Job Description</CardTitle>
          <CardDescription>Paste the job description here to see how well you match.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <textarea
            className="w-full min-h-[200px] p-3 border rounded-md resize-y bg-background"
            placeholder="Paste Job Description here..."
            value={jobDescription}
            onChange={(e) => setJobDescription(e.target.value)}
          />
          <Button onClick={handleAnalyze} disabled={loading || !jobDescription.trim()}>
            {loading ? <><Loader2 className="mr-2 h-4 w-4 animate-spin" /> Analyzing...</> : "Analyze Compatibility"}
          </Button>
        </CardContent>
      </Card>

      {analysis && (
        <div className="space-y-6 animate-in fade-in slide-in-from-bottom-4">
          {/* GREEN TIER */}
          {analysis.compatibilityTier === "GREEN" && (
            <Card className="border-green-500/50 bg-green-500/5">
              <CardHeader>
                <div className="flex items-center gap-2">
                  <CheckCircle className="text-green-500 h-6 w-6" />
                  <CardTitle className="text-green-700 dark:text-green-400">Great Match! ({analysis.matchScore}%)</CardTitle>
                </div>
                <CardDescription>{analysis.detailedReasoning}</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div>
                  <h4 className="font-medium text-sm mb-2">Matching Skills:</h4>
                  <div className="flex flex-wrap gap-2">
                    {analysis.matchingSkills.map((s: string) => <Badge key={s} className="bg-green-500 hover:bg-green-600">{s}</Badge>)}
                  </div>
                </div>
                <Button onClick={handleTailor} disabled={loadingAction === "tailoring"} className="w-full sm:w-auto">
                  {loadingAction === "tailoring" ? <><RefreshCcw className="mr-2 h-4 w-4 animate-spin" /> Tailoring...</> : "Tailor My Resume"}
                </Button>
              </CardContent>
            </Card>
          )}

          {/* AMBER TIER */}
          {analysis.compatibilityTier === "AMBER" && (
            <Card className="border-amber-500/50 bg-amber-500/5">
              <CardHeader>
                <div className="flex items-center gap-2">
                  <AlertTriangle className="text-amber-500 h-6 w-6" />
                  <CardTitle className="text-amber-700 dark:text-amber-400">This role is a stretch ({analysis.matchScore}%)</CardTitle>
                </div>
                <CardDescription>{analysis.detailedReasoning}</CardDescription>
              </CardHeader>
              <CardContent className="space-y-6">
                <div>
                  <h4 className="font-medium text-sm text-destructive mb-2">Missing Critical Skills:</h4>
                  <div className="flex flex-wrap gap-2">
                    {analysis.missingCriticalSkills.map((s: string) => <Badge key={s} variant="destructive">{s}</Badge>)}
                  </div>
                </div>

                {gapAnalysis && (
                  <div className="space-y-4 mt-6">
                    <h3 className="text-lg font-semibold">Learning Roadmap</h3>
                    {gapAnalysis.learningRoadmap.map((item: any, idx: number) => (
                      <Card key={idx} className="bg-background">
                        <CardHeader className="py-3 px-4">
                          <CardTitle className="text-md flex justify-between">
                            <span>Skill: {item.skill}</span>
                            <span className="text-sm font-normal text-muted-foreground">{item.estimatedTimeToProficiency}</span>
                          </CardTitle>
                        </CardHeader>
                        <CardContent className="py-3 px-4 space-y-3">
                          <div>
                            <span className="text-xs font-semibold uppercase text-muted-foreground">Courses:</span>
                            <ul className="list-disc list-inside text-sm pl-4 mt-1">
                              {item.suggestedCourses.map((c: string, i: number) => <li key={i}>{c}</li>)}
                            </ul>
                          </div>
                          <div>
                            <span className="text-xs font-semibold uppercase text-muted-foreground">Projects:</span>
                            <ul className="list-disc list-inside text-sm pl-4 mt-1">
                              {item.projectIdeas.map((p: string, i: number) => <li key={i}>{p}</li>)}
                            </ul>
                          </div>
                        </CardContent>
                      </Card>
                    ))}
                  </div>
                )}
              </CardContent>
            </Card>
          )}

          {/* RED TIER */}
          {analysis.compatibilityTier === "RED" && (
            <Card className="border-destructive/50 bg-destructive/5">
              <CardHeader>
                <div className="flex items-center gap-2">
                  <XCircle className="text-destructive h-6 w-6" />
                  <CardTitle className="text-destructive">Role not aligned ({analysis.matchScore}%)</CardTitle>
                </div>
                <CardDescription>{analysis.detailedReasoning}</CardDescription>
              </CardHeader>
              <CardContent>
                <p className="text-sm text-muted-foreground">
                  Your profile has significant gaps compared to the requirements. We recommend focusing on roles more closely aligned with your current experience level and skill set.
                </p>
              </CardContent>
            </Card>
          )}
        </div>
      )}

      {/* TAILORING RESULT DIFF VIEWER */}
      {tailoringResult && (
        <Card className="mt-8 border-primary">
          <CardHeader>
            <CardTitle>Tailored Resume Content</CardTitle>
            <CardDescription>Your resume has been adjusted to better highlight relevance to the job.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="bg-muted p-4 rounded-md whitespace-pre-wrap text-sm">
              {tailoringResult.tailoredContent}
            </div>

            <div>
              <h4 className="font-semibold mb-3">Key Changes Made:</h4>
              <div className="space-y-3">
                {tailoringResult.changesMade.map((change: any, idx: number) => (
                  <div key={idx} className="border rounded-md overflow-hidden text-sm">
                    <div className="bg-destructive/10 p-2 border-b border-destructive/20">
                      <span className="line-through text-destructive">{change.original}</span>
                    </div>
                    <div className="bg-green-500/10 p-2 border-b border-green-500/20">
                      <span className="text-green-700 dark:text-green-400">{change.revised}</span>
                    </div>
                    <div className="bg-muted/50 p-2 text-muted-foreground text-xs italic">
                      Reason: {change.reason}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
