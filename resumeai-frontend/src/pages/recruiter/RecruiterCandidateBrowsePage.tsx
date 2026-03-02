import { useState, useEffect } from "react";
import apiClient from "../../lib/axios";
import { toast } from "sonner";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Badge } from "../../components/ui/badge";
import { Button } from "../../components/ui/button";
import { ExternalLink, Mail, Loader2 } from "lucide-react";

export default function RecruiterCandidateBrowsePage() {
  const [candidates, setCandidates] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchCandidates = async () => {
      try {
        const res = await apiClient.get("/recruiter/candidates?size=50&sort=createdAt,desc");
        setCandidates(res.data.content || []);
      } catch (error) {
        toast.error("Failed to load candidates.");
      } finally {
        setLoading(false);
      }
    };
    fetchCandidates();
  }, []);

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
        <div className="grid md:grid-cols-2 gap-6">
          {candidates.map(candidate => (
            <Card key={candidate.candidateId} className="flex flex-col">
              <CardHeader className="pb-3 border-b border-border/40 bg-muted/20">
                <div className="flex justify-between items-start">
                  <div>
                    <CardTitle className="text-xl">Candidate {candidate.candidateId.substring(0,8)}</CardTitle>
                    <p className="text-sm font-medium text-muted-foreground mt-1">{candidate.headline || "Professional"}</p>
                  </div>
                  {candidate.latestAtsScore !== null && (
                    <Badge variant="outline" className="font-mono text-xs">
                      General ATS Score: {candidate.latestAtsScore}
                    </Badge>
                  )}
                </div>
              </CardHeader>
              <CardContent className="pt-4 flex-1 flex flex-col justify-between space-y-4">
                {candidate.skills && candidate.skills.length > 0 && (
                  <div>
                    <p className="text-sm font-semibold mb-2">Skills:</p>
                    <div className="flex flex-wrap gap-1">
                      {candidate.skills.slice(0, 8).map((s: string) => (
                        <Badge key={s} variant="secondary" className="text-xs">{s}</Badge>
                      ))}
                      {candidate.skills.length > 8 && (
                        <Badge variant="outline" className="text-xs">+{candidate.skills.length - 8} more</Badge>
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
