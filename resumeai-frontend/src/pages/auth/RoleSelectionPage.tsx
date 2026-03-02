import { useState, useEffect } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from "../../components/ui/card";
import { Button } from "../../components/ui/button";
import { toast } from "sonner";
import apiClient from "../../lib/axios";
import { useAuth } from "../../context/AuthContext";

export default function RoleSelectionPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { login } = useAuth();
  const [role, setRole] = useState<"CANDIDATE" | "RECRUITER">("CANDIDATE");
  const [loading, setLoading] = useState(false);

  const email = searchParams.get("email");
  const name = searchParams.get("name");
  const tempToken = searchParams.get("tempToken");

  useEffect(() => {
    if (!email || !name || !tempToken) {
      navigate("/login");
    }
  }, [email, name, tempToken, navigate]);

  if (!email || !name || !tempToken) {
    return null;
  }

  const handleCompleteRegistration = async () => {
    setLoading(true);
    try {
      const response = await apiClient.post("/auth/complete-oauth", {
        email,
        name,
        role,
        tempToken,
      });

      const { token, refreshToken, id, role: userRole } = response.data;
      login(token, refreshToken, { id, name, email, role: userRole });

      if (userRole === "CANDIDATE") {
        navigate("/candidate");
      } else {
        navigate("/recruiter");
      }
    } catch (error: any) {
      toast.error(error.response?.data || "Failed to complete registration");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-muted/40 p-4">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>Welcome, {name}!</CardTitle>
          <CardDescription>
            Please select your role to complete registration.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex flex-col gap-4">
            <Button
              variant={role === "CANDIDATE" ? "default" : "outline"}
              className="h-20 flex-col gap-1"
              onClick={() => setRole("CANDIDATE")}
            >
              <span className="text-lg font-bold">I am a Candidate</span>
              <span className="text-xs font-normal">Looking for jobs and resume help</span>
            </Button>
            <Button
              variant={role === "RECRUITER" ? "default" : "outline"}
              className="h-20 flex-col gap-1"
              onClick={() => setRole("RECRUITER")}
            >
              <span className="text-lg font-bold">I am a Recruiter</span>
              <span className="text-xs font-normal">Looking to hire top talent</span>
            </Button>
          </div>
        </CardContent>
        <CardFooter>
          <Button
            className="w-full"
            onClick={handleCompleteRegistration}
            disabled={loading}
          >
            {loading ? "Completing setup..." : "Complete Registration"}
          </Button>
        </CardFooter>
      </Card>
    </div>
  );
}
