import { useState, useEffect } from "react";
import { useLocation } from "react-router-dom";
import apiClient from "../../lib/axios";
import { toast } from "sonner";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Badge } from "../../components/ui/badge";
import { Button } from "../../components/ui/button";
import { Input } from "../../components/ui/input";
import { Mail, MapPin, Briefcase, Loader2, Search, CheckCircle2, Zap } from "lucide-react";

interface CompatibilityResult {
  compatibilityScore: number;
  recommendation: string;
  reasoning: string;
  matchingStrengths: string[];
  missingRequirements: string[];
}

export default function CandidateJobBrowsePage() {
  const location = useLocation();

  const [jobs, setJobs] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [expandedSkills, setExpandedSkills] = useState<Set<string>>(new Set());
  const [expandedDesc, setExpandedDesc] = useState<Set<string>>(new Set());

  const [keyword, setKeyword] = useState("");
  const [locationFilter, setLocationFilter] = useState("");
  const [skillsFilter, setSkillsFilter] = useState("");
  const [searchParams, setSearchParams] = useState({ keyword: "", location: "", skills: "" });

  const [appliedJobIds, setAppliedJobIds] = useState<Set<string>>(new Set());
  const [applyingJobIds, setApplyingJobIds] = useState<Set<string>>(new Set());

  const [compatibilityResults, setCompatibilityResults] = useState<Record<string, CompatibilityResult>>({});
  const [compatibilityLoading, setCompatibilityLoading] = useState<Set<string>>(new Set());

  const toggleSkills = (jobId: string) => {
    setExpandedSkills(prev => {
      const next = new Set(prev);
      next.has(jobId) ? next.delete(jobId) : next.add(jobId);
      return next;
    });
  };

  const toggleDesc = (jobId: string) => {
    setExpandedDesc(prev => {
      const next = new Set(prev);
      next.has(jobId) ? next.delete(jobId) : next.add(jobId);
      return next;
    });
  };

  const handleSearch = () => {
    setSearchParams({ keyword, location: locationFilter, skills: skillsFilter });
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter") handleSearch();
  };

  const fetchAppliedIds = async () => {
    try {
      const res = await apiClient.get("/candidate/jobs/applied");
      setAppliedJobIds(new Set(res.data as string[]));
    } catch {
      // silently fail — not critical
    }
  };

  useEffect(() => {
    setLoading(true);
    setExpandedSkills(new Set());
    setExpandedDesc(new Set());

    const params = new URLSearchParams({ size: "50" });
    if (searchParams.keyword) params.set("keyword", searchParams.keyword);
    if (searchParams.location) params.set("location", searchParams.location);
    if (searchParams.skills) params.set("skills", searchParams.skills);

    const fetchJobs = async () => {
      try {
        const res = await apiClient.get(`/candidate/jobs?${params.toString()}`);
        setJobs(res.data.content || []);
      } catch {
        toast.error("Failed to load jobs.");
      } finally {
        setLoading(false);
      }
    };

    fetchJobs();
    fetchAppliedIds();
  }, [location.key, searchParams]);

  const handleApply = async (jobId: string) => {
    setApplyingJobIds(prev => new Set(prev).add(jobId));
    try {
      await apiClient.post(`/candidate/jobs/${jobId}/apply`);
      setAppliedJobIds(prev => new Set(prev).add(jobId));
      toast.success("Application submitted successfully!");
    } catch (error: any) {
      if (error.response?.status === 409) {
        toast.info("You have already applied to this job.");
        setAppliedJobIds(prev => new Set(prev).add(jobId));
      } else {
        toast.error("Failed to apply. Please try again.");
      }
    } finally {
      setApplyingJobIds(prev => {
        const next = new Set(prev);
        next.delete(jobId);
        return next;
      });
    }
  };

  const handleCompatibilityCheck = async (jobId: string) => {
    setCompatibilityLoading(prev => new Set(prev).add(jobId));
    try {
      const res = await apiClient.get(`/candidate/jobs/${jobId}/compatibility`);
      setCompatibilityResults(prev => ({ ...prev, [jobId]: res.data }));
    } catch {
      toast.error("Failed to check compatibility. Please try again.");
    } finally {
      setCompatibilityLoading(prev => {
        const next = new Set(prev);
        next.delete(jobId);
        return next;
      });
    }
  };

  const getScoreColor = (score: number) => {
    if (score >= 70) return "bg-green-500 text-white";
    if (score >= 40) return "bg-amber-500 text-white";
    return "bg-red-500 text-white";
  };

  const getRecommendationColor = (rec: string) => {
    if (rec === "LIKELY_SELECTED") return "bg-green-500/10 text-green-700 border-green-200";
    if (rec === "POSSIBLE") return "bg-amber-500/10 text-amber-700 border-amber-200";
    return "bg-red-500/10 text-red-700 border-red-200";
  };

  const getRecommendationLabel = (rec: string) => {
    if (rec === "LIKELY_SELECTED") return "Likely Selected";
    if (rec === "POSSIBLE") return "Possible";
    return "Unlikely";
  };

  const visibleJobs = jobs.filter(job => !appliedJobIds.has(job.id));

  const gridClass =
    visibleJobs.length === 1 ? "grid-cols-1" :
    visibleJobs.length === 2 ? "grid-cols-2" :
    "grid-cols-3";

  return (
    <div className="container mx-auto p-6 max-w-6xl space-y-8 mt-8">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Browse Jobs</h1>
        <p className="text-muted-foreground mt-2">Discover open positions and connect with recruiters.</p>
      </div>

      {/* Search bar */}
      <div className="flex flex-wrap gap-3">
        <Input
          placeholder="Keyword (title, company…)"
          value={keyword}
          onChange={e => setKeyword(e.target.value)}
          onKeyDown={handleKeyDown}
          className="w-56"
        />
        <Input
          placeholder="Location"
          value={locationFilter}
          onChange={e => setLocationFilter(e.target.value)}
          onKeyDown={handleKeyDown}
          className="w-44"
        />
        <Input
          placeholder="Skills (comma-separated)"
          value={skillsFilter}
          onChange={e => setSkillsFilter(e.target.value)}
          onKeyDown={handleKeyDown}
          className="w-56"
        />
        <Button onClick={handleSearch}>
          <Search className="mr-2 h-4 w-4" /> Search
        </Button>
      </div>

      {loading ? (
        <div className="flex justify-center p-8">
          <Loader2 className="animate-spin h-8 w-8 text-primary" />
        </div>
      ) : visibleJobs.length === 0 ? (
        <Card className="p-8 text-center text-muted-foreground">
          No open jobs found.
        </Card>
      ) : (
        <div className={`grid gap-6 ${gridClass}`}>
          {visibleJobs.map(job => {
            const compatResult = compatibilityResults[job.id];
            const isCheckingCompat = compatibilityLoading.has(job.id);
            const isApplied = appliedJobIds.has(job.id);
            const isApplying = applyingJobIds.has(job.id);

            return (
              <Card key={job.id} className="flex flex-col">
                <CardHeader className="pb-3 border-b border-border/40 bg-muted/20">
                  <div className="space-y-1">
                    <div className="flex items-start justify-between gap-2">
                      <CardTitle className="text-xl leading-tight">{job.title}</CardTitle>
                      {job.jobType && (
                        <Badge variant="secondary" className="text-xs shrink-0">{job.jobType.replace("_", " ")}</Badge>
                      )}
                    </div>
                    <p className="text-sm font-medium text-muted-foreground">{job.company || job.recruiter?.companyName}</p>
                    {job.location && (
                      <p className="text-xs text-muted-foreground flex items-center gap-1">
                        <MapPin className="h-3 w-3" />{job.location}
                      </p>
                    )}
                  </div>
                </CardHeader>

                <CardContent className="pt-4 flex-1 flex flex-col justify-between space-y-4">
                  {/* Meta: salary + experience */}
                  <div className="flex flex-wrap gap-3 text-xs text-muted-foreground">
                    {job.salaryRange && (
                      <span className="flex items-center gap-1">
                        <Briefcase className="h-3 w-3" />{job.salaryRange}
                      </span>
                    )}
                    {(job.experienceMin != null || job.experienceMax != null) && (
                      <span>
                        {job.experienceMin ?? 0}–{job.experienceMax ?? "+"} yrs exp
                      </span>
                    )}
                  </div>

                  {/* Description */}
                  {job.description && (
                    <div>
                      <p className={`text-sm text-muted-foreground ${expandedDesc.has(job.id) ? "" : "line-clamp-3"}`}>
                        {job.description}
                      </p>
                      <button
                        onClick={() => toggleDesc(job.id)}
                        className="text-xs text-primary hover:underline mt-1"
                      >
                        {expandedDesc.has(job.id) ? "Show less" : "Show more"}
                      </button>
                    </div>
                  )}

                  {/* Required skills */}
                  {job.requiredSkills && job.requiredSkills.length > 0 && (
                    <div>
                      <p className="text-sm font-semibold mb-2">Required Skills:</p>
                      <div className="flex flex-wrap gap-1">
                        {(expandedSkills.has(job.id)
                          ? job.requiredSkills
                          : job.requiredSkills.slice(0, 8)
                        ).map((s: string) => (
                          <Badge key={s} variant="secondary" className="text-xs">{s}</Badge>
                        ))}
                        {job.requiredSkills.length > 8 && (
                          <button onClick={() => toggleSkills(job.id)} className="inline-flex items-center">
                            <Badge variant="outline" className="text-xs cursor-pointer hover:bg-accent">
                              {expandedSkills.has(job.id) ? "Show less" : `+${job.requiredSkills.length - 8} more`}
                            </Badge>
                          </button>
                        )}
                      </div>
                    </div>
                  )}

                  {/* Compatibility Result */}
                  {compatResult && (
                    <div className="rounded-lg border bg-muted/30 p-3 space-y-2">
                      <div className="flex items-center gap-3">
                        <div className={`w-12 h-12 rounded-full flex items-center justify-center text-lg font-bold shrink-0 ${getScoreColor(compatResult.compatibilityScore)}`}>
                          {compatResult.compatibilityScore}
                        </div>
                        <div>
                          <Badge variant="outline" className={`text-xs font-semibold ${getRecommendationColor(compatResult.recommendation)}`}>
                            {getRecommendationLabel(compatResult.recommendation)}
                          </Badge>
                          <p className="text-xs text-muted-foreground mt-1">{compatResult.reasoning}</p>
                        </div>
                      </div>
                      {compatResult.matchingStrengths?.length > 0 && (
                        <div>
                          <p className="text-xs font-semibold text-green-600 mb-1">Matching Strengths:</p>
                          <div className="flex flex-wrap gap-1">
                            {compatResult.matchingStrengths.map(s => (
                              <Badge key={s} className="text-xs bg-green-500/10 text-green-700 hover:bg-green-500/20">{s}</Badge>
                            ))}
                          </div>
                        </div>
                      )}
                      {compatResult.missingRequirements?.length > 0 && (
                        <div>
                          <p className="text-xs font-semibold text-red-600 mb-1">Missing Requirements:</p>
                          <div className="flex flex-wrap gap-1">
                            {compatResult.missingRequirements.map(s => (
                              <Badge key={s} variant="destructive" className="text-xs bg-red-500/10 text-red-700 hover:bg-red-500/20">{s}</Badge>
                            ))}
                          </div>
                        </div>
                      )}
                    </div>
                  )}

                  {/* Footer: actions */}
                  <div className="flex items-center justify-between pt-2 border-t border-border/40 gap-2 flex-wrap">
                    <span className="text-xs text-muted-foreground">
                      Posted by {job.recruiter?.user?.name || "Recruiter"}
                      {job.recruiter?.companyName ? ` · ${job.recruiter.companyName}` : ""}
                    </span>
                    <div className="flex gap-2 flex-wrap">
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleCompatibilityCheck(job.id)}
                        disabled={isCheckingCompat}
                      >
                        {isCheckingCompat ? (
                          <><Loader2 className="mr-2 h-3 w-3 animate-spin" />Checking…</>
                        ) : (
                          <><Zap className="mr-2 h-3 w-3" />Find Compatibility</>
                        )}
                      </Button>
                      {isApplied ? (
                        <Button size="sm" variant="secondary" disabled>
                          <CheckCircle2 className="mr-2 h-3 w-3" />Applied
                        </Button>
                      ) : (
                        <Button size="sm" onClick={() => handleApply(job.id)} disabled={isApplying}>
                          {isApplying ? <><Loader2 className="mr-2 h-3 w-3 animate-spin" />Applying…</> : "Apply"}
                        </Button>
                      )}
                      {job.recruiter?.user?.email && (
                        <Button size="sm" variant="ghost" asChild>
                          <a href={`mailto:${job.recruiter.user.email}`}>
                            <Mail className="mr-2 h-4 w-4" /> Contact HR
                          </a>
                        </Button>
                      )}
                    </div>
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
}
