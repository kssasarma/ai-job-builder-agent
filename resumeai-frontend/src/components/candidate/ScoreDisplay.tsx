import { Card, CardContent, CardHeader, CardTitle } from "../ui/card";
import { Badge } from "../ui/badge";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";

interface ScoreData {
  overallScore: number;
  categories: { name: string; score: number; feedback: string }[];
  improvements: { text: string; priority: string }[];
  detectedSkills: string[];
  experienceSummary: string;
  educationSummary: string;
}

export function ScoreDisplay({ data }: { data: ScoreData }) {
  const getScoreColor = (score: number) => {
    if (score >= 75) return "text-green-500";
    if (score >= 50) return "text-amber-500";
    return "text-red-500";
  };

  const getPriorityBadgeVariant = (priority: string) => {
    switch (priority.toUpperCase()) {
      case "HIGH":
        return "destructive";
      case "MEDIUM":
        return "secondary";
      default:
        return "outline";
    }
  };

  return (
    <div className="space-y-6">
      {/* Overall Score */}
      <Card>
        <CardHeader className="text-center">
          <CardTitle>Overall ATS Score</CardTitle>
        </CardHeader>
        <CardContent className="flex justify-center">
          <div className="relative flex items-center justify-center h-48 w-48 rounded-full border-8 border-muted">
            <span className={`text-6xl font-bold ${getScoreColor(data.overallScore)}`}>
              {data.overallScore}
            </span>
            <div className="absolute -bottom-6 text-sm text-muted-foreground">
              out of 100
            </div>
          </div>
        </CardContent>
      </Card>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Category Breakdown */}
        <Card className="col-span-1">
          <CardHeader>
            <CardTitle>Category Breakdown</CardTitle>
          </CardHeader>
          <CardContent className="h-[300px]">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart
                data={data.categories}
                layout="vertical"
                margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
              >
                <CartesianGrid strokeDasharray="3 3" horizontal={false} />
                <XAxis type="number" domain={[0, 100]} />
                <YAxis dataKey="name" type="category" width={100} tick={{fontSize: 12}} />
                <Tooltip cursor={{fill: 'transparent'}} />
                <Bar dataKey="score" fill="hsl(var(--primary))" radius={[0, 4, 4, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>

        {/* Actionable Improvements */}
        <Card className="col-span-1">
          <CardHeader>
            <CardTitle>Actionable Improvements</CardTitle>
          </CardHeader>
          <CardContent>
            <ul className="space-y-4">
              {data.improvements.map((imp, idx) => (
                <li key={idx} className="flex items-start gap-3 border-b pb-3 last:border-0">
                  <Badge variant={getPriorityBadgeVariant(imp.priority)} className="mt-0.5">
                    {imp.priority}
                  </Badge>
                  <span className="text-sm">{imp.text}</span>
                </li>
              ))}
            </ul>
          </CardContent>
        </Card>
      </div>

      {/* Skills & Summaries */}
      <Card>
        <CardHeader>
          <CardTitle>Detected Profile Data</CardTitle>
        </CardHeader>
        <CardContent className="space-y-6">
          <div>
            <h4 className="font-semibold mb-2 text-sm text-muted-foreground uppercase tracking-wider">Skills</h4>
            <div className="flex flex-wrap gap-2">
              {data.detectedSkills.map((skill, i) => (
                <Badge key={i} variant="secondary">{skill}</Badge>
              ))}
            </div>
          </div>
          <div className="grid md:grid-cols-2 gap-4">
            <div>
              <h4 className="font-semibold mb-2 text-sm text-muted-foreground uppercase tracking-wider">Experience Summary</h4>
              <p className="text-sm bg-muted p-3 rounded-md">{data.experienceSummary}</p>
            </div>
            <div>
              <h4 className="font-semibold mb-2 text-sm text-muted-foreground uppercase tracking-wider">Education Summary</h4>
              <p className="text-sm bg-muted p-3 rounded-md">{data.educationSummary}</p>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
