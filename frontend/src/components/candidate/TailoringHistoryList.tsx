import { useState, useEffect } from "react";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "../../components/ui/card";
import { Badge } from "../../components/ui/badge";
import apiClient from "../../lib/axios";

interface HistoryProps {
  resumeId: string;
}

export function TailoringHistoryList({ resumeId }: HistoryProps) {
  const [history, setHistory] = useState<any[]>([]);

  useEffect(() => {
    fetchHistory();
  }, [resumeId]);

  const fetchHistory = async () => {
    try {
      const res = await apiClient.get(`/candidate/tailoring/resume/${resumeId}`);
      setHistory(res.data);
    } catch (e) {
      console.error("Failed to fetch history");
    }
  };

  const getTierColor = (tier: string) => {
    switch (tier) {
      case "GREEN": return "bg-green-500 hover:bg-green-600 text-white";
      case "AMBER": return "bg-amber-500 hover:bg-amber-600 text-white";
      case "RED": return "bg-red-500 hover:bg-red-600 text-white";
      default: return "bg-gray-500";
    }
  };

  if (history.length === 0) return null;

  return (
    <div className="space-y-4 mt-8">
      <h3 className="text-lg font-semibold">Previous Analyses</h3>
      <div className="grid gap-4">
        {history.map((h: any) => (
          <Card key={h.id}>
            <CardHeader className="py-4">
              <div className="flex justify-between items-start">
                <div className="flex-1 mr-4">
                  <CardTitle className="text-base line-clamp-1">{h.jobDescription}</CardTitle>
                  <CardDescription>
                    {new Date(h.createdAt).toLocaleDateString()}
                  </CardDescription>
                </div>
                <div className="flex flex-col items-end gap-2">
                  <Badge className={getTierColor(h.compatibilityTier)}>
                    {h.compatibilityTier} Tier
                  </Badge>
                  <span className="text-sm font-semibold">{h.compatibilityScore}% Match</span>
                </div>
              </div>
            </CardHeader>
          </Card>
        ))}
      </div>
    </div>
  );
}
