import { useState, useEffect } from "react";
import { useLocation } from "react-router-dom";
import apiClient from "../../lib/axios";
import { toast } from "sonner";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Badge } from "../../components/ui/badge";
import { Loader2, MapPin, Briefcase } from "lucide-react";

interface ApplicationSummary {
  applicationId: string;
  status: string;
  appliedAt: string;
  updatedAt: string;
  jobPostingId: string;
  jobTitle: string;
  company: string;
  location?: string;
  jobType?: string;
  salaryRange?: string;
}

const STATUS_CONFIG: Record<string, { label: string; className: string }> = {
  APPLIED:   { label: "Applied",   className: "bg-blue-500/10 text-blue-700 border-blue-200" },
  CONTACTED: { label: "Contacted", className: "bg-amber-500/10 text-amber-700 border-amber-200" },
  INTERVIEW: { label: "Interview", className: "bg-purple-500/10 text-purple-700 border-purple-200" },
  REJECTED:  { label: "Rejected",  className: "bg-red-500/10 text-red-700 border-red-200" },
  SELECTED:  { label: "Selected",  className: "bg-green-500/10 text-green-700 border-green-200" },
};

export default function CandidateApplicationsPage() {
  const location = useLocation();
  const [applications, setApplications] = useState<ApplicationSummary[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    apiClient.get("/candidate/applications")
      .then(res => setApplications(res.data || []))
      .catch(() => toast.error("Failed to load applications."))
      .finally(() => setLoading(false));
  }, [location.key]);

  const getStatusConfig = (status: string) =>
    STATUS_CONFIG[status] ?? { label: status, className: "bg-muted text-muted-foreground" };

  return (
    <div className="container mx-auto p-6 max-w-4xl space-y-8 mt-8">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">My Applications</h1>
        <p className="text-muted-foreground mt-2">Track the status of every role you've applied to.</p>
      </div>

      {loading ? (
        <div className="flex justify-center p-8">
          <Loader2 className="animate-spin h-8 w-8 text-primary" />
        </div>
      ) : applications.length === 0 ? (
        <Card className="p-8 text-center text-muted-foreground">
          You haven't applied to any jobs yet. Head over to Browse Jobs to get started.
        </Card>
      ) : (
        <div className="space-y-4">
          {applications.map(app => {
            const { label, className } = getStatusConfig(app.status);
            return (
              <Card key={app.applicationId} className="flex flex-col sm:flex-row sm:items-center gap-4 p-5">
                {/* Left: job info */}
                <div className="flex-1 space-y-1">
                  <CardTitle className="text-lg leading-tight">{app.jobTitle}</CardTitle>
                  <p className="text-sm font-medium text-muted-foreground">{app.company}</p>
                  <div className="flex flex-wrap gap-3 text-xs text-muted-foreground mt-1">
                    {app.location && (
                      <span className="flex items-center gap-1">
                        <MapPin className="h-3 w-3" />{app.location}
                      </span>
                    )}
                    {app.jobType && (
                      <span className="flex items-center gap-1">
                        <Briefcase className="h-3 w-3" />{app.jobType.replace("_", " ")}
                      </span>
                    )}
                    {app.salaryRange && (
                      <span>{app.salaryRange}</span>
                    )}
                  </div>
                  <p className="text-xs text-muted-foreground pt-1">
                    Applied {new Date(app.appliedAt).toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" })}
                    {app.updatedAt !== app.appliedAt && (
                      <> · Updated {new Date(app.updatedAt).toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" })}</>
                    )}
                  </p>
                </div>

                {/* Right: status badge */}
                <div className="shrink-0">
                  <Badge variant="outline" className={`text-sm font-semibold px-3 py-1 ${className}`}>
                    {label}
                  </Badge>
                </div>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
}
