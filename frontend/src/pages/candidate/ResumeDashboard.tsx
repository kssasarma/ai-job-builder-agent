import { useState, useEffect, useCallback } from "react";
import { FileUpload } from "../../components/candidate/FileUpload";
import { ScoreDisplay } from "../../components/candidate/ScoreDisplay";
import apiClient from "../../lib/axios";
import { useOperationStatus, type OperationStatusMessage } from "../../lib/websocket";
import { toast } from "sonner";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "../../components/ui/card";
import { Loader2 } from "lucide-react";
import TailoringUI from "../../components/candidate/TailoringUI";
import { TailoringHistoryList } from "../../components/candidate/TailoringHistoryList";

export default function ResumeDashboard() {
  const [loading, setLoading] = useState(false);
  const [scoringStatus, setScoringStatus] = useState<string | null>(null);
  const [scoreData, setScoreData] = useState<any>(null);
  const [currentResumeId, setCurrentResumeId] = useState<string | null>(null);

  const handleUpload = async (file: File) => {
    setLoading(true);
    setScoringStatus(null);
    setScoreData(null);

    const formData = new FormData();
    formData.append("file", file);

    try {
      // 1. Upload
      const uploadRes = await apiClient.post("/candidate/resume/upload", formData, {
        headers: { "Content-Type": "multipart/form-data" },
      });
      const resumeId = uploadRes.data.resumeId;
      setCurrentResumeId(resumeId);

      // 2. Trigger Scoring
      await apiClient.post(`/candidate/resume/${resumeId}/score`);
      setScoringStatus("PROCESSING");

    } catch (error: any) {
      toast.error(error.response?.data || "Failed to process resume");
      setLoading(false);
    }
  };

  // Pushed by the backend over WebSocket the moment scoring finishes — no more
  // polling getScoringStatus() on an interval and waiting up to 3s for an update.
  const handleOperationStatus = useCallback(async (payload: OperationStatusMessage) => {
    if (payload.type !== "SCORING" || !currentResumeId) return;

    if (payload.status === "COMPLETED") {
      try {
        const res = await apiClient.get(`/candidate/resume/${currentResumeId}/score/status`);
        setScoreData(res.data.result);
        setScoringStatus("COMPLETED");
        setLoading(false);
        toast.success("Analysis complete!");
        if (res.data.profileUpdated) {
          toast.success("Profile updated as per the resume.");
        }
      } catch (error) {
        console.error("Failed to fetch completed score", error);
      }
    } else if (payload.status?.startsWith("FAILED")) {
      toast.error(payload.status);
      setScoringStatus("FAILED");
      setLoading(false);
    }
  }, [currentResumeId]);

  useOperationStatus(scoringStatus === "PROCESSING" ? currentResumeId : null, handleOperationStatus);

  useEffect(() => {
    // Fetch latest primary resume on load
    apiClient.get("/candidate/resume").then(res => {
      const resumes = res.data;
      const primary = resumes.find((r: any) => r.primary);
      if (primary) {
        setCurrentResumeId(primary.id);
        if (primary.scoreBreakdown) {
          setScoreData(JSON.parse(primary.scoreBreakdown));
          setScoringStatus("COMPLETED");
        }
      }
    }).catch(console.error);
  }, []);

  return (
    <div className="container mx-auto p-6 max-w-5xl space-y-8">
      <div className="flex justify-between items-end">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Resume Dashboard</h1>
          <p className="text-muted-foreground mt-2">Upload your resume to get an AI-powered ATS score.</p>
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Upload Resume</CardTitle>
          <CardDescription>Upload your latest PDF resume for analysis.</CardDescription>
        </CardHeader>
        <CardContent>
          <FileUpload onUpload={handleUpload} loading={loading} />
        </CardContent>
      </Card>

      {scoringStatus === "PROCESSING" && (
        <Card className="border-primary/50 bg-primary/5">
          <CardContent className="flex flex-col items-center justify-center p-12 space-y-4">
            <Loader2 className="h-12 w-12 text-primary animate-spin" />
            <div className="text-center">
              <h3 className="text-lg font-medium">Analyzing your resume...</h3>
              <p className="text-sm text-muted-foreground">Our AI is currently reviewing your formatting, skills, and experience.</p>
            </div>
          </CardContent>
        </Card>
      )}

      {scoreData && (
        <div className="animate-in fade-in slide-in-from-bottom-4 duration-500">
          <ScoreDisplay data={scoreData} resumeId={currentResumeId ?? undefined} />

          {currentResumeId && (
            <div className="mt-12 pt-8 border-t">
              <TailoringUI resumeId={currentResumeId} />
              <TailoringHistoryList resumeId={currentResumeId} />
            </div>
          )}
        </div>
      )}
    </div>
  );
}
