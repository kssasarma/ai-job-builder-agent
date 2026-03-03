import { useState, useEffect } from "react";
import { useLocation } from "react-router-dom";
import apiClient from "../../lib/axios";
import { toast } from "sonner";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Badge } from "../../components/ui/badge";
import { Button } from "../../components/ui/button";
import { ExternalLink, Mail, Loader2, Download } from "lucide-react";

export default function RecruiterCandidateBrowsePage() {
  const location = useLocation();
  const [candidates, setCandidates] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [expandedSkills, setExpandedSkills] = useState<Set<string>>(new Set());

  const toggleSkills = (candidateId: string) => {
    setExpandedSkills(prev => {
      const next = new Set(prev);
      if (next.has(candidateId)) {
        next.delete(candidateId);
      } else {
        next.add(candidateId);
      }
      return next;
    });
  };

  const downloadResume = async (candidateId: string, candidateName?: string) => {
    try {
      const res = await apiClient.get(`/recruiter/candidates/${candidateId}/resume/download`, { responseType: 'blob' });
      const url = URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }));
      const a = document.createElement('a');
      a.href = url;
      a.download = `resume_${(candidateName || candidateId).replace(/\s+/g, '_')}.pdf`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch {
      toast.error("No resume available for this candidate.");
    }
  };

  useEffect(() => {
    setLoading(true);
    setExpandedSkills(new Set());
    const fetchCandidates = async () => {
      try {
        const res = await apiClient.get("/recruiter/candidates?size=50");
        setCandidates(res.data.content || []);
      } catch (error) {
        toast.error("Failed to load candidates.");
      } finally {
        setLoading(false);
      }
    };
    fetchCandidates();
  }, [location.key]);

  return (
    <div className="container mx-auto p-6 max-w-6xl space-y-8 mt-8">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Browse Candidates</h1>
        <p className="text-muted-foreground mt-2">Discover candidates open to new opportunities.</p>
      </div>

      {loading ? (
        <div className="flex justify-center p-8"><Loader2 className="animate-spin h-8 w-8 text-primary" /></div>
      ) : candidates.length === 0 ? (
        <Card className="p-8 text-center text-muted-foreground">
          No candidates found.
        </Card>
      ) : (
        <div className={`grid gap-6 ${candidates.length === 1 ? "grid-cols-1" : candidates.length === 2 ? "grid-cols-2" : "grid-cols-3"}`}>
          {candidates.map(candidate => (
            <Card key={candidate.candidateId} className="flex flex-col">
              <CardHeader className="pb-3 border-b border-border/40 bg-muted/20">
                <div className="flex justify-between items-start">
                  <div>
                    <CardTitle className="text-xl">{candidate.name || `Candidate ${candidate.candidateId.substring(0,8)}`}</CardTitle>
                    <p className="text-sm font-medium text-muted-foreground mt-1">{candidate.headline || "Professional"}</p>
                  </div>
                </div>
              </CardHeader>
              <CardContent className="pt-4 flex-1 flex flex-col justify-between space-y-4">
                {candidate.experienceSummary && (
                  <div>
                    <p className="text-sm font-semibold mb-1">Experience Summary:</p>
                    <p className="text-sm text-muted-foreground">{candidate.experienceSummary}</p>
                  </div>
                )}

                {candidate.skills && candidate.skills.length > 0 && (
                  <div>
                    <p className="text-sm font-semibold mb-2">Skills:</p>
                    <div className="flex flex-wrap gap-1">
                      {(expandedSkills.has(candidate.candidateId)
                        ? candidate.skills
                        : candidate.skills.slice(0, 8)
                      ).map((s: string) => (
                        <Badge key={s} variant="secondary" className="text-xs">{s}</Badge>
                      ))}
                      {candidate.skills.length > 8 && (
                        <button
                          onClick={() => toggleSkills(candidate.candidateId)}
                          className="inline-flex items-center"
                        >
                          <Badge variant="outline" className="text-xs cursor-pointer hover:bg-accent">
                            {expandedSkills.has(candidate.candidateId)
                              ? "Show less"
                              : `+${candidate.skills.length - 8} more`}
                          </Badge>
                        </button>
                      )}
                    </div>
                  </div>
                )}

                <div className="flex gap-2 pt-2 border-t border-border/40 justify-end">
                  {candidate.linkedinUrl && (
                    <Button variant="outline" size="sm" asChild>
                      <a href={candidate.linkedinUrl} target="_blank" rel="noreferrer">
                        <ExternalLink className="mr-2 h-4 w-4" /> LinkedIn
                      </a>
                    </Button>
                  )}
                  <Button
                    variant="outline"
                    size="sm"
                    title="Download Resume"
                    onClick={() => downloadResume(candidate.candidateId, candidate.name)}
                  >
                    <Download className="mr-2 h-4 w-4" /> Resume
                  </Button>
                  {candidate.preferredContactEmail && (
                    <Button size="sm" asChild>
                      <a href={`mailto:${candidate.preferredContactEmail}`}>
                        <Mail className="mr-2 h-4 w-4" /> Contact
                      </a>
                    </Button>
                  )}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
