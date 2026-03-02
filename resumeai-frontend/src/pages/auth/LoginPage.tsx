import { useState } from "react";
import { Button } from "../../components/ui/button";
import { Input } from "../../components/ui/input";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "../../components/ui/card";
import { useAuth } from "../../context/AuthContext";
import apiClient from "../../lib/axios";
import { toast } from "sonner";
import { useNavigate } from "react-router-dom";
import React from "react";

export default function LoginPage() {
  const [isLogin, setIsLogin] = useState(true);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [name, setName] = useState("");
  const [role, setRole] = useState<"CANDIDATE" | "RECRUITER">("CANDIDATE");
  const [loading, setLoading] = useState(false);
  const { login, isAuthenticated, user, isLoading } = useAuth();
  const navigate = useNavigate();

  React.useEffect(() => {
    if (!isLoading && isAuthenticated) {
      if (user?.role === "CANDIDATE") {
        navigate("/candidate", { replace: true });
      } else {
        navigate("/recruiter", { replace: true });
      }
    }
  }, [isLoading, isAuthenticated, user, navigate]);

  const handleGoogleLogin = () => {
    window.location.href = "http://localhost:8080/oauth2/authorization/google";
  };

  if (isLoading || isAuthenticated) {
    return null; // or a loading spinner
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      const endpoint = isLogin ? "/auth/login" : "/auth/register";
      const payload = isLogin ? { email, password } : { email, password, name, role };
      const response = await apiClient.post(endpoint, payload);

      const { token, refreshToken, id, name: userName, email: userEmail, role: userRole } = response.data;
      login(token, refreshToken, { id, name: userName, email: userEmail, role: userRole });

      if (userRole === "CANDIDATE") {
        navigate("/candidate");
      } else {
        navigate("/recruiter");
      }
    } catch (error: any) {
      toast.error(error.response?.data || "Authentication failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-muted/40 p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="space-y-1">
          <CardTitle className="text-2xl font-bold">
            {isLogin ? "Sign in to ResumeAI" : "Create an account"}
          </CardTitle>
          <CardDescription>
            {isLogin
              ? "Enter your email below to sign in to your account"
              : "Enter your information to create an account"}
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <Button variant="outline" className="w-full" onClick={handleGoogleLogin}>
            Sign in with Google
          </Button>

          <div className="relative">
            <div className="absolute inset-0 flex items-center">
              <span className="w-full border-t" />
            </div>
            <div className="relative flex justify-center text-xs uppercase">
              <span className="bg-card px-2 text-muted-foreground">
                Or continue with
              </span>
            </div>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            {!isLogin && (
              <>
                <div className="space-y-2">
                  <Input
                    placeholder="Full Name"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    required
                  />
                </div>
                <div className="flex gap-4">
                  <Button
                    type="button"
                    variant={role === "CANDIDATE" ? "default" : "outline"}
                    className="flex-1"
                    onClick={() => setRole("CANDIDATE")}
                  >
                    Candidate
                  </Button>
                  <Button
                    type="button"
                    variant={role === "RECRUITER" ? "default" : "outline"}
                    className="flex-1"
                    onClick={() => setRole("RECRUITER")}
                  >
                    Recruiter
                  </Button>
                </div>
              </>
            )}
            <div className="space-y-2">
              <Input
                type="email"
                placeholder="m@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>
            <div className="space-y-2">
              <Input
                type="password"
                placeholder="Password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>
            <Button className="w-full" type="submit" disabled={loading}>
              {loading ? "Please wait..." : (isLogin ? "Sign In" : "Sign Up")}
            </Button>
          </form>
        </CardContent>
        <CardFooter className="flex justify-center">
          <Button
            variant="link"
            className="text-sm text-muted-foreground"
            onClick={() => setIsLogin(!isLogin)}
          >
            {isLogin
              ? "Don't have an account? Sign up"
              : "Already have an account? Sign in"}
          </Button>
        </CardFooter>
      </Card>
    </div>
  );
}
