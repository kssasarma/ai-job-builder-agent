import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import { Toaster } from "./components/ui/sonner";

import React from "react";
import LoginPage from "./pages/auth/LoginPage";
import RoleSelectionPage from "./pages/auth/RoleSelectionPage";
import OAuth2RedirectHandler from "./pages/auth/OAuth2RedirectHandler";
import { useAuth } from "./context/AuthContext";

import ResumeDashboard from "./pages/candidate/ResumeDashboard";
import CandidateProfilePage from "./pages/candidate/CandidateProfilePage";
import RecruiterDashboard from "./pages/recruiter/RecruiterDashboard";
import Navbar from "./components/layout/Navbar";

const PrivateRoute = ({ children, role }: { children: React.ReactElement; role?: "CANDIDATE" | "RECRUITER" }) => {
  const { isAuthenticated, user, isLoading } = useAuth();

  if (isLoading) {
    return <div>Loading...</div>;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (role && user?.role !== role) {
    return <Navigate to="/" replace />;
  }

  return (
    <div className="flex flex-col min-h-screen">
      <Navbar />
      <main className="flex-1">
        {children}
      </main>
    </div>
  );
};

const RootRedirect = () => {
  const { isAuthenticated, user, isLoading } = useAuth();

  if (isLoading) return <div>Loading...</div>;

  if (isAuthenticated) {
    return user?.role === "CANDIDATE" ? <Navigate to="/candidate" replace /> : <Navigate to="/recruiter" replace />;
  }

  return <Navigate to="/login" replace />;
};

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register/role-selection" element={<RoleSelectionPage />} />
          <Route path="/oauth2/redirect" element={<OAuth2RedirectHandler />} />
          <Route
            path="/candidate/*"
            element={
              <PrivateRoute role="CANDIDATE">
                <Routes>
                  <Route path="" element={<ResumeDashboard />} />
                  <Route path="profile" element={<CandidateProfilePage />} />
                </Routes>
              </PrivateRoute>
            }
          />
          <Route
            path="/recruiter/*"
            element={
              <PrivateRoute role="RECRUITER">
                <Routes>
                  <Route path="" element={<RecruiterDashboard />} />
                </Routes>
              </PrivateRoute>
            }
          />
          <Route path="/" element={<RootRedirect />} />
        </Routes>
        <Toaster />
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
