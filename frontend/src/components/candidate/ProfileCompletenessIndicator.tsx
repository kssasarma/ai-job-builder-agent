import { useState, useEffect } from "react";
import apiClient from "../../lib/axios";
import { Progress } from "../ui/progress";
import { Card, CardHeader, CardTitle, CardContent } from "../ui/card";
import { Link } from "react-router-dom";
import { CheckCircle2, Circle } from "lucide-react";

interface CompletenessData {
  completionPercentage: number;
  missingFields: string[];
  hasResume: boolean;
  hasScore: boolean;
  openToOpportunities: boolean;
}

export default function ProfileCompletenessIndicator() {
  const [data, setData] = useState<CompletenessData | null>(null);

  useEffect(() => {
    const fetchCompleteness = async () => {
      try {
        const response = await apiClient.get("/candidate/profile/completeness");
        setData(response.data);
      } catch (error) {
        console.error("Failed to fetch profile completeness", error);
      }
    };

    fetchCompleteness();
  }, []);

  if (!data) return null;

  const items = [
    { key: "headline", label: "Add a headline", done: !data.missingFields.includes("headline") },
    { key: "skills", label: "Add your skills", done: !data.missingFields.includes("skills") },
    { key: "linkedinUrl", label: "Add LinkedIn URL", done: !data.missingFields.includes("linkedinUrl") },
    { key: "preferredContactEmail", label: "Add contact email", done: !data.missingFields.includes("preferredContactEmail") },
    { key: "hasResume", label: "Upload a resume", done: data.hasResume },
    { key: "hasScore", label: "Get your ATS score", done: data.hasScore },
    { key: "openToOpportunities", label: "Opt-in to opportunities", done: data.openToOpportunities },
  ];

  return (
    <Card className="mb-6">
      <CardHeader className="pb-3">
        <CardTitle className="text-lg flex justify-between items-center">
          <span>Profile Completeness</span>
          <span className="text-muted-foreground">{data.completionPercentage}%</span>
        </CardTitle>
        <Progress value={data.completionPercentage} className="h-2" />
      </CardHeader>
      <CardContent>
        {data.completionPercentage < 100 && (
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 mt-2">
            {items.map((item) => (
              <div key={item.key} className="flex items-center gap-2 text-sm">
                {item.done ? (
                  <CheckCircle2 className="h-4 w-4 text-green-500" />
                ) : (
                  <Circle className="h-4 w-4 text-muted-foreground" />
                )}
                <span className={item.done ? "line-through text-muted-foreground" : ""}>
                  {item.key === "hasResume" || item.key === "hasScore" ? (
                    <Link to="/candidate" className="hover:underline">{item.label}</Link>
                  ) : (
                    <Link to="/candidate/profile" className="hover:underline">{item.label}</Link>
                  )}
                </span>
              </div>
            ))}
          </div>
        )}
        {data.completionPercentage === 100 && (
          <p className="text-sm text-green-600 dark:text-green-400 font-medium mt-2">
            Your profile is 100% complete! You're fully discoverable by recruiters.
          </p>
        )}
      </CardContent>
    </Card>
  );
}
