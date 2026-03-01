import { useState, useEffect } from "react";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "../../components/ui/card";
import { Badge } from "../../components/ui/badge";
import { Button } from "../../components/ui/button";
import { PlusCircle, Search, Loader2, Mail, ExternalLink } from "lucide-react";
import apiClient from "../../lib/axios";
import { toast } from "sonner";
import { JobCreateModal } from "../../components/recruiter/JobCreateModal";

export default function RecruiterDashboard() {
  const [jobs, setJobs] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [selectedJob, setSelectedJob] = useState<any | null>(null);
  const [matches, setMatches] = useState<any[]>([]);
  const [matchingStatus, setMatchingStatus] = useState<string | null>(null);

  const fetchJobs = async () => {
    try {
      const res = await apiClient.get("/recruiter/jobs?size=50&sort=createdAt,desc");
      // Since it's paginated, jobs are inside the "content" field
      setJobs(res.data.content);
    } catch (error) {
      toast.error("Failed to load jobs");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchJobs();
  }, []);

  const handleMatchCandidates = async (jobId: string) => {
    setSelectedJob(jobs.find(j => j.id === jobId));
    setMatchingStatus("PROCESSING");
    setMatches([]);

    try {
      await apiClient.post(`/recruiter/jobs/${jobId}/find-candidates`);
      pollMatchingStatus(jobId);
    } catch (error) {
      toast.error("Failed to start matching process");
      setMatchingStatus(null);
    }
  };

  const pollMatchingStatus = (jobId: string) => {
    const interval = setInterval(async () => {
      try {
        const res = await apiClient.get(`/recruiter/jobs/${jobId}/matches`);
        if (res.data.status === "COMPLETED") {
          setMatches(res.data.matches);
          setMatchingStatus("COMPLETED");
          clearInterval(interval);
          toast.success("Matching complete!");
        } else if (res.data.status && res.data.status.startsWith("FAILED")) {
          toast.error("Matching failed: " + res.data.status);
          setMatchingStatus(null);
          clearInterval(interval);
        }
      } catch (error) {
        console.error(error);
      }
    }, 3000);
  };

  const getScoreColor = (score: number) => {
    if (score >= 75) return "bg-green-500 text-white";
    if (score >= 50) return "bg-amber-500 text-white";
    return "bg-red-500 text-white";
  };

  return (
    <div className="container mx-auto p-6 max-w-6xl space-y-8 mt-8">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Recruiter Dashboard</h1>
          <p className="text-muted-foreground mt-2">Manage your job postings and find top talent.</p>
        </div>
        <Button onClick={() => setModalOpen(true)}>
          <PlusCircle className="mr-2 h-4 w-4" /> Create Job
        </Button>
      </div>

      <JobCreateModal open={modalOpen} onOpenChange={setModalOpen} onJobCreated={fetchJobs} />

      <div className="grid md:grid-cols-12 gap-6">
        {/* Left Column: Job List */}
        <div className="md:col-span-4 space-y-4">
          <h2 className="text-xl font-semibold mb-4">Your Postings</h2>
          {loading ? (
            <div className="flex justify-center p-8"><Loader2 className="animate-spin h-8 w-8 text-primary" /></div>
          ) : jobs.length === 0 ? (
            <Card className="p-8 text-center text-muted-foreground">
              No jobs created yet.
            </Card>
          ) : (
            jobs.map(job => (
              <Card
                key={job.id}
                className={`cursor-pointer transition-colors hover:border-primary/50 ${selectedJob?.id === job.id ? 'border-primary shadow-sm' : ''}`}
                onClick={() => setSelectedJob(job)}
              >
                <CardHeader className="p-4">
                  <div className="flex justify-between items-start">
                    <CardTitle className="text-lg">{job.title}</CardTitle>
                    <Badge variant={job.status === "ACTIVE" ? "default" : "secondary"} className="text-[10px]">
                      {job.status}
                    </Badge>
                  </div>
                  <CardDescription>{job.company} • {job.location || "Remote"}</CardDescription>
                </CardHeader>
                <CardContent className="p-4 pt-0 flex justify-end">
                  <Button variant="secondary" size="sm" onClick={(e) => { e.stopPropagation(); handleMatchCandidates(job.id); }}>
                    <Search className="mr-2 h-3 w-3" /> Find Matches
                  </Button>
                </CardContent>
              </Card>
            ))
          )}
        </div>

        {/* Right Column: Matching Results */}
        <div className="md:col-span-8">
          {!selectedJob ? (
            <div className="h-full min-h-[400px] border-2 border-dashed rounded-xl flex items-center justify-center text-muted-foreground">
              Select a job posting to view details or find candidates.
            </div>
          ) : (
            <div className="space-y-6">
              <Card>
                <CardHeader>
                  <CardTitle>{selectedJob.title}</CardTitle>
                  <CardDescription>{selectedJob.company}</CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="mb-4">
                    <span className="text-sm font-semibold">Required Skills: </span>
                    <div className="flex flex-wrap gap-1 mt-1">
                      {selectedJob.requiredSkills?.map((s: string) => <Badge key={s} variant="outline">{s}</Badge>)}
                    </div>
                  </div>
                  <Button onClick={() => handleMatchCandidates(selectedJob.id)} disabled={matchingStatus === "PROCESSING"}>
                    {matchingStatus === "PROCESSING" ? <><Loader2 className="mr-2 h-4 w-4 animate-spin" /> Analyzing Candidates...</> : "Refresh Matches"}
                  </Button>
                </CardContent>
              </Card>

              {matchingStatus === "PROCESSING" && (
                <Card className="border-primary/50 bg-primary/5">
                  <CardContent className="flex flex-col items-center justify-center p-12 space-y-4">
                    <Loader2 className="h-10 w-10 text-primary animate-spin" />
                    <p className="text-sm font-medium">AI is matching candidates to your job description...</p>
                  </CardContent>
                </Card>
              )}

              {matchingStatus === "COMPLETED" && (
                <div className="space-y-4">
                  <h3 className="text-lg font-semibold">Top Matched Candidates ({matches.length})</h3>
                  {matches.length === 0 ? (
                    <p className="text-muted-foreground">No matching candidates found that are open to opportunities.</p>
                  ) : (
                    matches.map(match => (
                      <Card key={match.id} className="overflow-hidden">
                        <div className="flex flex-col sm:flex-row">
                          <div className="p-6 flex flex-col items-center justify-center bg-muted/30 border-r min-w-[120px]">
                            <div className={`w-16 h-16 rounded-full flex items-center justify-center text-2xl font-bold ${getScoreColor(match.matchScore)}`}>
                              {match.matchScore}
                            </div>
                            <span className="text-xs text-muted-foreground mt-2 font-semibold">MATCH</span>
                          </div>
                          <div className="p-6 flex-1">
                            <div className="flex justify-between items-start mb-2">
                              <div>
                                <h4 className="text-lg font-bold">Candidate {match.candidate.id.substring(0,8)}</h4>
                                <p className="text-sm text-muted-foreground">{match.candidate.headline || "Professional"}</p>
                              </div>
                              <div className="flex gap-2">
                                {match.candidate.linkedinUrl && (
                                  <Button variant="outline" size="icon" asChild>
                                    <a href={match.candidate.linkedinUrl} target="_blank" rel="noreferrer"><ExternalLink className="h-4 w-4" /></a>
                                  </Button>
                                )}
                                <Button size="sm" asChild>
                                  <a href={`mailto:${match.candidate.preferredContactEmail}?subject=Regarding ${selectedJob.title} opportunity at ${selectedJob.company}`}>
                                    <Mail className="mr-2 h-4 w-4" /> Contact
                                  </a>
                                </Button>
                              </div>
                            </div>

                            <div className="mt-4 text-sm">
                              <p className="font-semibold mb-1">AI Reasoning:</p>
                              <p className="text-muted-foreground">{match.matchReasoning}</p>
                            </div>

                            <div className="mt-4 grid sm:grid-cols-2 gap-4">
                              <div>
                                <p className="text-xs font-semibold text-green-600 dark:text-green-400 mb-1">Matching Skills:</p>
                                <div className="flex flex-wrap gap-1">
                                  {match.matchingSkills?.map((s: string) => <Badge key={s} className="bg-green-500/10 text-green-700 hover:bg-green-500/20">{s}</Badge>)}
                                </div>
                              </div>
                              {match.identifiedGaps?.length > 0 && (
                                <div>
                                  <p className="text-xs font-semibold text-red-600 dark:text-red-400 mb-1">Identified Gaps:</p>
                                  <div className="flex flex-wrap gap-1">
                                    {match.identifiedGaps.map((s: string) => <Badge key={s} variant="destructive" className="bg-red-500/10 text-red-700 hover:bg-red-500/20">{s}</Badge>)}
                                  </div>
                                </div>
                              )}
                            </div>
                          </div>
                        </div>
                      </Card>
                    ))
                  )}
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
