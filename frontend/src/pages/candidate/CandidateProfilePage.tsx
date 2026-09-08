import { useState, useEffect } from "react";
import { Card, CardContent, CardHeader, CardTitle, CardDescription, CardFooter } from "../../components/ui/card";
import { Input } from "../../components/ui/input";
import { Button } from "../../components/ui/button";
import { Switch } from "../../components/ui/switch";
import { Badge } from "../../components/ui/badge";
import apiClient from "../../lib/axios";
import { toast } from "sonner";
import { Loader2, ShieldCheck, Check, X } from "lucide-react";
import { CareerTimeline } from "../../components/candidate/CareerTimeline";

interface RevealRequest {
  id: string;
  companyName: string;
  status: string;
  createdAt: string;
}

export default function CandidateProfilePage() {
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [revealRequests, setRevealRequests] = useState<RevealRequest[]>([]);
  const [profile, setProfile] = useState({
    headline: "",
    linkedinUrl: "",
    preferredContactEmail: "",
    openToOpportunities: false,
    aiConsent: true,
    anonymizedDiscovery: false,
    skills: [] as string[]
  });
  const [verifiedSkills, setVerifiedSkills] = useState<string[]>([]);
  const [skillsInput, setSkillsInput] = useState("");

  useEffect(() => {
    fetchProfile();
    apiClient.get("/candidate/reveal-requests").then(res => setRevealRequests(res.data)).catch(() => {});
  }, []);

  const respondToReveal = async (id: string, approve: boolean) => {
    try {
      await apiClient.post(`/candidate/reveal-requests/${id}/${approve ? "approve" : "deny"}`);
      setRevealRequests(prev => prev.map(r => r.id === id ? { ...r, status: approve ? "APPROVED" : "DENIED" } : r));
      toast.success(approve ? "Profile revealed to that recruiter." : "Request denied.");
    } catch {
      toast.error("Couldn't update that request.");
    }
  };

  const fetchProfile = async () => {
    try {
      const res = await apiClient.get("/candidate/profile");
      const data = res.data;
      setProfile({
        headline: data.headline || "",
        linkedinUrl: data.linkedinUrl || "",
        preferredContactEmail: data.preferredContactEmail || "",
        openToOpportunities: data.openToOpportunities || false,
        aiConsent: data.aiConsent !== false,
        anonymizedDiscovery: data.anonymizedDiscovery || false,
        skills: data.skills || []
      });
      setVerifiedSkills(data.verifiedSkills || []);
      setSkillsInput(data.skills ? data.skills.join(", ") : "");
    } catch (error) {
      toast.error("Failed to load profile");
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      const skillsArray = skillsInput.split(",").map(s => s.trim()).filter(Boolean);
      await apiClient.put("/candidate/profile", {
        ...profile,
        skills: skillsArray
      });
      toast.success("Profile updated successfully");
      setProfile(prev => ({ ...prev, skills: skillsArray }));
    } catch (error) {
      toast.error("Failed to update profile");
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return <div className="flex justify-center p-12"><Loader2 className="h-8 w-8 animate-spin text-primary" /></div>;
  }

  return (
    <div className="container mx-auto p-6 max-w-3xl space-y-8 mt-8">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Profile Settings</h1>
        <p className="text-muted-foreground mt-2">Manage your public profile and discoverability.</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Basic Information</CardTitle>
          <CardDescription>Update your contact and professional details.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <label className="text-sm font-medium">Headline</label>
            <Input
              value={profile.headline}
              onChange={e => setProfile({...profile, headline: e.target.value})}
              placeholder="e.g. Senior Software Engineer at Acme"
            />
          </div>

          <div className="grid md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <label className="text-sm font-medium">Preferred Contact Email</label>
              <Input
                type="email"
                value={profile.preferredContactEmail}
                onChange={e => setProfile({...profile, preferredContactEmail: e.target.value})}
                placeholder="name@example.com"
              />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">LinkedIn URL</label>
              <Input
                type="url"
                value={profile.linkedinUrl}
                onChange={e => setProfile({...profile, linkedinUrl: e.target.value})}
                placeholder="https://linkedin.com/in/username"
              />
            </div>
          </div>

          <div className="space-y-2 pt-2">
            <label className="text-sm font-medium">Skills (Comma separated)</label>
            <Input
              value={skillsInput}
              onChange={e => setSkillsInput(e.target.value)}
              placeholder="Java, React, PostgreSQL"
            />
            <p className="text-xs text-muted-foreground">These will be used to match you with job opportunities.</p>
          </div>

          {verifiedSkills.length > 0 && (
            <div className="space-y-2 pt-2">
              <label className="text-sm font-medium flex items-center gap-1.5">
                <ShieldCheck className="h-4 w-4 text-green-600 dark:text-green-400" /> Verified Skills
              </label>
              <div className="flex flex-wrap gap-1.5">
                {verifiedSkills.map(s => (
                  <Badge key={s} className="bg-green-500/10 text-green-700 hover:bg-green-500/20 border-green-500/30">{s}</Badge>
                ))}
              </div>
              <p className="text-xs text-muted-foreground">Earned by passing a skill challenge — not just self-reported.</p>
            </div>
          )}
        </CardContent>
      </Card>

      {revealRequests.some(r => r.status === "PENDING") && (
        <Card className="border-primary/40">
          <CardHeader>
            <CardTitle>Profile Reveal Requests</CardTitle>
            <CardDescription>Recruiters asking to see your name and contact details, since your profile is anonymized.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            {revealRequests.filter(r => r.status === "PENDING").map(r => (
              <div key={r.id} className="flex items-center justify-between gap-3 p-3 rounded-lg border bg-muted/20">
                <div>
                  <p className="font-medium text-sm">{r.companyName}</p>
                  <p className="text-xs text-muted-foreground">Requested {new Date(r.createdAt).toLocaleDateString()}</p>
                </div>
                <div className="flex gap-2">
                  <Button size="sm" variant="outline" onClick={() => respondToReveal(r.id, false)}>
                    <X className="mr-1.5 h-3.5 w-3.5" /> Deny
                  </Button>
                  <Button size="sm" onClick={() => respondToReveal(r.id, true)}>
                    <Check className="mr-1.5 h-3.5 w-3.5" /> Approve
                  </Button>
                </div>
              </div>
            ))}
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader>
          <CardTitle>Discoverability</CardTitle>
          <CardDescription>Control whether recruiters can find your profile.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="flex items-center justify-between">
            <div className="space-y-0.5">
              <label className="text-base font-medium">Open to Opportunities</label>
              <p className="text-sm text-muted-foreground">Allow recruiters to see your profile and invite you to apply.</p>
            </div>
            <Switch
              checked={profile.openToOpportunities}
              onCheckedChange={checked => setProfile({...profile, openToOpportunities: checked})}
            />
          </div>
          <div className="flex items-center justify-between border-t pt-6">
            <div className="space-y-0.5">
              <label className="text-base font-medium">Anonymized-First Discovery</label>
              <p className="text-sm text-muted-foreground">Recruiters see a skills-only card first — your name and contact details stay hidden until you approve a reveal request.</p>
            </div>
            <Switch
              checked={profile.anonymizedDiscovery}
              onCheckedChange={checked => setProfile({...profile, anonymizedDiscovery: checked})}
            />
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>AI Data Use</CardTitle>
          <CardDescription>Every AI flow on this platform reads your resume — you can turn that off.</CardDescription>
        </CardHeader>
        <CardContent className="flex items-center justify-between">
          <div className="space-y-0.5">
            <label className="text-base font-medium">Allow AI scoring, matching &amp; tailoring</label>
            <p className="text-sm text-muted-foreground">Turning this off stops ATS scoring, compatibility analysis, resume tailoring, and candidate matching from running on your profile.</p>
          </div>
          <Switch
            checked={profile.aiConsent}
            onCheckedChange={checked => setProfile({...profile, aiConsent: checked})}
          />
        </CardContent>
        <CardFooter className="bg-muted/20 justify-end pt-6">
          <Button onClick={handleSave} disabled={saving}>
            {saving ? <><Loader2 className="mr-2 h-4 w-4 animate-spin" /> Saving...</> : "Save Changes"}
          </Button>
        </CardFooter>
      </Card>

      <CareerTimeline />
    </div>
  );
}
