import { useState, useEffect } from "react";
import apiClient from "../../lib/axios";
import { toast } from "sonner";
import { Button } from "../../components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "../../components/ui/card";
import { Input } from "../../components/ui/input";

export default function RecruiterProfilePage() {
  const [profile, setProfile] = useState<any>(null);
  const [companyName, setCompanyName] = useState("");
  const [companyWebsite, setCompanyWebsite] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const res = await apiClient.get("/recruiter/profile");
        setProfile(res.data);
        setCompanyName(res.data.companyName || "");
        setCompanyWebsite(res.data.companyWebsite || "");
      } catch (error) {
        toast.error("Failed to load profile.");
      } finally {
        setLoading(false);
      }
    };
    fetchProfile();
  }, []);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      const res = await apiClient.put("/recruiter/profile", {
        companyName,
        companyWebsite
      });
      setProfile(res.data);
      toast.success("Profile updated successfully!");
    } catch (error) {
      toast.error("Failed to update profile.");
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return <div className="p-8 text-center">Loading profile...</div>;
  }

  return (
    <div className="container mx-auto p-6 max-w-2xl mt-8">
      <Card>
        <CardHeader>
          <CardTitle>Recruiter Profile</CardTitle>
          <CardDescription>Manage your company and contact details.</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSave} className="space-y-4">
            <div className="space-y-2">
              <label className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70">Name</label>
              <Input value={profile?.user?.name || ""} disabled />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70">Email</label>
              <Input value={profile?.user?.email || ""} disabled />
            </div>
            <div className="space-y-2">
              <label htmlFor="companyName" className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70">Company Name</label>
              <Input
                id="companyName"
                value={companyName}
                onChange={e => setCompanyName(e.target.value)}
                placeholder="e.g. Acme Corp"
                required
              />
            </div>
            <div className="space-y-2">
              <label htmlFor="companyWebsite" className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70">Company Website</label>
              <Input
                id="companyWebsite"
                type="url"
                value={companyWebsite}
                onChange={e => setCompanyWebsite(e.target.value)}
                placeholder="https://acmecorp.com"
              />
            </div>
            <Button type="submit" disabled={saving}>
              {saving ? "Saving..." : "Save Profile"}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
