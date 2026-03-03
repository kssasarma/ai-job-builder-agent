import { useState, useEffect } from "react";
import { Card, CardContent, CardHeader, CardTitle, CardDescription, CardFooter } from "../../components/ui/card";
import { Input } from "../../components/ui/input";
import { Button } from "../../components/ui/button";
import { Switch } from "../../components/ui/switch";
import apiClient from "../../lib/axios";
import { toast } from "sonner";
import { Loader2 } from "lucide-react";

export default function CandidateProfilePage() {
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [profile, setProfile] = useState({
    headline: "",
    linkedinUrl: "",
    preferredContactEmail: "",
    openToOpportunities: false,
    skills: [] as string[]
  });
  const [skillsInput, setSkillsInput] = useState("");

  useEffect(() => {
    fetchProfile();
  }, []);

  const fetchProfile = async () => {
    try {
      const res = await apiClient.get("/candidate/profile");
      const data = res.data;
      setProfile({
        headline: data.headline || "",
        linkedinUrl: data.linkedinUrl || "",
        preferredContactEmail: data.preferredContactEmail || "",
        openToOpportunities: data.openToOpportunities || false,
        skills: data.skills || []
      });
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
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Discoverability</CardTitle>
          <CardDescription>Control whether recruiters can find your profile.</CardDescription>
        </CardHeader>
        <CardContent className="flex items-center justify-between">
          <div className="space-y-0.5">
            <label className="text-base font-medium">Open to Opportunities</label>
            <p className="text-sm text-muted-foreground">Allow recruiters to see your profile and invite you to apply.</p>
          </div>
          <Switch
            checked={profile.openToOpportunities}
            onCheckedChange={checked => setProfile({...profile, openToOpportunities: checked})}
          />
        </CardContent>
        <CardFooter className="bg-muted/20 justify-end pt-6">
          <Button onClick={handleSave} disabled={saving}>
            {saving ? <><Loader2 className="mr-2 h-4 w-4 animate-spin" /> Saving...</> : "Save Changes"}
          </Button>
        </CardFooter>
      </Card>
    </div>
  );
}
