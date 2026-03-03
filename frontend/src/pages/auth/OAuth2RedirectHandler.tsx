import { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";

export default function OAuth2RedirectHandler() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { login } = useAuth();

  useEffect(() => {
    const token = searchParams.get("token");
    const refreshToken = searchParams.get("refreshToken");
    const id = searchParams.get("userId");
    const name = searchParams.get("name");
    const email = searchParams.get("email");
    const role = searchParams.get("role") as "CANDIDATE" | "RECRUITER";

    if (token && refreshToken && id && name && email && role) {
      login(token, refreshToken, { id, name, email, role });

      if (role === "CANDIDATE") {
        navigate("/candidate");
      } else {
        navigate("/recruiter");
      }
    } else {
      navigate("/login?error=OAuth2_failed");
    }
  }, [searchParams, navigate, login]);

  return (
    <div className="flex min-h-screen items-center justify-center">
      <p>Logging you in...</p>
    </div>
  );
}
