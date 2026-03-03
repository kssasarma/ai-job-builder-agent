import { useState, useEffect } from "react";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription } from "../ui/dialog";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../ui/select";
import apiClient from "../../lib/axios";
import { toast } from "sonner";

interface JobCreateModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onJobCreated: () => void;
  job?: any | null;        // when provided, modal is in edit mode
  onJobUpdated?: () => void;
}

export function JobCreateModal({ open, onOpenChange, onJobCreated, job, onJobUpdated }: JobCreateModalProps) {
  const isEdit = !!job;

  const [title, setTitle] = useState("");
  const [company, setCompany] = useState("");
  const [description, setDescription] = useState("");
  const [skills, setSkills] = useState("");
  const [experienceMin, setExperienceMin] = useState("");
  const [experienceMax, setExperienceMax] = useState("");
  const [location, setLocation] = useState("");
  const [salaryRange, setSalaryRange] = useState("");
  const [jobType, setJobType] = useState("FULL_TIME");
  const [loading, setLoading] = useState(false);

  // Sync form when switching between create/edit or when job changes
  useEffect(() => {
    if (job) {
      setTitle(job.title ?? "");
      setCompany(job.company ?? "");
      setDescription(job.description ?? "");
      setSkills((job.requiredSkills ?? []).join(", "));
      setExperienceMin(job.experienceMin != null ? String(job.experienceMin) : "");
      setExperienceMax(job.experienceMax != null ? String(job.experienceMax) : "");
      setLocation(job.location ?? "");
      setSalaryRange(job.salaryRange ?? "");
      setJobType(job.jobType ?? "FULL_TIME");
    } else {
      setTitle(""); setCompany(""); setDescription(""); setSkills("");
      setExperienceMin(""); setExperienceMax(""); setLocation(""); setSalaryRange("");
      setJobType("FULL_TIME");
    }
  }, [job, open]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title || !company || !description) {
      toast.error("Please fill in all required fields");
      return;
    }

    setLoading(true);
    try {
      const payload = {
        title, company, description,
        requiredSkills: skills.split(",").map(s => s.trim()).filter(Boolean),
        experienceMin: experienceMin ? parseInt(experienceMin) : null,
        experienceMax: experienceMax ? parseInt(experienceMax) : null,
        location, salaryRange, jobType,
        status: job?.status ?? "OPEN",
      };

      if (isEdit) {
        await apiClient.put(`/recruiter/jobs/${job.id}`, payload);
        toast.success("Job posting updated!");
        onJobUpdated?.();
      } else {
        await apiClient.post("/recruiter/jobs", { ...payload, status: "OPEN" });
        toast.success("Job posting created!");
        onJobCreated();
      }
      onOpenChange(false);
    } catch (error: any) {
      toast.error(error.response?.data || (isEdit ? "Failed to update job posting" : "Failed to create job posting"));
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[525px]">
        <DialogHeader>
          <DialogTitle>{isEdit ? "Edit Job Posting" : "Create New Job Posting"}</DialogTitle>
          <DialogDescription>{isEdit ? "Update the details for this job posting." : "Add details for the new open position."}</DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4 py-4">
          <div className="space-y-2">
            <label className="text-sm font-medium">Job Title *</label>
            <Input value={title} onChange={e => setTitle(e.target.value)} placeholder="e.g. Senior Frontend Engineer" required />
          </div>
          <div className="space-y-2">
            <label className="text-sm font-medium">Company Name *</label>
            <Input value={company} onChange={e => setCompany(e.target.value)} placeholder="Acme Corp" required />
          </div>
          <div className="space-y-2">
            <label className="text-sm font-medium">Description *</label>
            <textarea
              className="w-full min-h-[100px] p-3 border rounded-md resize-y bg-background text-sm"
              value={description}
              onChange={e => setDescription(e.target.value)}
              placeholder="Job responsibilities, requirements..."
              required
            />
          </div>
          <div className="space-y-2">
            <label className="text-sm font-medium">Required Skills (Comma separated)</label>
            <Input value={skills} onChange={e => setSkills(e.target.value)} placeholder="React, TypeScript, Node.js" />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <label className="text-sm font-medium">Min Experience (Years)</label>
              <Input type="number" value={experienceMin} onChange={e => setExperienceMin(e.target.value)} placeholder="0" />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Max Experience (Years)</label>
              <Input type="number" value={experienceMax} onChange={e => setExperienceMax(e.target.value)} placeholder="5" />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <label className="text-sm font-medium">Location</label>
              <Input value={location} onChange={e => setLocation(e.target.value)} placeholder="San Francisco, CA or Remote" />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Salary Range</label>
              <Input value={salaryRange} onChange={e => setSalaryRange(e.target.value)} placeholder="$100k - $150k" />
            </div>
          </div>
          <div className="space-y-2">
            <label className="text-sm font-medium">Job Type</label>
            <Select value={jobType} onValueChange={setJobType}>
              <SelectTrigger>
                <SelectValue placeholder="Select Job Type" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="FULL_TIME">Full Time</SelectItem>
                <SelectItem value="PART_TIME">Part Time</SelectItem>
                <SelectItem value="CONTRACT">Contract</SelectItem>
                <SelectItem value="REMOTE">Remote</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
            <Button type="submit" disabled={loading}>{loading ? "Saving..." : isEdit ? "Save Changes" : "Create Job"}</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
