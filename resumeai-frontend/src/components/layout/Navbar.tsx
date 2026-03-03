import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { Button } from "../ui/button";
import { Badge } from "../ui/badge";

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  if (!user) return null;

  return (
    <nav className="border-b bg-background px-6 py-3 flex items-center justify-between">
      <div className="flex items-center gap-6">
        <Link to="/" className="text-xl font-bold">ResumeAI</Link>
        <div className="flex gap-4">
          <Link to={user.role === "CANDIDATE" ? "/candidate" : "/recruiter"} className="text-sm font-medium hover:underline">
            Dashboard
          </Link>
          {user.role === "CANDIDATE" && (
            <>
              <Link to="/candidate/jobs" className="text-sm font-medium hover:underline">
                Browse Jobs
              </Link>
              <Link to="/candidate/profile" className="text-sm font-medium hover:underline">
                Profile
              </Link>
            </>
          )}
          {user.role === "RECRUITER" && (
            <>
              <Link to="/recruiter/candidates" className="text-sm font-medium hover:underline">
                Browse Candidates
              </Link>
              <Link to="/recruiter/profile" className="text-sm font-medium hover:underline">
                Profile
              </Link>
            </>
          )}
        </div>
      </div>
      <div className="flex items-center gap-4">
        <span className="text-sm text-muted-foreground">Hello, {user.name}</span>
        <Badge variant={user.role === "CANDIDATE" ? "default" : "secondary"}>
          {user.role}
        </Badge>
        <Button variant="outline" size="sm" onClick={handleLogout}>Logout</Button>
      </div>
    </nav>
  );
}
