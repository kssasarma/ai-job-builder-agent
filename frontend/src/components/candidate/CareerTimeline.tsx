import { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "../ui/card";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Badge } from "../ui/badge";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../ui/select";
import { Trash2, Plus } from "lucide-react";
import apiClient from "../../lib/axios";
import { toast } from "sonner";

interface CareerEvent {
  id: string;
  type: string;
  title: string;
  description: string | null;
  eventDate: string;
}

const TYPE_LABELS: Record<string, string> = {
  PROMOTION: "Promotion",
  SKILL_GAINED: "Skill Gained",
  JOB_COMPLETED: "Job Completed",
  CERTIFICATION: "Certification",
};

/** Career record, not a resume: a living timeline that compounds across job searches. */
export function CareerTimeline() {
  const [events, setEvents] = useState<CareerEvent[]>([]);
  const [showForm, setShowForm] = useState(false);
  const [type, setType] = useState("SKILL_GAINED");
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [eventDate, setEventDate] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    apiClient.get("/candidate/career-events").then(res => setEvents(res.data)).catch(() => {});
  }, []);

  const addEvent = async () => {
    if (!title.trim() || !eventDate) return toast.error("Title and date are required");
    setSaving(true);
    try {
      const res = await apiClient.post("/candidate/career-events", { type, title, description, eventDate });
      setEvents(prev => [res.data, ...prev].sort((a, b) => b.eventDate.localeCompare(a.eventDate)));
      setTitle(""); setDescription(""); setEventDate(""); setShowForm(false);
    } catch {
      toast.error("Couldn't save that milestone.");
    } finally {
      setSaving(false);
    }
  };

  const removeEvent = async (id: string) => {
    try {
      await apiClient.delete(`/candidate/career-events/${id}`);
      setEvents(prev => prev.filter(e => e.id !== id));
    } catch {
      toast.error("Couldn't remove that milestone.");
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Career Timeline</CardTitle>
        <CardDescription>A record that compounds across job searches — promotions, new skills, completed roles.</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        {events.length === 0 && !showForm && (
          <p className="text-sm text-muted-foreground">No milestones yet.</p>
        )}
        <div className="space-y-3">
          {events.map(e => (
            <div key={e.id} className="flex items-start justify-between gap-3 p-3 rounded-lg border bg-muted/20">
              <div>
                <div className="flex items-center gap-2">
                  <Badge variant="outline" className="text-[10px]">{TYPE_LABELS[e.type] || e.type}</Badge>
                  <span className="text-xs text-muted-foreground">{new Date(e.eventDate).toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" })}</span>
                </div>
                <p className="font-medium text-sm mt-1">{e.title}</p>
                {e.description && <p className="text-xs text-muted-foreground mt-0.5">{e.description}</p>}
              </div>
              <Button size="icon" variant="ghost" className="h-7 w-7 shrink-0" onClick={() => removeEvent(e.id)}>
                <Trash2 className="h-3.5 w-3.5" />
              </Button>
            </div>
          ))}
        </div>

        {showForm ? (
          <div className="space-y-3 pt-2 border-t">
            <Select value={type} onValueChange={setType}>
              <SelectTrigger><SelectValue /></SelectTrigger>
              <SelectContent>
                {Object.entries(TYPE_LABELS).map(([value, label]) => (
                  <SelectItem key={value} value={value}>{label}</SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Input placeholder="Title (e.g. Promoted to Senior Engineer)" value={title} onChange={e => setTitle(e.target.value)} />
            <Input placeholder="Description (optional)" value={description} onChange={e => setDescription(e.target.value)} />
            <Input type="date" value={eventDate} onChange={e => setEventDate(e.target.value)} />
            <div className="flex gap-2">
              <Button size="sm" onClick={addEvent} disabled={saving}>Save</Button>
              <Button size="sm" variant="ghost" onClick={() => setShowForm(false)}>Cancel</Button>
            </div>
          </div>
        ) : (
          <Button size="sm" variant="outline" onClick={() => setShowForm(true)}>
            <Plus className="mr-2 h-3.5 w-3.5" /> Add milestone
          </Button>
        )}
      </CardContent>
    </Card>
  );
}
