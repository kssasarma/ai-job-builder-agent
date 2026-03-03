import { useState, useEffect } from "react";
import { useLocation } from "react-router-dom";
import apiClient from "../../lib/axios";
import { toast } from "sonner";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Badge } from "../../components/ui/badge";
import { Button } from "../../components/ui/button";
import { Input } from "../../components/ui/input";
import { Mail, MapPin, Briefcase, Loader2, Search } from "lucide-react";

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
  }, [location.key, searchParams]);

  const gridClass =
    jobs.length === 1 ? "grid-cols-1" :
    jobs.length === 2 ? "grid-cols-2" :
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
      ) : jobs.length === 0 ? (
        <Card className="p-8 text-center text-muted-foreground">
          No open jobs found.
        </Card>
      ) : (
        <div className={`grid gap-6 ${gridClass}`}>
          {jobs.map(job => (
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

                {/* Footer: contact HR */}
                <div className="flex items-center justify-between pt-2 border-t border-border/40">
                  <span className="text-xs text-muted-foreground">
                    Posted by {job.recruiter?.user?.name || "Recruiter"}
                    {job.recruiter?.companyName ? ` · ${job.recruiter.companyName}` : ""}
                  </span>
                  {job.recruiter?.user?.email && (
                    <Button size="sm" asChild>
                      <a href={`mailto:${job.recruiter.user.email}`}>
                        <Mail className="mr-2 h-4 w-4" /> Contact HR
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
